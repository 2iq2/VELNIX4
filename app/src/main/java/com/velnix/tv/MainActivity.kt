package com.velnix.tv

import android.net.Uri
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF0B0B0D)
private val Panel = Color(0xFF17171B)
private val Acid = Color(0xFFF4FF00)
private val Muted = Color(0xFF9A9AA3)
private const val PREFS_NAME = "velnix_preferences"
private const val KEY_PLAYLIST = "playlist_m3u"
private const val KEY_FAVORITES = "favorite_channels"
private const val KEY_XMLTV = "epg_xmltv"
private const val KEY_REMINDERS = "program_reminders"
private const val KEY_PROFILE_NAME = "profile_name"
private const val KEY_PROFILE_EMAIL = "profile_email"

data class Channel(
    val name: String,
    val category: String,
    val now: String,
    val number: String,
    val streamUrl: String? = null,
    val logoUrl: String? = null,
    val tvgId: String? = null
)

private fun favoriteKey(channel: Channel): String = channel.streamUrl ?: channel.name

private fun displayCategory(group: String?): String {
    val value = group?.trim().orEmpty()
    val normalized = value.lowercase()
    return when {
        value.isBlank() -> "عام"
        listOf("sport", "football", "soccer", "bein", "رياض").any { normalized.contains(it) } -> "رياضة"
        listOf("news", "خبر", "أخبار").any { normalized.contains(it) } -> "أخبار"
        listOf("general", "entertainment", "movie", "cinema", "عام").any { normalized.contains(it) } -> "عام"
        else -> value
    }
}

private val demoChannels = listOf(
    Channel("VELNIX SPORTS 1", "رياضة", "استورد قائمة M3U لربط البث", "01"),
    Channel("VELNIX SPORTS 2", "رياضة", "استورد قائمة M3U لربط البث", "02"),
    Channel("WORLD NEWS", "أخبار", "استورد قائمة M3U لربط البث", "03"),
    Channel("CINEMA PLUS", "عام", "استورد قائمة M3U لربط البث", "04"),
    Channel("DOCUMENTARY", "عام", "استورد قائمة M3U لربط البث", "05")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VelnixApp() }
    }
}

