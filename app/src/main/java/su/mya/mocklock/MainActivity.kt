package su.mya.mocklock

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
	private val viewModel: MockLocationViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		handleIntent(intent)

		setContent {
			val darkTheme = isSystemInDarkTheme()
			val context = LocalContext.current
			val colorScheme = when {
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
					if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
				}

				darkTheme -> darkColorScheme()
				else -> lightColorScheme()
			}

			MaterialTheme(colorScheme = colorScheme) {
				Surface(
					modifier = Modifier
						.fillMaxSize()
						.safeDrawingPadding(),
					color = MaterialTheme.colorScheme.background
				) {
					MockLocationScreen(viewModel)
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		handleIntent(intent)
	}

	private fun handleIntent(intent: Intent?) {
		if (intent?.action == Intent.ACTION_VIEW) {
			intent.data?.let { uri ->
				viewModel.loadGpx(uri)
			}
		}
	}
}

private fun hasLocationPermission(context: Context): Boolean {
	val fineLocation = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.ACCESS_FINE_LOCATION
	) == PackageManager.PERMISSION_GRANTED
	val coarseLocation = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.ACCESS_COARSE_LOCATION
	) == PackageManager.PERMISSION_GRANTED
	return fineLocation || coarseLocation
}

@Composable
fun MockLocationScreen(viewModel: MockLocationViewModel) {
	val context = LocalContext.current
	val uiState by viewModel.uiState.collectAsState()

	val permissionsToRequest = mutableListOf(
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.ACCESS_COARSE_LOCATION
	).apply {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			add(Manifest.permission.POST_NOTIFICATIONS)
		}
	}.toTypedArray()

	val permissionLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
				permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
		if (locationGranted) {
			viewModel.startPlayback()
		} else {
			Toast.makeText(context, "Location permission is required for mock playback service", Toast.LENGTH_LONG).show()
		}
	}

	LaunchedEffect(Unit) {
		if (!hasLocationPermission(context)) {
			permissionLauncher.launch(permissionsToRequest)
		}
	}

	val filePickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri ->
		uri?.let { viewModel.loadGpx(it) }
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			text = "GPX Mock Location Player",
			style = MaterialTheme.typography.headlineSmall
		)

		Spacer(modifier = Modifier.height(24.dp))

		Button(
			onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
			enabled = !uiState.isPlaying,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Open GPX File")
		}

		Spacer(modifier = Modifier.height(16.dp))

		Card(
			modifier = Modifier.fillMaxWidth()
		) {
			Column(modifier = Modifier.padding(16.dp)) {
				Text(text = "Status: ${uiState.statusMessage}")
				Spacer(modifier = Modifier.height(8.dp))
				Text(text = "Total Points: ${uiState.totalPoints}")
				Text(text = "Progress: ${uiState.currentIndex} / ${uiState.totalPoints}")
				Spacer(modifier = Modifier.height(8.dp))
				Text(text = "Latitude: ${uiState.currentLat}")
				Text(text = "Longitude: ${uiState.currentLon}")
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Button(
				onClick = {
					if (hasLocationPermission(context)) {
						viewModel.startPlayback()
					} else {
						permissionLauncher.launch(permissionsToRequest)
					}
				},
				enabled = !uiState.isPlaying && uiState.totalPoints > 0,
				modifier = Modifier.weight(1f)
			) {
				Text("Start")
			}

			Button(
				onClick = { viewModel.stopPlayback() },
				enabled = uiState.isPlaying,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.error
				),
				modifier = Modifier.weight(1f)
			) {
				Text("Stop")
			}
		}
	}
}