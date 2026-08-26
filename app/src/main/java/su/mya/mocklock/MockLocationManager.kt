package su.mya.mocklock

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock

class MockLocationManager(context: Context) {
	private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
	private val providerName = LocationManager.GPS_PROVIDER
	private var isProviderAdded = false

	@SuppressLint("WrongConstant")
	fun start() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				locationManager.addTestProvider(
					providerName,
					false,
					false,
					false,
					false,
					true,
					true,
					true,
					ProviderProperties.POWER_USAGE_LOW,
					ProviderProperties.ACCURACY_FINE
				)
			} else {
				@Suppress("DEPRECATION")
				locationManager.addTestProvider(
					providerName,
					false,
					false,
					false,
					false,
					true,
					true,
					true,
					1,
					1
				)
			}
			locationManager.setTestProviderEnabled(providerName, true)
			isProviderAdded = true
		} catch (e: SecurityException) {
			throw SecurityException("Mock Location permission not granted in Developer Options.", e)
		}
	}

	fun pushLocation(point: GpxPoint) {
		if (!isProviderAdded) return
		val mockLocation = Location(providerName).apply {
			latitude = point.latitude
			longitude = point.longitude
			altitude = 0.0
			time = System.currentTimeMillis()
			elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
			accuracy = 1.0f
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