package com.example.android.wearable.datalayer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.android.wearable.datalayer.databinding.ActivityMainBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainMobileActivity : ComponentActivity(), SensorEventListener {

    private val dataClient by lazy { Wearable.getDataClient(this) }

    // private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }
    // private val nodeClient by lazy { Wearable.getNodeClient(this) }

    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    private val clientDataViewModel by viewModels<MainMobileViewModel>()


    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        loadData()
        resetSteps()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        binding.btQuery.setOnClickListener {
            lifecycleScope.launch {
                binding.tvStatus.text = getCapabilitiesForReachableNodes().values.toString()
            }
        }
        binding.btSend.setOnClickListener {
            lifecycleScope.launch {
                sendMessage(binding.etMsg.text.toString())
            }
        }
        clientDataViewModel.obEvents.observe(this) {
            binding.tvStepsTaken.text = it.last().toString()
        }

    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel)
        // messageClient.addListener(clientDataViewModel)
        capabilityClient.addListener(
            clientDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )
        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(clientDataViewModel)
        //   messageClient.removeListener(clientDataViewModel)
        capabilityClient.removeListener(clientDataViewModel)
    }


    private suspend fun sendCount(count: Int) {
        try {
            val request = PutDataMapRequest.create(COUNT_PATH).apply {
                dataMap.putInt(COUNT_KEY, count)
            }
                .asPutDataRequest()
                .setUrgent()
            val result = dataClient.putDataItem(request).await()
            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    private suspend fun sendMessage(msg: String) {
        try {
            val request = PutDataMapRequest.create(MSG_PATH).apply {
                dataMap.putString(MSG_KEY, msg)
            }
                .asPutDataRequest()
                .setUrgent()
            val result = dataClient.putDataItem(request).await()
            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    private suspend fun sendSteps(count: Int) {
        try {
            val request = PutDataMapRequest.create(STEP_PATH).apply {
                dataMap.putInt(STEP_KEY, count)
            }
                .asPutDataRequest()
                .setUrgent()
            val result = dataClient.putDataItem(request).await()
            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    private suspend fun getCapabilitiesForReachableNodes(): Map<Node, Set<String>> =
        capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
            .await()
            .flatMap { (capability, capabilityInfo) ->
                capabilityInfo.nodes.map { it to capability }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .mapValues { it.value.toSet() }


    companion object {
        private const val TAG = "MainActivity"
        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val COUNT_PATH = "/count"
        private const val STEP_PATH = "/step"
        private const val STEP_KEY = "step"
        private const val MSG_PATH = "/msg"
        private const val COUNT_KEY = "count"
        private const val MSG_KEY = "msg"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
            binding.tvStepsTaken.text = ("$currentSteps")
            lifecycleScope.launch {
                sendSteps(currentSteps)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit

    private fun resetSteps() {
        binding.tvStepsTaken.setOnClickListener {
            Toast.makeText(this, "Long tap to reset steps", Toast.LENGTH_SHORT).show()
        }
        binding.tvStepsTaken.setOnLongClickListener {
            previousTotalSteps = totalSteps
            binding.tvStepsTaken.text = 0.toString()
            saveData()
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("previous_step", previousTotalSteps)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("previous_step", 0f)
        Log.d(TAG, "$savedNumber")
        previousTotalSteps = savedNumber
    }

}

/*    private fun startWearableActivity() {
        lifecycleScope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.map { node ->
                    async {
                        messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf())
                            .await()
                    }
                }.awaitAll()

                Log.d(TAG, "Starting activity requests sent successfully")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Starting activity failed: $exception")
            }
        }
    }
*/
