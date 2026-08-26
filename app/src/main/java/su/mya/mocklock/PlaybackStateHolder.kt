package su.mya.mocklock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
	val isPlaying: Boolean = false,
	val totalPoints: Int = 0,
	val currentIndex: Int = 0,
	val currentLat: Double = 0.0,
	val currentLon: Double = 0.0,
	val currentDistanceMeters: Double = 0.0,
	val totalDistanceMeters: Double = 0.0,
	val currentDurationMillis: Long = 0L,
	val totalDurationMillis: Long = 0L,
	val statusMessage: String = "Select a GPX file to begin."
)

object PlaybackStateHolder {
	private val _uiState = MutableStateFlow(UiState())
	val uiState = _uiState.asStateFlow()

	var trackPoints: List<GpxPoint> = emptyList()
		private set

	var cumulativeDistances: List<Double> = emptyList()
		private set

	var cumulativeTimes: List<Long> = emptyList()
		private set

	fun updateState(transform: (UiState) -> UiState) {
		_uiState.value = transform(_uiState.value)
	}

	/**
	 * Sets the loaded GPX points and precomputes cumulative distance and time metrics.
	 *
	 * @param points The parsed list of GPX points.
	 */
	fun setPoints(points: List<GpxPoint>) {
		trackPoints = points
		cumulativeDistances = computeCumulativeDistances(points)
		cumulativeTimes = computeCumulativeTimes(points)

		val totalDist = cumulativeDistances.lastOrNull() ?: 0.0
		val totalDur = cumulativeTimes.lastOrNull() ?: 0L
		val firstPoint = points.firstOrNull()

		_uiState.value = UiState(
			totalPoints = points.size,
			currentIndex = if (points.isNotEmpty()) 1 else 0,
			currentLat = firstPoint?.latitude ?: 0.0,
			currentLon = firstPoint?.longitude ?: 0.0,
			currentDistanceMeters = 0.0,
			totalDistanceMeters = totalDist,
			currentDurationMillis = 0L,
			totalDurationMillis = totalDur,
			statusMessage = "Loaded ${points.size} track points."
		)
	}

	/**
	 * Seeks playback position to the given 1-based index and updates state metrics.
	 *
	 * @param index 1-based point index.
	 */
	fun seekTo(index: Int) {
		if (_uiState.value.isPlaying || trackPoints.isEmpty()) return
		val clampedIndex = index.coerceIn(1, trackPoints.size)
		val pointIdx = clampedIndex - 1
		val point = trackPoints[pointIdx]
		val dist = cumulativeDistances.getOrElse(pointIdx) { 0.0 }
		val dur = cumulativeTimes.getOrElse(pointIdx) { 0L }

		_uiState.value = _uiState.value.copy(
			currentIndex = clampedIndex, currentLat = point.latitude, currentLon = point.longitude, currentDistanceMeters = dist, currentDurationMillis = dur
		)
	}

	/**
	 * Calculates cumulative distance in meters along the track for each point.
	 */
	private fun computeCumulativeDistances(points: List<GpxPoint>): List<Double> {
		if (points.isEmpty()) return emptyList()
		val distances = ArrayList<Double>(points.size)
		var runningDistance = 0.0
		distances.add(0.0)

		for (i in 1 until points.size) {
			val prev = points[i - 1]
			val curr = points[i]
			runningDistance += DistanceUtils.getDistance(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
			distances.add(runningDistance)
		}
		return distances
	}

	/**
	 * Calculates cumulative elapsed time in milliseconds for each point.
	 */
	private fun computeCumulativeTimes(points: List<GpxPoint>): List<Long> {
		if (points.isEmpty()) return emptyList()
		val times = ArrayList<Long>(points.size)
		var runningTime = 0L
		times.add(0L)

		for (i in 1 until points.size) {
			val prev = points[i - 1]
			val curr = points[i]
			val interval = curr.timeMillis - prev.timeMillis
			val stepDuration = if (interval > 0) interval.coerceIn(100L, 10000L) else 1000L
			runningTime += stepDuration
			times.add(runningTime)
		}
		return times
	}
}