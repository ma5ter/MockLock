package su.mya.mocklock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
	val isPlaying: Boolean = false,
	val totalPoints: Int = 0,
	val currentIndex: Int = 0,
	val currentLat: Double = 0.0,
	val currentLon: Double = 0.0,
	val statusMessage: String = "Select a GPX file to begin."
)

object PlaybackStateHolder {
	private val _uiState = MutableStateFlow(UiState())
	val uiState = _uiState.asStateFlow()

	var trackPoints: List<GpxPoint> = emptyList()

	fun updateState(transform: (UiState) -> UiState) {
		_uiState.value = transform(_uiState.value)
	}

	fun setPoints(points: List<GpxPoint>) {
		trackPoints = points
		_uiState.value = UiState(
			totalPoints = points.size, statusMessage = "Loaded ${points.size} track points."
		)
	}
}
