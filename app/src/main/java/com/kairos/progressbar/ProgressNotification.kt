package com.kairos.progressbar

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ProgressNotification(private val context: Context) {

    private val CHANNEL_ID = "progress_channel"
    private val NOTIFICATION_ID = 1
    private val notificationManager = NotificationManagerCompat.from(context)

    fun showProgressNotification() {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando archivo...")
            .setContentText("Progreso: 0%")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Para que no se pueda descartar mientras progresa
            .setProgress(100, 0, false)
        checkNotificationPermission(context) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
        // Simulación de progreso (50% en 5 segundos)
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val updateTask = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    builder.setProgress(100, progress, false)
                        .setContentText("Progreso: $progress%")
                    checkNotificationPermission(context) {
                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                    progress += 10
                    handler.postDelayed(this, 500)
                } else {
                    // Notificación completada
                    builder.setContentText("Descarga completa")
                        .setProgress(0, 0, false)
                        .setOngoing(false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }
        handler.post(updateTask)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Progreso de Descarga",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el progreso de una descarga"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }


    fun checkNotificationPermission(context: Context, onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                // Solicitar permiso
                (context as? Activity)?.let { activity ->
                    ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                }
            }
        } else {
            // No se necesita permiso en versiones menores a Android 13
            onGranted()
        }
    }

}
