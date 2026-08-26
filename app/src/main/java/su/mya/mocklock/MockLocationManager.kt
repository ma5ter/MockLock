package su.mya.mocklock

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MockLocationManager(context: Context) {
	private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
	private val providerName = LocationManager.GPS_PROVIDER
	private var isProviderAdded = false

	@SuppressLint("WrongConstant")
	fun start() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				locationManager.addTestProvider(
					providerName, false, true, false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
				)
			} else {
				@Suppress("DEPRECATION") locationManager.addTestProvider(
					providerName, false, true, false, false, true, true, true, 1, 1
				)
			}
			locationManager.setTestProviderEnabled(providerName, true)
			isProviderAdded = true
		} catch (e: SecurityException) {
			throw SecurityException("Mock Location permission not granted in Developer Options.", e)
		}
	}

	/**
	 * Injects a mock location with full receiver parameters and realism approximations.
	 *
	 * @param point Current GPX point being played.
	 * @param previousPoint Preceding point in the track, if available, for differential calculations.
	 */
	fun pushLocation(point: GpxPoint, previousPoint: GpxPoint? = null) {
		if (!isProviderAdded) return

		var calculatedSpeed = point.speed
		var calculatedBearing = point.bearing

		if (previousPoint != null) {
			val results = FloatArray(2)
			Location.distanceBetween(
				previousPoint.latitude, previousPoint.longitude, point.latitude, point.longitude, results
			)
			val distanceMeters = results[0]
			val initialBearing = (results[1] + 360f) % 360f

			if (calculatedBearing == null && distanceMeters > 0.1f) {
				calculatedBearing = initialBearing
			}

			if (calculatedSpeed == null) {
				val timeDeltaSeconds = (point.timeMillis - previousPoint.timeMillis) / 1000.0f
				if (timeDeltaSeconds > 0.05f) {
					calculatedSpeed = (distanceMeters / timeDeltaSeconds).coerceIn(0.0f, 150.0f)
				}
			}
		}

		val speed = calculatedSpeed ?: 0.0f
		val bearing = calculatedBearing ?: 0.0f
		val altitude = point.altitude ?: 0.0

		// GPS standard user equivalent range error (UERE) ~ 3.5m - 4.0m
		val hdop = point.hdop ?: 0.9f
		val vdop = point.vdop ?: (hdop * 1.4f)
		val pdop = point.pdop ?: sqrt((hdop * hdop + vdop * vdop).toDouble()).toFloat()

		// Estimate satellite count from HDOP: high precision (HDOP < 1.0) ~ 12-18 sats
		val satelliteCount = point.satellites ?: ((20f - (hdop * 3.0f)).roundToInt()).coerceIn(6, 18)
		val satellitesInView = (satelliteCount * 1.3f).roundToInt().coerceAtLeast(satelliteCount + 2)

		val horizontalAccuracy = (hdop * 3.5f).coerceIn(0.8f, 25.0f)
		val verticalAccuracy = (vdop * 4.5f).coerceIn(1.2f, 35.0f)

		val mockLocation = Location(providerName).apply {
			latitude = point.latitude
			longitude = point.longitude
			this.altitude = altitude
			time = System.currentTimeMillis()
			elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

			this.speed = speed
			this.bearing = bearing
			accuracy = horizontalAccuracy

			verticalAccuracyMeters = verticalAccuracy
			speedAccuracyMetersPerSecond = 0.3f
			bearingAccuracyDegrees = 3.0f

			// Vendor and standard GNSS metadata extras
			extras = Bundle().apply {
				// === Official / most widely supported ===
				putInt("satellites", satelliteCount) // Official Android key (deprecated in API 34 but still used)
				// === Common variants for "used in fix" ===
				putInt("satellitesUsed", satelliteCount)
				putInt("satellites_used_in_fix", satelliteCount)
				putInt("used_in_fix", satelliteCount)
				putInt("sat_count", satelliteCount)
				putInt("sats", satelliteCount)
				// === Satellites in view / tracked ===
				putInt("satellitesInView", satellitesInView)
				putInt("totalSatInView", satellitesInView)
				putInt("satellitesView", satellitesInView)
				putInt("satellites_in_view", satellitesInView)
				// === Dilution of Precision (very common) ===
				putFloat("hdop", hdop)
				putFloat("HDOP", hdop)
				putFloat("Hdop", hdop)
				putFloat("vdop", vdop)
				putFloat("VDOP", vdop)
				putFloat("pdop", pdop)
				putFloat("PDOP", pdop)
				// === Accuracy / RMS (used by professional receivers) ===
				putFloat("hrms", horizontalAccuracy)
				putFloat("HRMS", horizontalAccuracy)
				putFloat("2drms", horizontalAccuracy)
				putFloat("vrms", verticalAccuracy)
				putFloat("VRMS", verticalAccuracy)
				putFloat("3drms", sqrt(horizontalAccuracy * horizontalAccuracy + verticalAccuracy * verticalAccuracy))
				// === Fix quality / differential status ===
				putInt("diffStatus", 1) // 1=Autonomous, 2=DGPS, 4=Fixed, 5=Float
				putInt("status", 1)
				val ft = point.fixType ?: if (altitude != 0.0) "3d" else "2d"
				putString("fixType", ft)
				putString("fix_type", ft)
				// === Other useful metadata ===
				putFloat("meanCn0", 35f) // AOSP sometimes provides this
				putFloat("maxCn0", 45f)
			}
		}

		locationManager.setTestProviderLocation(providerName, mockLocation)
	}

	fun stop() {
		if (isProviderAdded) {
			try {
				locationManager.setTestProviderEnabled(providerName, false)
				locationManager.removeTestProvider(providerName)
			} catch (_: Exception) {
			} finally {
				isProviderAdded = false
			}
		}
	}
}