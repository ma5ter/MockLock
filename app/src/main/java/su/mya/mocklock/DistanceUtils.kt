package su.mya.mocklock

import java.text.MessageFormat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceUtils {

	private const val METERS_IN_KILOMETER = 1000f

	private fun toRadians(degree: Double): Double {
		return degree / 180.0 * Math.PI
	}

	/**
	 * Gets distance in meters
	 */
	fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
		val R = 6372.8 // for haversine use R = 6372.8 km instead of 6371 km
		val dLat = toRadians(lat2 - lat1)
		val dLon = toRadians(lon2 - lon1)
		val a = sin(dLat / 2) * sin(dLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
		return 2.0 * R * 1000.0 * asin(sqrt(a))
	}

	/**
	 * Formats distance in meters into human-readable string representation.
	 */
	fun getFormattedDistance(meters: Double): String {
		val format1 = "{0,number,0.0} "
		val format2 = "{0,number,0.00} "

		val mainUnitStr = "km"
		val mainUnitInMeters = METERS_IN_KILOMETER

		return when {
			meters >= 100 * mainUnitInMeters -> "${(meters / mainUnitInMeters + 0.5).toInt()} $mainUnitStr"
			meters > 9.99f * mainUnitInMeters -> MessageFormat.format(format1 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ')
			meters > 0.999f * mainUnitInMeters -> MessageFormat.format(format2 + mainUnitStr, meters / mainUnitInMeters).replace('\n', ' ')
			else -> "${(meters + 0.5).toInt()} m"
		}
	}
}