@Composable
private fun VelnixApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var selectedTab by remember { mutableStateOf("الكل") }
    var query by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showEpgGuide by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf(prefs.getString(KEY_PROFILE_NAME, "") ?: "") }
    var profileEmail by remember { mutableStateOf(prefs.getString(KEY_PROFILE_EMAIL, "") ?: "") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var epgPrograms by remember { mutableStateOf(emptyList<com.velnix.tv.domain.model.EpgProgram>()) }
    var epgMessage by remember { mutableStateOf<String?>(null) }
    var nowUtcMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var importedChannels by remember {
        mutableStateOf(
            prefs.getString(KEY_PLAYLIST, null)?.let { raw ->
                com.velnix.tv.data.iptv.M3uParser.parse(raw).mapIndexed { index, item ->
                    Channel(
                        name = item.name,
                        category = displayCategory(item.group),
                        now = "قناة مستوردة",
                        number = (index + 1).toString().padStart(2, '0'),
                        streamUrl = item.streamUrl,
                        logoUrl = item.logoUrl,
                        tvgId = item.id
                    )
                }
            } ?: emptyList()
        )
    }
    var favorites by remember {
        mutableStateOf(prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet())
    }
    val tabs = listOf("الكل", "رياضة", "أخبار", "عام", "المفضلة")
    val allChannels = if (importedChannels.isNotEmpty()) importedChannels else demoChannels
    val filtered = allChannels.filter { channel ->
        (selectedTab == "الكل" ||
            (selectedTab == "المفضلة" && favoriteKey(channel) in favorites) ||
            (selectedTab != "المفضلة" && channel.category.contains(selectedTab, ignoreCase = true))) &&
            (query.isBlank() || channel.name.contains(query, ignoreCase = true))
    }

    val playlistPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                importMessage = null
                try {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("تعذرت قراءة الملف")
                    }
                    val parsed = com.velnix.tv.data.iptv.M3uParser.parse(raw)
                    if (parsed.isEmpty()) {
                        importMessage = "الملف لا يحتوي قنوات M3U صالحة."
                    } else {
                        importedChannels = parsed.mapIndexed { index, item ->
                            Channel(
                                name = item.name,
                                category = displayCategory(item.group),
                                now = "قناة مستوردة",
                                number = (index + 1).toString().padStart(2, '0'),
                                streamUrl = item.streamUrl,
                                logoUrl = item.logoUrl,
                                tvgId = item.id
                            )
                        }
                        withContext(Dispatchers.IO) {
                            prefs.edit().putString(KEY_PLAYLIST, raw).apply()
                        }
                        selectedTab = "الكل"
                        importMessage = "تم استيراد ${parsed.size} قناة وحفظ القائمة على الجهاز."
                    }
                } catch (exception: Exception) {
                    importMessage = "فشل استيراد القائمة: ${exception.localizedMessage ?: "خطأ غير معروف"}"
                } finally {
                    isImporting = false
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        val savedXmltv = withContext(Dispatchers.IO) { prefs.getString(KEY_XMLTV, null) }
        if (!savedXmltv.isNullOrBlank()) {
            epgPrograms = withContext(Dispatchers.Default) {
                com.velnix.tv.data.epg.XmltvParser.parse(savedXmltv)
            }
            epgMessage = "دليل البرامج المحفوظ: ${epgPrograms.size} برنامج."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowUtcMillis = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val epgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                epgMessage = "جارٍ تحميل دليل البرامج..."
                try {
                    val xml = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("تعذرت قراءة ملف XMLTV")
                    }
                    val parsed = withContext(Dispatchers.Default) {
                        com.velnix.tv.data.epg.XmltvParser.parse(xml)
                    }
                    if (parsed.isEmpty()) {
                        epgMessage = "الملف لا يحتوي برامج صالحة."
                    } else {
                        withContext(Dispatchers.IO) {
                            prefs.edit().putString(KEY_XMLTV, xml).apply()
                        }
                        epgPrograms = parsed
                        epgMessage = "تم تحميل ${parsed.size} برنامج من دليل XMLTV."
                    }
                } catch (exception: Exception) {
                    epgMessage = "تعذر تحميل الدليل: ${exception.localizedMessage ?: "خطأ غير معروف"}"
                }
            }
        }
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
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(color = Panel, shape = RoundedCornerShape(50)) {
                            Text("● LIVE", color = Color(0xFFFF4545), modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { showAccount = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Acid)
                        ) {
                            Text(if (profileName.isBlank()) "حسابي" else profileName, maxLines = 1, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("شاهد ما تحب", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("قنواتك ومحتواك في مكان واحد", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { playlistPicker.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "application/x-mpegURL", "text/plain", "*/*")) },
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = Ink)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isImporting) "جارٍ استيراد القائمة..." else "استيراد قائمة M3U", fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { epgPicker.launch(arrayOf("application/xml", "text/xml", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Acid)
                    ) { Text("استيراد XMLTV", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { showEpgGuide = true },
                        enabled = epgPrograms.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Acid)
                    ) { Text("دليل البرامج", fontSize = 11.sp) }
                }
                importMessage?.let { Text(it, color = if (it.startsWith("فشل") || it.startsWith("الملف")) Color(0xFFFF7777) else Acid, fontSize = 11.sp) }
                epgMessage?.let { Text(it, color = if (it.startsWith("تعذر") || it.startsWith("الملف")) Color(0xFFFF7777) else Acid, fontSize = 11.sp) }
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    tabs.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab, fontSize = 11.sp) },
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
                    items(filtered, key = { it.number + it.name }) { channel ->
                        ChannelCard(
                            channel = channel,
                            epg = findChannelEpg(channel, epgPrograms, nowUtcMillis),
                            favorite = favoriteKey(channel) in favorites,
                            onFavorite = {
                                favorites = if (favoriteKey(channel) in favorites) favorites - favoriteKey(channel) else favorites + favoriteKey(channel)
                                prefs.edit().putStringSet(KEY_FAVORITES, favorites.toSet()).apply()
                            },
                            onClick = { selectedChannel = channel }
                        )
                    }
                }
            }
        }

        selectedChannel?.let { channel ->
            StreamPlayerDialog(channel = channel, onDismiss = { selectedChannel = null })
        }
        if (showEpgGuide) {
            EpgGuideDialog(
                channels = allChannels,
                programs = epgPrograms,
                nowUtcMillis = nowUtcMillis,
                onPlay = { channel ->
                    showEpgGuide = false
                    selectedChannel = channel
                },
                onDismiss = { showEpgGuide = false }
            )
        }
    }
}

