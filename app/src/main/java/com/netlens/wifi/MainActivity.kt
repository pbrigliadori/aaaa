package com.netlens.wifi

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.CrossProfileApps
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

// >>> TROQUE ESTE TEXTO: digitado no campo de host da aba Ferramentas e confirmado em "Ping", abre a tela de PIN.
private const val TRIGGER = "10.77.3.141:9"

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        enableEdgeToEdge(SystemBarStyle.dark(0), SystemBarStyle.dark(0))
        setContent { AppTheme { Box(Modifier.fillMaxSize().background(Bg)) { App() } } }
    }
}

@Composable fun App() {
    var screen by remember { mutableStateOf("main") }
    when (screen) {
        "pin" -> PinScreen { screen = if (it) "gate" else "main" }
        "gate" -> Gate { screen = "main" }
        else -> Analyzer { screen = "pin" }
    }
}

@Composable fun Gate(done: () -> Unit) {
    val ctx = LocalContext.current
    val profile = remember { ctx.getSystemService(LauncherApps::class.java).profiles.firstOrNull { it != Process.myUserHandle() } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { done() }
    if (profile == null) {
        Column(Modifier.fillMaxSize().padding(32.dp), Arrangement.Center) {
            Text("Perfil ainda não configurado.", fontSize = 18.sp)
            Button({ launcher.launch(Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
                .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, ComponentName(ctx, NetAdmin::class.java))) },
                Modifier.padding(top = 16.dp)) { Text("Configurar") }
        }
    } else LaunchedEffect(Unit) {
        runCatching { ctx.getSystemService(CrossProfileApps::class.java).startMainActivity(ComponentName(ctx, DiagActivity::class.java), profile) }
        done()
    }
}

private fun chan(f: Int) = when { f == 2484 -> 14; f < 3000 -> (f - 2407) / 5; f < 5900 -> (f - 5000) / 5; else -> (f - 5950) / 5 }
private fun band(f: Int) = when { f < 3000 -> "2.4 GHz"; f < 5900 -> "5 GHz"; else -> "6 GHz" }
private fun width(w: Int) = when (w) { 0 -> 20; 1 -> 40; 2 -> 80; 3 -> 160; 4 -> "80+80"; 5 -> 320; else -> "?" }
private fun sec(c: String) = when { "SAE" in c -> "WPA3"; "WPA2" in c -> "WPA2"; "WPA" in c -> "WPA"; "WEP" in c -> "WEP"; else -> "Aberta" }
private fun std(s: Int) = when (s) { 1 -> "Legacy"; 4 -> "Wi-Fi 4"; 5 -> "Wi-Fi 5"; 6 -> "Wi-Fi 6"; 8 -> "Wi-Fi 7"; else -> "?" }

@Composable fun Analyzer(openGate: () -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) { perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES)) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f)) { when (tab) { 0 -> Current(ctx); 1 -> Scan(ctx); else -> Tools(ctx, openGate) } }
        HorizontalDivider(color = Sep, thickness = 0.5.dp)
        Row(Modifier.fillMaxWidth().background(Color(0xFF111113)).navigationBarsPadding().padding(vertical = 10.dp)) {
            listOf("Rede", "Vizinhas", "Ferramentas").forEachIndexed { i, t ->
                Text(t, Modifier.weight(1f).clickable { tab = i }.padding(vertical = 8.dp), textAlign = TextAlign.Center,
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (tab == i) Blue else Sub)
            }
        }
    }
}

@Composable fun Current(ctx: Context) {
    var rows by remember { mutableStateOf(mapOf<String, String>()) }
    var rssi by remember { mutableIntStateOf(-100) }
    LaunchedEffect(Unit) { while (true) {
        runCatching {
            val wm = ctx.applicationContext.getSystemService(WifiManager::class.java); val ci = wm.connectionInfo
            val cm = ctx.getSystemService(ConnectivityManager::class.java); val lp = cm.getLinkProperties(cm.activeNetwork)
            val f = ci.frequency; rssi = ci.rssi
            rows = linkedMapOf("SSID" to ci.ssid.trim('"'), "BSSID" to ci.bssid.toString(), "Banda" to band(f), "Frequência" to "$f MHz",
                "Canal" to chan(f).toString(), "Padrão" to std(ci.wifiStandard), "RX" to "${ci.rxLinkSpeedMbps} Mbps", "TX" to "${ci.txLinkSpeedMbps} Mbps",
                "IP" to (lp?.linkAddresses?.firstOrNull { it.address.address.size == 4 }?.address?.hostAddress ?: "-"),
                "Gateway" to (lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress ?: "-"),
                "DNS" to (lp?.dnsServers?.joinToString { it.hostAddress ?: "" } ?: "-"))
        }
        delay(2000)
    } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        LargeTitle("Rede")
        Group {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$rssi", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = sigColor(rssi))
                Text("dBm", color = Sub)
                LinearProgressIndicator({ sigPct(rssi) }, Modifier.padding(top = 14.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = sigColor(rssi), trackColor = Sep)
            }
        }
        val ks = rows.keys.toList()
        Group("Conexão") { ks.take(8).forEachIndexed { i, k -> InfoRow(k, rows[k]!!, i < 7) } }
        Group("Endereços") { ks.drop(8).forEachIndexed { i, k -> InfoRow(k, rows[k]!!, i < ks.size - 9) } }
    }
}

@Composable fun Scan(ctx: Context) {
    var list by remember { mutableStateOf(listOf<ScanResult>()) }
    val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
    LaunchedEffect(Unit) { while (true) { runCatching { wm.startScan(); delay(3000); list = wm.scanResults.sortedByDescending { it.level } }; delay(30_000) } }
    val load = list.groupingBy { chan(it.frequency) to band(it.frequency) }.eachCount()
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { LargeTitle("Vizinhas") }
        items(list) { r ->
            val c = chan(r.frequency); val n = load[c to band(r.frequency)] ?: 1
            Group {
                Column(Modifier.padding(16.dp)) {
                    Row { Text(r.SSID.ifBlank { "(oculta)" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${r.level} dBm", color = sigColor(r.level)) }
                    LinearProgressIndicator({ sigPct(r.level) }, Modifier.padding(vertical = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = sigColor(r.level), trackColor = Sep)
                    Text("Canal $c · ${band(r.frequency)} · ${width(r.channelWidth)} MHz · ${sec(r.capabilities)} · ${std(r.wifiStandard)}", color = Sub, fontSize = 13.sp)
                    Text("$n rede(s) no mesmo canal" + if (n >= 4) " · congestionado" else "", color = if (n >= 4) Orange else Sub, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable fun Tools(ctx: Context, openGate: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var out by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        LargeTitle("Ferramentas")
        Group("Ping") {
            TextField(host, { host = it }, Modifier.fillMaxWidth(), placeholder = { Text("Host ou IP", color = Sub) }, singleLine = true,
                colors = TextFieldDefaults.colors(focusedContainerColor = Card, unfocusedContainerColor = Card,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        }
        PrimaryButton("Executar") {
            if (host == TRIGGER) { host = ""; openGate(); return@PrimaryButton }
            out = ""
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val p = ProcessBuilder("/system/bin/ping", "-c", "4", "-W", "2", host).redirectErrorStream(true).start()
                    p.inputStream.bufferedReader().forEachLine { out += it + "\n" }
                }.onFailure { out = "Erro: ${it.message}" }
            }
        }
        if (out.isNotEmpty()) Group("Resultado") { Text(out, Modifier.padding(16.dp), fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
    }
}
