package com.example.android.wearable.datalayer.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.android.wearable.datalayer.R
import com.example.android.wearable.datalayer.databinding.ActivityWearBinding
import com.example.android.wearable.datalayer.service.StepCounterService
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainWearActivity : ComponentActivity() {

    //used for communication between phone and app if we want to share complex data
    private val dataClient by lazy { Wearable.getDataClient(this) }

    //used to simple message communication between phone and app
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    //used to get list of device compatible with wear or paired device
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private val mainWearViewModel by viewModels<MainWearViewModel>()


    private var stepCounterServiceBound = false
    private var stepCounterService: StepCounterService? = null
    private val stepCounterServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as StepCounterService.LocalBinder
            stepCounterService = binder.stepCounterService
            stepCounterServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            stepCounterService = null
            stepCounterServiceBound = false
        }
    }


    private lateinit var binding: ActivityWearBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wear)
        onQueryOtherDevicesClicked()
        binding.btStartWalk.setOnClickListener {
            onClickStartWalking()
        }
        StepCounterService.stepCountLiveData.observe(this) { step ->
            binding.tvCount.text = step
            lifecycleScope.launch {
                sendStepCount(step)
            }

        }
        binding.textStatus.setOnLongClickListener {
            onQueryOtherDevicesClicked()
            true
        }
        binding.btStop.setOnClickListener {
            stepCounterService?.stopWalking()
            binding.tvCount.text = "0"
            lifecycleScope.launch {
                sendStepCount("0")
            }
        }
        mainWearViewModel.obEvents.observe(this) {
            binding.textInfo.visibility=View.VISIBLE
            binding.textInfo.text = it.toString()
        }
    }


    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, StepCounterService::class.java)
        bindService(serviceIntent, stepCounterServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private suspend fun sendStepCount(count: String) {
        try {
            val request = PutDataMapRequest.create("/step").apply {
                dataMap.putString("step", count)
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

    override fun onStop() {
        if (stepCounterServiceBound) {
            unbindService(stepCounterServiceConnection)
            stepCounterServiceBound = false
        }
        super.onStop()
    }


    private fun onClickStartWalking() {
        Log.d(TAG, "onClickWalking")
        stepCounterService?.startWalking()
    }


    private fun onQueryOtherDevicesClicked() {
        lifecycleScope.launch {
            try {
                val nodes = getCapabilitiesForReachableNodes()
                    .filterValues { "mobile" in it || "wear" in it }.keys
                displayNodes(nodes)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Querying nodes failed: $exception")
            }
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

    private fun displayNodes(nodes: Set<Node>) {
        val message = if (nodes.isEmpty()) {
            getString(R.string.no_device)
        } else {
            getString(R.string.connected_nodes, nodes.joinToString(", ") { it.displayName })
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(mainWearViewModel)
        messageClient.addListener(mainWearViewModel)
        capabilityClient.addListener(
            mainWearViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )

    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(mainWearViewModel)
        messageClient.removeListener(mainWearViewModel)
        capabilityClient.removeListener(mainWearViewModel)
    }


    companion object {
        private const val TAG = "MainActivity"
    }

}
