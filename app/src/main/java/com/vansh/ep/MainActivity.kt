@file:OptIn(ExperimentalMaterial3Api::class)
package com.vansh.ep

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.vansh.ep.backend.Personalizer
import com.vansh.ep.models.GestureData
import com.vansh.ep.models.Song
import com.vansh.ep.pipeline.VibePipeline
import com.vansh.ep.ui.theme.EmaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = "http://127.0.0.1:8888/callback"

    private val playerState = mutableStateOf<PlayerState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showIntro by rememberSaveable { mutableStateOf(true) }
                    
                    if (showIntro) {
                        IntroDarkScreen(onFinished = { showIntro = false })
                    } else {
                        VibeEngineScreen(
                            pState = playerState.value,
                            onPlaySong = { song -> playSpotifySong(song) },
                            onTogglePlay = { togglePlayback() },
                            onSkipNext = { skipNext() },
                            onSkipPrev = { skipPrevious() },
                            onSeek = { pos -> seekTo(pos) }
                        )
                    }
                }
            }
        }
    }

    private fun playSpotifySong(song: Song) {
        val remote = spotifyAppRemote
        if (remote != null && remote.isConnected) {
            remote.playerApi.play("spotify:track:${song.id}")
                .setErrorCallback { openExternalSpotify(song.id) }
        } else {
            connectSpotify {
                val connectedRemote = spotifyAppRemote
                if (connectedRemote != null && connectedRemote.isConnected) {
                    connectedRemote.playerApi.play("spotify:track:${song.id}")
                        .setErrorCallback { openExternalSpotify(song.id) }
                } else {
                    openExternalSpotify(song.id)
                }
            }
        }
    }

    private fun openExternalSpotify(trackId: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:$trackId"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() 
        } catch (e: Exception) {
            Toast.makeText(this, "Spotify app not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayback() {
        val remote = spotifyAppRemote ?: return
        val state = playerState.value ?: return
        if (state.isPaused) remote.playerApi.resume() else remote.playerApi.pause()
    }

    private fun skipNext() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

    private fun skipPrevious() {
        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    private fun seekTo(pos: Long) {
        spotifyAppRemote?.playerApi?.seekTo(pos)
    }

    private fun connectSpotify(onSuccess: (() -> Unit)? = null) {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                spotifyAppRemote = remote
                remote.playerApi.subscribeToPlayerState().setEventCallback(object : Subscription.EventCallback<PlayerState> {
                    override fun onEvent(state: PlayerState) {
                        runOnUiThread { 
                            playerState.value = state 
                        }
                    }
                })
                onSuccess?.invoke()
            }
            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", "Spotify Fail")
                onSuccess?.invoke()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        connectSpotify()
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
    }
}

