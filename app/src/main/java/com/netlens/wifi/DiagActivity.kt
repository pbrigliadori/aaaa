package com.netlens.wifi

import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Roda SOMENTE no perfil de trabalho (profile owner). É o "cofre". */
class DiagActivity : ComponentActivity() {
    private val dpm by lazy { getSystemService(DevicePolicyManager::class.java) }
    private val admin by lazy { ComponentName(this, NetAdmin::class.java) }
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { lockAll(); finishAndRemoveTask() }
    }

    private fun userApps(): List<ApplicationInfo> =
        packageManager.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES).filter {
            it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.flags and ApplicationInfo.FLAG_INSTALLED != 0 && it.packageName != packageName
        }
    private fun lockAll() = userApps().forEach { dpm.setApplicationHidden(admin, it.packageName, true) }
    private fun unlockAll() = userApps().forEach { dpm.setApplicationHidden(admin, it.packageName, false) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
        enableEdgeToEdge(SystemBarStyle.dark(0), SystemBarStyle.dark(0))
        setContent {
            AppTheme {
                Box(Modifier.fillMaxSize().background(Bg)) {
                    var ok by remember { mutableStateOf(false) }
                    if (!ok) PinScreen { if (it) { unlockAll(); ok = true } else finishAndRemoveTask() }
                    else {
                        var apps by remember { mutableStateOf(userApps()) }
                        var pkg by remember { mutableStateOf("") }
                        LazyColumn(Modifier.statusBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
                            item { LargeTitle("Dados") }
                            item { Group("Trazer app do perfil pessoal") {
                                TextField(pkg, { pkg = it.trim() }, Modifier.fillMaxWidth(), placeholder = { Text("com.exemplo.app", color = Sub) }, singleLine = true,
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Card, unfocusedContainerColor = Card,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)) } }
                            item { PrimaryButton("Trazer") { runCatching { dpm.installExistingPackage(admin, pkg) }; apps = userApps() } }
                            item { Group("Apps") {
                                if (apps.isEmpty()) Text("Nenhum app ainda", Modifier.padding(16.dp), color = Sub)
                                apps.forEachIndexed { i, a -> Column(Modifier.clickable { packageManager.getLaunchIntentForPackage(a.packageName)?.let(::startActivity) }) {
                                    InfoRow(packageManager.getApplicationLabel(a).toString(), "Abrir", i < apps.size - 1) } } } }
                            item { Spacer(Modifier.height(12.dp)); Button({ lockAll(); finishAndRemoveTask() }, Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Bloquear tudo", fontSize = 17.sp) } }
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() { unregisterReceiver(screenOff); super.onDestroy() }
}