private data class ChannelEpg(
    val current: com.velnix.tv.domain.model.EpgProgram?,
    val next: com.velnix.tv.domain.model.EpgProgram?
)

private data class PendingReminder(
    val channelName: String,
    val programTitle: String,
    val startUtcMillis: Long,
    val key: String
)

private fun findChannelEpg(
    channel: Channel,
    programs: List<com.velnix.tv.domain.model.EpgProgram>,
    now: Long
): ChannelEpg {
    val id = channel.tvgId?.takeIf { it.isNotBlank() } ?: return ChannelEpg(null, null)
    val matching = programs.filter { it.channelId.equals(id, ignoreCase = true) }
    return ChannelEpg(
        current = matching.firstOrNull { now >= it.startUtcMillis && now < it.endUtcMillis },
        next = matching.filter { it.startUtcMillis > now }.minByOrNull { it.startUtcMillis }
    )
}

@Composable
private fun ChannelCard(channel: Channel, epg: ChannelEpg, favorite: Boolean, onFavorite: () -> Unit, onClick: () -> Unit) {
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
            Text(
                epg.current?.let { "الآن: ${it.title}" } ?: channel.now,
                color = if (epg.current != null) Acid else Muted,
                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            epg.next?.let { next ->
                Text("التالي: ${next.title}", color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            epg.current?.let { current ->
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { current.progressAt(System.currentTimeMillis()) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Acid,
                    trackColor = Color(0xFF34343A)
                )
            }
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Acid)
        }
        IconButton(onClick = onFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (favorite) Acid else Muted)
        }
    }
}