@Composable
fun IntroDarkScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(0.8f, tween(1500, easing = EaseInOutQuart))
        delay(800)
        alpha.animateTo(0f, tween(1200, easing = EaseInOutQuart))
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF030303)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(100.dp).alpha(alpha.value),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun RandomRotatedGestureSwipeHint(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var shapeType by rememberSaveable { mutableStateOf("wave") }
    var rotationAngle by rememberSaveable { mutableFloatStateOf(0f) }
    var hintCount by rememberSaveable { mutableIntStateOf(0) }

    suspend fun playGesture() {
        if (hintCount >= 2) { onFinished(); return }
        shapeType = listOf("ellipse", "wave", "line", "spiral", "infinity").random()
        rotationAngle = Random.nextInt(0, 360).toFloat()
        alpha.animateTo(0.3f, tween(800))
        progress.animateTo(1f, tween(2000, easing = LinearEasing))
        alpha.animateTo(0f, tween(1000))
        progress.snapTo(0f)
        hintCount++
        if (hintCount >= 2) onFinished()
    }

    LaunchedEffect(Unit) {
        while (hintCount < 2) {
            playGesture()
            if (hintCount < 2) delay(1000)
        }
    }

    if (hintCount < 2) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val path = Path()
            rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
                when (shapeType) {
                    "ellipse" -> {
                        val rect = Rect(centerX - 250f, centerY - 150f, centerX + 250f, centerY + 150f)
                        path.arcTo(rect, 0f, 180f * progress.value, false)
                    }
                    "wave" -> {
                        val amplitude = 360f
                        val fullLength = 600f
                        val startX = centerX - fullLength / 2
                        val startAngle = PI.toFloat() * 1.5f 
                        path.moveTo(startX, centerY + (amplitude * sin(startAngle)))
                        for (i in 1..100) {
                            val p = i.toFloat() / 100f
                            if (p > progress.value) break
                            val angle = startAngle + (PI.toFloat() * 1.0f) * p 
                            path.lineTo(startX + fullLength * p, centerY + (amplitude * sin(angle)))
                        }
                    }
                    "line" -> {
                        path.moveTo(centerX - 200f, centerY)
                        path.lineTo(centerX - 200f + (400f * progress.value), centerY)
                    }
                    "spiral" -> {
                        val maxRadius = 150f
                        path.moveTo(centerX, centerY)
                        for (i in 1..200) {
                            val p = i.toFloat() / 200f
                            if (p > progress.value) break
                            val angle = p * PI.toFloat() * 4f
                            val radius = p * maxRadius
                            path.lineTo(centerX + radius * cos(angle), centerY + radius * sin(angle))
                        }
                    }
                    "infinity" -> {
                        val a = 250f
                        for (i in 0..100) {
                            val p = i.toFloat() / 100f
                            if (p > progress.value) break
                            val t = p * PI.toFloat() * 2f
                            val denom = 1 + sin(t) * sin(t)
                            val x = (a * cos(t)) / denom
                            val y = (a * sin(t) * cos(t)) / denom
                            if (i == 0) path.moveTo(centerX + x, centerY + y)
                            else path.lineTo(centerX + x, centerY + y)
                        }
                    }
                }
                drawPath(path, Color.Gray.copy(alpha = alpha.value), style = Stroke(width = 45f, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
fun SongItem(song: Song, isPlaying: Boolean, onClick: () -> Unit, onFeedback: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Row {
                IconButton(onClick = { onFeedback(false) }) { Icon(Icons.Default.ThumbDown, null, Modifier.size(20.dp), tint = Color.Gray) }
                IconButton(onClick = { onFeedback(true) }) { Icon(Icons.Default.ThumbUp, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun VibeEngineScreen(
    pState: PlayerState?,
    onPlaySong: (Song) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val personalizer = remember { Personalizer(context) }

    var isEngineEnabled by rememberSaveable { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isFusing by remember { mutableStateOf(false) }
    
    var songList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var hintsFinished by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("ema_prefs", Context.MODE_PRIVATE) }
    val hasBeenRedirected = remember { mutableStateOf(prefs.getBoolean("has_redirected", false)) }

    val xCoords = remember { mutableStateListOf<Float>() }
    val yCoords = remember { mutableStateListOf<Float>() }
    val pressures = remember { mutableStateListOf<Float>() }

    val activeAlpha = remember { Animatable(if (hintsFinished) 1f else 0f) }
    LaunchedEffect(hintsFinished) {
        if (hintsFinished) activeAlpha.animateTo(1f, tween(1000))
        else activeAlpha.snapTo(0f)
    }

    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var processingTime by rememberSaveable { mutableLongStateOf(10000L) }

    val gpsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { _ -> }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            // ONLY trigger GPS settings popup if permission was JUST granted (covers "Allow this time")
            val lr = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(lr)
            val client = LocationServices.getSettingsClient(context)
            client.checkLocationSettings(builder.build()).addOnSuccessListener { isEngineEnabled = true }
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try { gpsLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build()) } catch (_: Exception) { }
                    }
                }
        } else if (!hasBeenRedirected.value) {
            // First time they click "Don't allow" -> Redirect to settings
            Toast.makeText(context, "Location required for full vibe syncing.", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) })
            hasBeenRedirected.value = true
            prefs.edit().putBoolean("has_redirected", true).apply()
        }
        isEngineEnabled = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 20.dp, end = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("search song...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        if (searchQuery.isNotEmpty()) {
                            scope.launch { 
                                isFusing = true
                                val result = withTimeoutOrNull(processingTime) {
                                    personalizer.learnFromSearch(searchQuery)
                                    VibePipeline.execute(context, manualQuery = searchQuery)
                                }
                                if (result != null) { songList = result }
                                isFusing = false 
                            }
                        }
                    })
                )
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(checked = isEngineEnabled, onCheckedChange = { checked ->
                        if (checked) {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (fineLoc != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                isEngineEnabled = true 
                            } else {
                                isEngineEnabled = true
                            }
                        } else {
                            isEngineEnabled = false
                        }
                    })
                    Text("Engine", style = MaterialTheme.typography.labelSmall)
                }
            }

            AnimatedVisibility(visible = isEngineEnabled && songList.isEmpty() && !isRecording && !isFusing, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Text("swipe to get a song...", modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(32.dp))
                    .background(if (isRecording) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .pointerInput(isEngineEnabled) {
                        if (!isEngineEnabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                if (!isRecording && !isFusing) {
                                    isRecording = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    xCoords.clear(); yCoords.clear(); pressures.clear()
                                    scope.launch {
                                        delay(2000)
                                        isRecording = false
                                        isFusing = true
                                        val result = withTimeoutOrNull(processingTime) {
                                            VibePipeline.execute(context, gestureData = GestureData(xCoords.toList(), yCoords.toList(), pressures.toList(), System.currentTimeMillis()))
                                        }
                                        if (result != null) { songList = result }
                                        isFusing = false
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                if (isRecording) { xCoords.add(change.position.x); yCoords.add(change.position.y); pressures.add(change.pressure) }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isEngineEnabled && songList.isEmpty() && !isRecording && !isFusing) {
                    if (!hintsFinished) RandomRotatedGestureSwipeHint { hintsFinished = true }
                    else Text("Energy Field is active", modifier = Modifier.alpha(activeAlpha.value), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal, fontSize = 16.sp, textAlign = TextAlign.Center)
                }

                if (isFusing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Fetching vibes...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                else if (songList.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        itemsIndexed(songList) { _, song ->
                            val isPlaying = pState?.track?.uri?.contains(song.id) == true
                            SongItem(song, isPlaying, { 
                                scope.launch { personalizer.learnFromPlayedSong(song) }
                                onPlaySong(song) 
                            }, { liked -> personalizer.saveFeedback(song.primaryGenre, liked) })
                        }
                    }
                } else if (!isEngineEnabled) { Text("Enable Engine", color = Color.Gray) }
            }
        }

        if (pState != null && !isPlayerExpanded) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)) {
                MiniPlayer(pState, onExpand = { isPlayerExpanded = true }, onTogglePlay = onTogglePlay)
            }
        }

        if (songList.isNotEmpty() && !isPlayerExpanded) {
            IconButton(
                onClick = { songList = emptyList(); searchQuery = ""; hintsFinished = false }, 
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 140.dp, end = 24.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
            ) { Icon(Icons.Default.Clear, null, tint = Color.White) }
        }

        FullPlayerOverlay(visible = isPlayerExpanded, playerState = pState, onCollapse = { isPlayerExpanded = false }, onTogglePlay = onTogglePlay, onSkipNext = onSkipNext, onSkipPrev = onSkipPrev, onSeek = onSeek)
    }
}

