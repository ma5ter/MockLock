package su.mya.mocklock

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeParseException

data class GpxPoint(
	val latitude: Double,
	val longitude: Double,
	val timeMillis: Long,
	val altitude: Double? = null,
	val speed: Float? = null,
	val bearing: Float? = null,
	val hdop: Float? = null,
	val vdop: Float? = null,
	val pdop: Float? = null,
	val satellites: Int? = null,
	val fixType: String? = null
)

object GpxParser {
	/**
	 * Parses a GPX stream into a list of [GpxPoint] instances with location metadata.
	 *
	 * @param inputStream The GPX XML data input stream.
	 * @return List of parsed [GpxPoint] elements.
	 */
	fun parse(inputStream: InputStream): List<GpxPoint> {
		val points = mutableListOf<GpxPoint>()
		val factory = XmlPullParserFactory.newInstance()
		factory.isNamespaceAware = true
		val parser = factory.newPullParser()
		parser.setInput(inputStream, null)

		var eventType = parser.eventType
		var currentLat: Double? = null
		var currentLon: Double? = null
		var currentTimeMillis: Long? = null
		var currentElevation: Double? = null
		var currentSpeed: Float? = null
		var currentBearing: Float? = null
		var currentHdop: Float? = null
		var currentVdop: Float? = null
		var currentPdop: Float? = null
		var currentSatellites: Int? = null
		var currentFix: String? = null
		var currentTag = ""

		while (eventType != XmlPullParser.END_DOCUMENT) {
			val name = parser.name
			when (eventType) {
				XmlPullParser.START_TAG -> {
					currentTag = name
					if (name.equals("trkpt", ignoreCase = true) || name.equals("wpt", ignoreCase = true)) {
						currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
						currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
						currentTimeMillis = null
						currentElevation = null
						currentSpeed = null
						currentBearing = null
						currentHdop = null
						currentVdop = null
						currentPdop = null
						currentSatellites = null
						currentFix = null
					}
				}

				XmlPullParser.TEXT -> {
					val text = parser.text?.trim().orEmpty()
					if (text.isNotEmpty() && currentLat != null && currentLon != null) {
						when {
							currentTag.equals("time", ignoreCase = true) && currentTimeMillis == null -> {
								currentTimeMillis = try {
									Instant.parse(text).toEpochMilli()
								} catch (_: DateTimeParseException) {
									System.currentTimeMillis()
								}
							}

							currentTag.equals("ele", ignoreCase = true) -> {
								currentElevation = text.toDoubleOrNull()
							}

							currentTag.equals("speed", ignoreCase = true) -> {
								currentSpeed = text.toFloatOrNull()
							}

							currentTag.equals("course", ignoreCase = true) || currentTag.equals("bearing", ignoreCase = true) -> {
								currentBearing = text.toFloatOrNull()
							}

							currentTag.equals("hdop", ignoreCase = true) -> {
								currentHdop = text.toFloatOrNull()
							}

							currentTag.equals("vdop", ignoreCase = true) -> {
								currentVdop = text.toFloatOrNull()
							}

							currentTag.equals("pdop", ignoreCase = true) -> {
								currentPdop = text.toFloatOrNull()
							}

							currentTag.equals("sat", ignoreCase = true) -> {
								currentSatellites = text.toIntOrNull()
							}

							currentTag.equals("fix", ignoreCase = true) -> {
								currentFix = text
							}
						}
					}
				}

				XmlPullParser.END_TAG -> {
					if (name.equals("trkpt", ignoreCase = true) || name.equals("wpt", ignoreCase = true)) {
						if (currentLat != null && currentLon != null) {
							points.add(
								GpxPoint(
									latitude = currentLat,
									longitude = currentLon,
									timeMillis = currentTimeMillis ?: System.currentTimeMillis(),
									altitude = currentElevation,
									speed = currentSpeed,
									bearing = currentBearing,
									hdop = currentHdop,
									vdop = currentVdop,
									pdop = currentPdop,
									satellites = currentSatellites,
									fixType = currentFix
								)
							)
						}
						currentLat = null
						currentLon = null
						currentTimeMillis = null
						currentElevation = null
						currentSpeed = null
						currentBearing = null
						currentHdop = null
						currentVdop = null
						currentPdop = null
						currentSatellites = null
						currentFix = null
					}
					currentTag = ""
				}
			}
			eventType = parser.next()
		}
		return points
	}
}