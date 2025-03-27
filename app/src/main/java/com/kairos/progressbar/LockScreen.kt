package com.kairos.progressbar

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class LockScreen : AppCompatActivity() {

    private val CHANNEL_ID = "progress_channel"
    private val NOTIFICATION_ID = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Mostrar la notificación con ProgressBar
        showProgressNotification()
    }

    private fun showProgressNotification() {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = NotificationManagerCompat.from(this)

        // Construcción inicial de la notificación con ProgressBar
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando archivo...")
            .setContentText("Progreso: 0%")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Notificación importante
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración, LED
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Mostrar en pantalla de bloqueo
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true) // Evita que el usuario la descarte
            .setProgress(100, 0, false)

        checkNotificationPermission(applicationContext){
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
        // Enciende la pantalla si está bloqueada
        wakeUpScreen()

        // Simulación de carga de progreso
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val updateTask = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    builder.setProgress(100, progress, false)
                        .setContentText("Progreso: $progress%")
                    checkNotificationPermission(applicationContext){
                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                    progress += 10
                    handler.postDelayed(this, 500)
                } else {
                    // Notificación finalizada
                    builder.setContentText("Descarga completa")
                        .setProgress(0, 0, false)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setOngoing(false)
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
                "Notificaciones con ProgressBar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones con barra de progreso"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp:WakeLock"
        )
        wakeLock.acquire(3000) // Enciende la pantalla por 3 segundos
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