@Composable
private fun EpgGuideDialog(
    channels: List<Channel>,
    programs: List<com.velnix.tv.domain.model.EpgProgram>,
    nowUtcMillis: Long,
    onPlay: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reminderPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var reminderKeys by remember {
        mutableStateOf(reminderPrefs.getStringSet(KEY_REMINDERS, emptySet())?.toSet() ?: emptySet())
    }
    var pendingReminder by remember { mutableStateOf<PendingReminder?>(null) }
    var reminderMessage by remember { mutableStateOf<String?>(null) }

    fun enableReminder(request: PendingReminder) {
        ProgramReminderScheduler.schedule(context, request.channelName, request.programTitle, request.startUtcMillis, request.key)
        reminderKeys = reminderKeys + request.key
        reminderPrefs.edit().putStringSet(KEY_REMINDERS, reminderKeys).apply()
        reminderMessage = "تم ضبط التذكير قبل موعد البرنامج بخمس دقائق."
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingReminder
        if (granted && request != null) enableReminder(request)
        else reminderMessage = "لم يتم تفعيل الإشعارات. فعّل إذن الإشعارات من إعدادات الهاتف."
        pendingReminder = null
    }

    var selectedDayOffset by remember { mutableIntStateOf(0) }
    val dayStart = remember(selectedDayOffset, nowUtcMillis) {
        Calendar.getInstance().apply {
            timeInMillis = nowUtcMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        }.timeInMillis
    }
    val dayEnd = remember(dayStart) {
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
    val dateLabel = remember(dayStart) {
        SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date(dayStart))
    }
    val guideChannels = channels.filter { !it.tvgId.isNullOrBlank() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(10.dp),
            color = Panel,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("دليل البرامج", color = Acid, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("جدول القنوات والبرامج", color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("إغلاق", color = Muted) }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0..6).forEach { offset ->
                        FilterChip(
                            selected = selectedDayOffset == offset,
                            onClick = { selectedDayOffset = offset },
                            label = {
                                Text(
                                    when (offset) {
                                        0 -> "اليوم"
                                        1 -> "غداً"
                                        else -> "+$offset"
                                    },
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Acid,
                                selectedLabelColor = Ink,
                                labelColor = Color.White
                            )
                        )
                    }
                }
                Text(dateLabel, color = Muted, fontSize = 11.sp)
                reminderMessage?.let { Text(it, color = Acid, fontSize = 10.sp) }
                Spacer(Modifier.height(8.dp))
                if (programs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("استورد ملف XMLTV حتى يظهر جدول البرامج.", color = Muted)
                    }
                } else if (guideChannels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ماكو قنوات مرتبطة بمعرّف tvg-id. تأكد أن ملف M3U يحتوي معرّفات القنوات.", color = Muted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(guideChannels, key = { it.number + it.name }) { channel ->
                            val channelPrograms = programs
                                .filter {
                                    it.channelId.equals(channel.tvgId, ignoreCase = true) &&
                                        it.startUtcMillis < dayEnd && it.endUtcMillis > dayStart
                                }
                                .sortedBy { it.startUtcMillis }
                            if (channelPrograms.isNotEmpty()) {
                                Column(
                                    Modifier.fillMaxWidth()
                                        .background(Color(0xFF222228), RoundedCornerShape(15.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("${channelPrograms.size} برامج", color = Muted, fontSize = 10.sp)
                                        TextButton(onClick = { onPlay(channel) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Acid)
                                            Text("مشاهدة", color = Acid, fontSize = 10.sp)
                                        }
                                    }
                                    channelPrograms.forEach { program ->
                                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        val isCurrent = nowUtcMillis >= program.startUtcMillis && nowUtcMillis < program.endUtcMillis
                                        Column(
                                            Modifier.fillMaxWidth()
                                                .background(Panel, RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "${timeFormat.format(Date(program.startUtcMillis))} – ${timeFormat.format(Date(program.endUtcMillis))}",
                                                    color = if (isCurrent) Acid else Muted,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (isCurrent) {
                                                    Text("الآن", color = Acid, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                val reminderKey = "${channel.tvgId}|${program.startUtcMillis}"
                                                val hasReminder = reminderKey in reminderKeys
                                                TextButton(
                                                    enabled = hasReminder || program.startUtcMillis > nowUtcMillis,
                                                    onClick = {
                                                        if (hasReminder) {
                                                            ProgramReminderScheduler.cancel(context, reminderKey)
                                                            reminderKeys = reminderKeys - reminderKey
                                                            reminderPrefs.edit().putStringSet(KEY_REMINDERS, reminderKeys).apply()
                                                            reminderMessage = "تم إلغاء التذكير."
                                                        } else {
                                                            val request = PendingReminder(channel.name, program.title, program.startUtcMillis, reminderKey)
                                                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                                                androidx.core.content.ContextCompat.checkSelfPermission(
                                                                    context, android.Manifest.permission.POST_NOTIFICATIONS
                                                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                                            ) {
                                                                pendingReminder = request
                                                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                            } else {
                                                                enableReminder(request)
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text(if (hasReminder) "إلغاء التذكير" else "ذكّرني", color = if (hasReminder) Muted else Acid, fontSize = 9.sp)
                                                }
                                            }
                                            Text(program.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            program.description?.takeIf { it.isNotBlank() }?.let {
                                                Text(it, color = Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamPlayerDialog(channel: Channel, onDismiss: () -> Unit) {
    var streamUrl by remember(channel.number) { mutableStateOf(channel.streamUrl.orEmpty()) }
    var activeUrl by remember(channel.number) { mutableStateOf(channel.streamUrl) }
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
                    Media3Player(url = activeUrl!!, onError = { error = it })
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
private fun Media3Player(url: String, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasPlayingBeforePause by remember(url) { mutableStateOf(true) }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            val lower = url.lowercase()
            val mime = when {
                lower.substringBefore('?').endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
                lower.substringBefore('?').endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
                else -> null
            }
            val item = MediaItem.Builder().setUri(Uri.parse(url)).apply {
                if (mime != null) setMimeType(mime)
            }.build()
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError("تعذر تشغيل البث: ${error.errorCodeName}")
                }
            })
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPlayingBeforePause = player.isPlaying || player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> if (wasPlayingBeforePause) player.play()
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
