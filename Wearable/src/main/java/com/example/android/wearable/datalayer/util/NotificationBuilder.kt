package com.example.android.wearable.datalayer.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.android.wearable.datalayer.R
import com.example.android.wearable.datalayer.service.StepCounterService
import com.example.android.wearable.datalayer.ui.MainWearActivity


class NotificationBuilder(private val context: Context) {

    companion object {
        private const val TAG = "NOTIFICATION_BUILDER"
        private const val PACKAGE_NAME = "com.android.example.wear"
        private const val EXTRA_CANCEL_WALK_FROM_NOTIFICATION =
            "${PACKAGE_NAME}.extra.CANCEL_SUBSCRIPTION_FROM_NOTIFICATION"
        private const val NOTIFICATION_ID = 12345678
        const val NOTIFICATION_CHANNEL_ID = "walking_workout_channel_01"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    private val stepChannelExists =
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createStepChannel() {
        val titleText = context.getString(R.string.notification_title)
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            titleText,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun generateNotification(mainText: String): Notification {
        Log.d(TAG, "generateNotification")

        val titleText = context.getString(R.string.notification_title)

        if (stepChannelExists.not()) {
            createStepChannel()
        }
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)
        val launchActivityIntent = Intent(context, MainWearActivity::class.java)
        val cancelIntent = Intent(context, StepCounterService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_WALK_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            context, 0, launchActivityIntent, 0
        )
        val notificationCompatBuilder =
            NotificationCompat.Builder(
                context,
                StepCounterService.NOTIFICATION_CHANNEL_ID
            )
        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher)
            .addAction(
                R.drawable.ic_launcher, context.getString(R.string.app_name),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                context.getString(R.string.cancel_walk),
                servicePendingIntent
            )

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(mainText)
            .build()
        val ongoingActivity =
            OngoingActivity.Builder(
                context,
                NOTIFICATION_ID, notificationBuilder
            )
                .setAnimatedIcon(R.drawable.ic_launcher)
                .setStaticIcon(R.drawable.ic_launcher)
                .setTouchIntent(activityPendingIntent)
                .setStatus(ongoingActivityStatus)
                .build()

        ongoingActivity.apply(context)
        return notificationBuilder.build()
    }

/*
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun generateNotification(mainText: String): Notification {
        Log.d(StepCounterService.TAG, "generateNotification()")
        val titleText = getString(R.string.notification_title)

        val notificationChannel = NotificationChannel(
            StepCounterService.NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)
        val launchActivityIntent = Intent(this, MainWearActivity::class.java)
        val cancelIntent = Intent(this, StepCounterService::class.java)
        cancelIntent.putExtra(StepCounterService.EXTRA_CANCEL_WALK_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext,
                StepCounterService.NOTIFICATION_CHANNEL_ID
            )
        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_launcher)
            .addAction(
                R.drawable.ic_launcher, getString(R.string.app_name),
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                getString(R.string.cancel_walk),
                servicePendingIntent
            )

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(mainText)
            .build()
        val ongoingActivity =
            OngoingActivity.Builder(applicationContext,
                StepCounterService.NOTIFICATION_ID, notificationBuilder)
                .setAnimatedIcon(R.drawable.ic_launcher)
                .setStaticIcon(R.drawable.ic_launcher)
                .setTouchIntent(activityPendingIntent)
                .setStatus(ongoingActivityStatus)
                .build()

        ongoingActivity.apply(applicationContext)
        return notificationBuilder.build()
    }
*/


}
