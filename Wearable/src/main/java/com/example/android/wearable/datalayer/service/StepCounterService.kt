package com.example.android.wearable.datalayer.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.android.wearable.datalayer.R
import com.example.android.wearable.datalayer.ui.MainWearActivity
import com.example.android.wearable.datalayer.util.NotificationBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StepCounterService : LifecycleService(), SensorEventListener {

    private lateinit var notificationManager: NotificationManager
    private lateinit var sensorManager: SensorManager
    private val localBinder = LocalBinder()
    private var mockDataForWalkingWorkoutJob: Job? = null
    private var activityRunning = false

    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var notificationBuilder: NotificationBuilder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        notificationBuilder = NotificationBuilder(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    /**
     * Here we stop service if we get stopIntent from notification cancel steps button
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand")
        val cancelStepCounterFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_WALK_FROM_NOTIFICATION, false) ?: false
        if (cancelStepCounterFromNotification) {
            stopStepCounterService()
        }
        return Service.START_NOT_STICKY
    }

    /**
     * OnBind is used to make service as background and keep status of whether
     * show notification or not
     */
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        activityRunning = true
        notForegroundService()
        return localBinder
    }

    /**
     * OnReBind is used to make service as background and keep status of whether
     * show notification or not
     */
    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind")
        activityRunning = true
        notForegroundService()
    }

    /**
     * OnUnbind is used to make service as foreground and show notification to user
     */
    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")
        activityRunning = false
        //val notification = generateNotification(getString(R.string.step_count_loading))
        val notification =
            notificationBuilder?.generateNotification(getString(R.string.step_count_loading))
        startForeground(NOTIFICATION_ID, notification)
        return true
    }


    /**
     * stopping foreground to remove notification from tray
     */
    private fun notForegroundService() {
        stopForeground(true)
    }

    /**
     * Starting service to start taking steps count
     */
    fun startWalking() {
        Log.d(TAG, "startWalking")
        mockDataForWalkingWorkoutJob?.cancel()
        stopSelf()
        startService(Intent(applicationContext, StepCounterService::class.java))
        mockDataForWalkingWorkoutJob = lifecycleScope.launch {
            mockSensorAndLocationForWalkingWorkout()
        }
    }

    fun stopWalking() {
        Log.d(TAG, "stopWalking")
        stopStepCounterService()
    }


    /**
     * Cancelling service and running job if any
     */
    private fun stopStepCounterService() {
        Log.d(TAG, "stopStepCounterService()")
        mockDataForWalkingWorkoutJob?.cancel()
        stopSelf()
    }


    /**
     * Mimic step data as emulator does not provide sensor and showing step count in activity
     * or notification depending on whether activity is running or not
     */
    private suspend fun mockSensorAndLocationForWalkingWorkout() {
        for (walkingPoints in 0 until 1000) {
            val notification = notificationBuilder?.generateNotification(
                getString(
                    R.string.step_count_text,
                    walkingPoints
                )
            )
            if (activityRunning)
                _stepCountLiveData.postValue(walkingPoints.toString())
            else
                notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "mockSensorAndLocationForWalkingWorkout(): $walkingPoints")
            delay(THREE_SECONDS_MILLISECONDS)
        }
    }



    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        internal val stepCounterService: StepCounterService
            get() = this@StepCounterService
    }

    companion object {
        private val _stepCountLiveData = MutableLiveData<String>()
        val stepCountLiveData: LiveData<String> = _stepCountLiveData
        private const val TAG = "StepCounterServiceOnly"
        private const val THREE_SECONDS_MILLISECONDS = 3000L
        private const val PACKAGE_NAME = "com.android.example.wear"
        private const val EXTRA_CANCEL_WALK_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_SUBSCRIPTION_FROM_NOTIFICATION"
        private const val NOTIFICATION_ID = 12345678
        const val NOTIFICATION_CHANNEL_ID = "walking_workout_channel_01"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        totalSteps = event!!.values[0]
        val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()
        val notification = notificationBuilder?.generateNotification(
            getString(
                R.string.step_count_text,
                currentSteps
            )
        )
        if (activityRunning)
            _stepCountLiveData.postValue(currentSteps.toString())
        else
            notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "onSensorChanged: $currentSteps")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit
}

