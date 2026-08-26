package su.mya.mocklock

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.Instant
import java.time.format.DateTimeParseException

data class GpxPoint(
	val latitude: Double,
	val longitude: Double,
	val timeMillis: Long
)

object GpxParser {
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
					}
				}

				XmlPullParser.TEXT -> {
					if (currentTag.equals("time", ignoreCase = true) && currentLat != null && currentLon != null) {
						val text = parser.text.trim()
						if (text.isNotEmpty() && currentTimeMillis == null) {
							currentTimeMillis = try {
								Instant.parse(text).toEpochMilli()
							} catch (e: DateTimeParseException) {
								System.currentTimeMillis()
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
									timeMillis = currentTimeMillis ?: System.currentTimeMillis()
								)
							)
						}
						currentLat = null
						currentLon = null
						currentTimeMillis = null
					}
					currentTag = ""
				}
			}
			eventType = parser.next()
		}
		return points
	}
}