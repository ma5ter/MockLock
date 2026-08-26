package su.mya.mocklock

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MockLocationViewModel(application: Application) : AndroidViewModel(application) {
	val uiState: StateFlow<UiState> = PlaybackStateHolder.uiState

	fun loadGpx(uri: Uri) {
		viewModelScope.launch {
			try {
				val points = withContext(Dispatchers.IO) {
					getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
						GpxParser.parse(stream)
					} ?: emptyList()
				}

				if (points.isNotEmpty()) {
					PlaybackStateHolder.setPoints(points)
				} else {
					PlaybackStateHolder.updateState { it.copy(statusMessage = "No points found in GPX file.") }
				}
			} catch (e: Exception) {
				PlaybackStateHolder.updateState { it.copy(statusMessage = "Error parsing file: ${e.message}") }
			}
		}
	}

	fun startPlayback() {
		MockLocationService.startService(getApplication())
	}

	fun stopPlayback() {
		MockLocationService.stopService(getApplication())
	}
}