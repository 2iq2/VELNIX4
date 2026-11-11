package com.velnix.tv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

private val Ink = Color(0xFF0B0B0D)
private val Panel = Color(0xFF17171B)
private val Acid = Color(0xFFF4FF00)
private val Muted = Color(0xFF9A9AA3)

data class Channel(val name: String, val category: String, val now: String, val number: String)

private val demoChannels = listOf(
    Channel("VELNIX SPORTS 1", "رياضة", "أضف رابط البث الخاص بك للتشغيل", "01"),
    Channel("VELNIX SPORTS 2", "رياضة", "أضف رابط البث الخاص بك للتشغيل", "02"),
    Channel("WORLD NEWS", "أخبار", "أضف رابط البث الخاص بك للتشغيل", "03"),
    Channel("CINEMA PLUS", "عام", "أضف رابط البث الخاص بك للتشغيل", "04"),
    Channel("DOCUMENTARY", "عام", "أضف رابط البث الخاص بك للتشغيل", "05")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VelnixApp() }
    }
}

@Composable
private fun VelnixApp() {
    var selectedTab by remember { mutableStateOf("الكل") }
    var query by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    val tabs = listOf("الكل", "رياضة", "أخبار", "عام")
    val filtered = demoChannels.filter {
        (selectedTab == "الكل" || it.category == selectedTab) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Acid,
        onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VELNIX", color = Acid, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                        Text("LIVE ENTERTAINMENT", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(50)) {
                        Text("● LIVE", color = Color(0xFFFF4545), modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("شاهد ما تحب", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("قنواتك ومحتواك في مكان واحد", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث عن قناة...", color = Muted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Acid) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Acid, unfocusedBorderColor = Panel,
                        focusedContainerColor = Panel, unfocusedContainerColor = Panel
                    )
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Acid, selectedLabelColor = Ink,
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = selectedTab == tab,
                                borderColor = Panel, selectedBorderColor = Acid
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("القنوات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size} قناة", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.number }) { channel ->
                        ChannelCard(
                            channel = channel,
                            favorite = channel.name in favorites,
                            onFavorite = {
                                favorites = if (channel.name in favorites) favorites - channel.name else favorites + channel.name
                            },
                            onClick = { selectedChannel = channel }
                        )
                    }
                }
            }
        }
    }

    selectedChannel?.let { channel ->
        StreamPlayerDialog(channel = channel, onDismiss = { selectedChannel = null })
    }
}

@Composable
private fun ChannelCard(channel: Channel, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(54.dp).background(Color(0xFF26262C), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(channel.number, color = Acid, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(channel.now, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (favorite) Acid else Muted)
        }
    }
}

@Composable
private fun StreamPlayerDialog(channel: Channel, onDismiss: () -> Unit) {
    var streamUrl by remember(channel.number) { mutableStateOf("") }
    var activeUrl by remember(channel.number) { mutableStateOf<String?>(null) }
    var error by remember(channel.number) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = Panel
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(channel.name, color = Acid, fontWeight = FontWeight.Bold)
                        Text("مشغل VELNIX", color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("إغلاق", color = Muted) }
                }
                if (activeUrl != null) {
                    Media3Player(url = activeUrl!!)
                } else {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            .background(Color.Black, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("VELNIX PLAYER", color = Acid, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رابط البث HLS / DASH") },
                    placeholder = { Text("https://example.com/live.m3u8") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Acid, unfocusedBorderColor = Muted,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        val candidate = streamUrl.trim()
                        if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
                            activeUrl = candidate
                            error = null
                        } else {
                            error = "أدخل رابط بث يبدأ بـ http:// أو https://"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = Ink)
                ) { Text(if (activeUrl == null) "تشغيل القناة" else "تحديث رابط التشغيل", fontWeight = FontWeight.Bold) }
                error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                Text(
                    "استخدم روابط البث التي تملك تصريحاً باستخدامها. دعم HLS وDASH يعتمد على صيغة المصدر وتوافقه.",
                    color = Muted, fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun Media3Player(url: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            val lower = url.lowercase()
            val mime = when {
                lower.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
                lower.contains(".mpd") -> MimeTypes.APPLICATION_MPD
                else -> null
            }
            val item = MediaItem.Builder().setUri(Uri.parse(url)).apply {
                if (mime != null) setMimeType(mime)
            }.build()
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.player = player }
    )
}
