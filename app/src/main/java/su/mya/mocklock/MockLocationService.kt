package su.mya.mocklock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MockLocationService : Service() {
	private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private lateinit var mockLocationManager: MockLocationManager
	private var playbackJob: Job? = null

	companion object {
		const val ACTION_START = "su.mya.mocklock.action.START"
		const val ACTION_STOP = "su.mya.mocklock.action.STOP"
		private const val NOTIFICATION_CHANNEL_ID = "mock_location_playback"
		private const val NOTIFICATION_ID = 1001

		fun startService(context: Context) {
			val intent = Intent(context, MockLocationService::class.java).apply {
				action = ACTION_START
			}
			context.startForegroundService(intent)
		}

		fun stopService(context: Context) {
			val intent = Intent(context, MockLocationService::class.java).apply {
				action = ACTION_STOP
			}
			context.startService(intent)
		}
	}

	override fun onCreate() {
		super.onCreate()
		mockLocationManager = MockLocationManager(this)
		createNotificationChannel()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_START -> {
				val notification = buildNotification("Preparing playback...")
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
				} else {
					startForeground(NOTIFICATION_ID, notification)
				}
				startPlayback()
			}

			ACTION_STOP -> stopPlayback()
		}
		return START_NOT_STICKY
	}

	private fun startPlayback() {
		val points = PlaybackStateHolder.trackPoints
		if (points.isEmpty()) {
			PlaybackStateHolder.updateState { it.copy(statusMessage = "No track points to play.") }
			stopPlayback()
			return
		}

		playbackJob?.cancel()
		playbackJob = serviceScope.launch {
			try {
				mockLocationManager.start()
				PlaybackStateHolder.updateState { it.copy(isPlaying = true, statusMessage = "Playing track...") }

				var previousPoint: GpxPoint? = null
				for (i in points.indices) {
					val currentPoint = points[i]
					mockLocationManager.pushLocation(currentPoint, previousPoint)
					previousPoint = currentPoint

					PlaybackStateHolder.updateState {
						it.copy(
							currentIndex = i + 1, currentLat = currentPoint.latitude, currentLon = currentPoint.longitude
						)
					}
					updateNotification("Point ${i + 1}/${points.size} (${currentPoint.latitude}, ${currentPoint.longitude})")

					if (i < points.size - 1) {
						val nextPoint = points[i + 1]
						val interval = nextPoint.timeMillis - currentPoint.timeMillis
						val sleepDuration = if (interval > 0) interval.coerceIn(100L, 10000L) else 1000L
						delay(sleepDuration.milliseconds)
					}
				}
				PlaybackStateHolder.updateState { it.copy(statusMessage = "Track playback completed.") }
			} catch (e: SecurityException) {
				PlaybackStateHolder.updateState { it.copy(statusMessage = e.message ?: "Mock location error.") }
			} catch (e: Exception) {
				PlaybackStateHolder.updateState { it.copy(statusMessage = "Error: ${e.message}") }
			} finally {
				mockLocationManager.stop()
				PlaybackStateHolder.updateState { it.copy(isPlaying = false) }
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
		}
	}

	private fun stopPlayback() {
		playbackJob?.cancel()
		mockLocationManager.stop()
		PlaybackStateHolder.updateState {
			it.copy(
				isPlaying = false, statusMessage = "Playback stopped."
			)
		}
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			NOTIFICATION_CHANNEL_ID, "Mock Location Playback", NotificationManager.IMPORTANCE_LOW
		).apply {
			description = "Active mock location playback notification"
		}
		val manager = getSystemService(NotificationManager::class.java)
		manager?.createNotificationChannel(channel)
	}

	private fun buildNotification(contentText: String): Notification {
		val pendingIntent = PendingIntent.getActivity(
			this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)

		return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setContentTitle("Mock Location Playing").setContentText(contentText)
			.setSmallIcon(R.drawable.service_icon).setContentIntent(pendingIntent).setOngoing(true).build()
	}

	private fun updateNotification(contentText: String) {
		val manager = getSystemService(NotificationManager::class.java)
		manager?.notify(NOTIFICATION_ID, buildNotification(contentText))
	}

	override fun onDestroy() {
		super.onDestroy()
		serviceScope.cancel()
		mockLocationManager.stop()
	}

	override fun onBind(intent: Intent?): IBinder? = null
}