package com.velnix.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0B0B0D)
private val Panel = Color(0xFF17171B)
private val Acid = Color(0xFFF4FF00)
private val Muted = Color(0xFF9A9AA3)

data class Channel(val name: String, val category: String, val now: String, val number: String)

private val demoChannels = listOf(
    Channel("VELNIX SPORTS 1", "رياضة", "بث تجريبي · لا يوجد مصدر مربوط", "01"),
    Channel("VELNIX SPORTS 2", "رياضة", "قريباً", "02"),
    Channel("WORLD NEWS", "أخبار", "قريباً", "03"),
    Channel("CINEMA PLUS", "عام", "قريباً", "04"),
    Channel("DOCUMENTARY", "عام", "قريباً", "05")
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
                Text("كل قنواتك في مكان واحد", color = Muted, fontSize = 13.sp)
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
                Text("القنوات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered) { channel ->
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
        AlertDialog(
            onDismissRequest = { selectedChannel = null },
            containerColor = Panel,
            title = { Text(channel.name, color = Acid) },
            text = { Text("هذه بطاقة قناة تجريبية. ربط مصدر البث والمشغل الفعلي يأتي في مرحلة تنفيذ المشغل.") },
            confirmButton = { TextButton(onClick = { selectedChannel = null }) { Text("إغلاق", color = Acid) } }
        )
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
            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(channel.now, color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (favorite) Acid else Muted)
        }
    }
}