@Composable
fun MiniPlayer(state: PlayerState, onExpand: () -> Unit, onTogglePlay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp, vertical = 4.dp).clickable { onExpand() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(state.track?.name ?: "Unknown Track", fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                Text(state.track?.artist?.name ?: "Unknown Artist", fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            }
            IconButton(onClick = onTogglePlay) { Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null, tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
fun FullPlayerOverlay(visible: Boolean, playerState: PlayerState?, onCollapse: () -> Unit, onTogglePlay: () -> Unit, onSkipNext: () -> Unit, onSkipPrev: () -> Unit, onSeek: (Long) -> Unit) {
    var manualPosition by rememberSaveable { mutableLongStateOf(0L) }
    val actualPosition = playerState?.playbackPosition ?: 0L
    
    LaunchedEffect(actualPosition) { manualPosition = actualPosition }
    LaunchedEffect(playerState?.isPaused) {
        if (playerState != null && !playerState.isPaused) {
            while (true) {
                delay(1000)
                manualPosition += 1000
            }
        }
    }

    AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCollapse, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(32.dp)) }
                Spacer(Modifier.height(40.dp))
                Box(Modifier.size(280.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                }
                Spacer(Modifier.height(40.dp))
                Text(playerState?.track?.name ?: "Unknown Track", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(playerState?.track?.artist?.name ?: "Unknown Artist", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                val duration = playerState?.track?.duration ?: 0L
                val progress = if (duration > 0) (manualPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                Slider(value = progress, onValueChange = { p -> 
                    val newPos = (p * duration).toLong()
                    manualPosition = newPos
                    onSeek(newPos)
                }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(manualPosition), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSkipPrev) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(48.dp)) }
                    FloatingActionButton(onClick = onTogglePlay, containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape) { Icon(if (playerState?.isPaused == true) Icons.Default.PlayArrow else Icons.Default.Pause, null, Modifier.size(32.dp)) }
                    IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, null, Modifier.size(48.dp)) }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}
