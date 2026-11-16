package com.pulsehud

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat

private val Bg = Color(0xFF090D17)
private val Panel = Color(0xFF151D2D)
private val Cyan = Color(0xFF42E8D5)

class MainActivity : ComponentActivity() {
    private val fontPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                getSharedPreferences("pulsehud", MODE_PRIVATE).edit().putString("font_uri", uri.toString()).apply()
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MonitorState.restore(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Cyan, background = Bg, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) {
                PulseDashboard(
                    onStart = { startOverlay() },
                    onStop = { sendOverlay(OverlayService.ACTION_STOP) },
                    onEdit = { sendOverlay(OverlayService.ACTION_EDIT) },
                    onLive = { sendOverlay(OverlayService.ACTION_LIVE) },
                    onAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onFont = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) }
                )
            }
        }
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val i = Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_START)
        ContextCompat.startForegroundService(this, i)
    }

    private fun sendOverlay(action: String) {
        val i = Intent(this, OverlayService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= 26) startService(i) else startService(i)
    }
}

@Composable
private fun PulseDashboard(
    onStart: () -> Unit, onStop: () -> Unit, onEdit: () -> Unit, onLive: () -> Unit,
    onAccessibility: () -> Unit, onFont: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { HudPrefs.prefs(context) }
    var showCps by remember { mutableStateOf(prefs.getBoolean("show_cps", true)) }
    var showFps by remember { mutableStateOf(prefs.getBoolean("show_fps", true)) }
    var showBattery by remember { mutableStateOf(prefs.getBoolean("show_battery", true)) }
    var history by remember { mutableStateOf(prefs.getBoolean("history", true)) }
    var interval by remember { mutableFloatStateOf(prefs.getInt("interval_ms", 1000).toFloat()) }
    var fontSize by remember { mutableFloatStateOf(prefs.getInt("font_size", 14).toFloat()) }
    var opacity by remember { mutableFloatStateOf(prefs.getInt("opacity", 75).toFloat()) }
    var alignment by remember { mutableStateOf(prefs.getString("alignment", "left") ?: "left") }
    var colorName by remember { mutableStateOf(prefs.getString("text_color", "#FFFFFF") ?: "#FFFFFF") }
    var layout by remember { mutableStateOf(prefs.getString("layout", "horizontal") ?: "horizontal") }
    var preset by remember { mutableStateOf("Gaming") }
    var customName by remember { mutableStateOf("") }
    var customPresets by remember { mutableStateOf(prefs.getStringSet("custom_presets", emptySet())?.toList() ?: emptyList()) }

    fun save(key: String, value: Any) = HudPrefs.put(context, key, value)

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("PULSEHUD", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("LIVE PERFORMANCE CONTROL", color = Color(0xFF8493AA), fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Surface(color = Color(0xFF183A3A), shape = RoundedCornerShape(50)) {
                    Text("● READY", color = Cyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LIVE MONITOR", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("TOTAL CLICKS", MonitorState.totalClicks.toString(), Modifier.weight(1f))
                        Metric("PEAK CPS", MonitorState.peakCps.toString(), Modifier.weight(1f))
                    }
                    Text("Current CPS is calculated over your selected rolling interval.", color = Color.LightGray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("START OVERLAY") }
                        OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text("STOP") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("EDIT POSITION") }
                        OutlinedButton(onClick = onLive, modifier = Modifier.weight(1f)) { Text("TOUCH-THROUGH") }
                    }
                    OutlinedButton(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.TouchApp, null); Spacer(Modifier.width(6.dp)); Text("ENABLE CLICK ACCESSIBILITY")
                    }
                    Text("Accessibility tracking is best-effort. Games may not expose touch events.", color = Color(0xFFFFC66D), fontSize = 11.sp)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("WIDGET CONTROL", color = Cyan, fontWeight = FontWeight.Bold)
                    ToggleRow("CPS counters", showCps) { showCps = it; save("show_cps", it) }
                    ToggleRow("FPS", showFps) { showFps = it; save("show_fps", it) }
                    ToggleRow("Battery level + temperature", showBattery) { showBattery = it; save("show_battery", it) }
                    Text("CPS measurement interval · ${interval.toInt()} ms", color = Color.LightGray, fontSize = 12.sp)
                    Slider(value = interval, onValueChange = { interval = it }, valueRange = 250f..5000f, onValueChangeFinished = { save("interval_ms", interval.toInt()) })
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("APPEARANCE STUDIO", color = Cyan, fontWeight = FontWeight.Bold)
                    Text("Preset base", color = Color.LightGray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Minimal", "Gaming", "Neon", "Glass", "Terminal").forEach { name ->
                            FilterChip(selected = preset == name, onClick = {
                                preset = name
                                val c = when(name) {
                                    "Neon" -> "#42E8D5"
                                    "Terminal" -> "#72FF78"
                                    "Gaming" -> "#FFFFFF"
                                    "Glass" -> "#D9E8FF"
                                    else -> "#E5EAF2"
                                }
                                colorName = c; save("text_color", c)
                                save("preset_base", name)
                            }, label = { Text(name, fontSize = 10.sp) })
                        }
                    }
                    Text("Layout", color = Color.LightGray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("horizontal", "vertical", "widgets").forEach { name ->
                            FilterChip(selected = layout == name, onClick = { layout = name; save("layout", name) }, label = { Text(name) })
                        }
                    }
                    Text("Font size · ${fontSize.toInt()} sp", color = Color.LightGray, fontSize = 12.sp)
                    Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 9f..30f, onValueChangeFinished = { save("font_size", fontSize.toInt()) })
                    Text("Background opacity · ${opacity.toInt()}%", color = Color.LightGray, fontSize = 12.sp)
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0f..100f, onValueChangeFinished = { save("opacity", opacity.toInt()) })
                    Text("Text alignment", color = Color.LightGray, fontSize = 12.sp)
                    SingleChoiceSegmentedButtonRow {
                        listOf("left", "center", "right").forEachIndexed { index, item ->
                            SegmentedButton(selected = alignment == item, onClick = { alignment = item; save("alignment", item) }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(item) }
                        }
                    }
                    Text("Text color", color = Color.LightGray, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FFFFFF", "#42E8D5", "#72FF78", "#FFCB6B", "#FF6B9D").forEach { c ->
                            Button(onClick = { colorName = c; save("text_color", c) }, colors = ButtonDefaults.buttonColors(containerColor = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { Color.White }), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(38.dp)) {}
                        }
                    }
                    OutlinedButton(onClick = onFont, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FontDownload, null); Spacer(Modifier.width(6.dp)); Text("IMPORT TTF / OTF FONT")
                    }
                    Text("Position: use EDIT POSITION on the live overlay, then drag it. Saved automatically.", color = Color.LightGray, fontSize = 11.sp)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PRESET WORKSHOP", color = Cyan, fontWeight = FontWeight.Bold)
                    Text("Choose a built-in preset above as your base, then save your customized settings here.", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("New preset name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (customName.isNotBlank()) {
                            val updated = (customPresets + "${customName.trim()} · based on $preset").distinct()
                            customPresets = updated
                            save("custom_presets", updated.toSet())
                            customName = ""
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("SAVE CURRENT AS PRESET") }
                    customPresets.forEach { Text("◆ $it", color = Color(0xFFB8C8DA), fontSize = 12.sp) }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SESSION & PRIVACY", color = Cyan, fontWeight = FontWeight.Bold)
                    ToggleRow("Save local session history (storage groundwork)", history) { history = it; save("history", it) }
                    OutlinedButton(onClick = { MonitorState.resetSession(context) }, modifier = Modifier.fillMaxWidth()) { Text("RESET CLICK COUNTERS") }
                    Text("No account or cloud upload is used by this starter project.", color = Color.LightGray, fontSize = 11.sp)
                }
            }
            Text("PulseHUD 0.1.0 · Android 10+ · Prototype build", color = Color(0xFF65748A), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun Metric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFF0D1421), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF8394AC), fontSize = 9.sp, letterSpacing = 1.sp)
            Text(value, color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
