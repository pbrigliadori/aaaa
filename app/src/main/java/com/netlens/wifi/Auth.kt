package com.netlens.wifi

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN com PBKDF2 + salt e bloqueio progressivo. Cada instância (pessoal / trabalho) tem o seu. */
object Auth {
    private fun p(c: Context) = c.getSharedPreferences("nl_cfg", 0)
    private fun hash(pin: String, salt: ByteArray) =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 210_000, 256)).encoded
    fun has(c: Context) = p(c).contains("h")
    fun set(c: Context, pin: String) {
        val s = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p(c).edit().putString("s", Base64.encodeToString(s, 0)).putString("h", Base64.encodeToString(hash(pin, s), 0))
            .putInt("f", 0).putLong("u", 0).apply()
    }
    fun wait(c: Context) = maxOf(0L, p(c).getLong("u", 0) - System.currentTimeMillis())
    fun check(c: Context, pin: String): Boolean {
        if (wait(c) > 0) return false
        val sp = p(c)
        val ok = MessageDigest.isEqual(hash(pin, Base64.decode(sp.getString("s", ""), 0)), Base64.decode(sp.getString("h", ""), 0))
        if (ok) sp.edit().putInt("f", 0).apply() else {
            val f = sp.getInt("f", 0) + 1
            val lock = if (f >= 3) minOf(30_000L shl (f - 3).coerceAtMost(7), 3_600_000L) else 0L
            sp.edit().putInt("f", f).putLong("u", System.currentTimeMillis() + lock).apply()
        }
        return ok
    }
}

/** Erro NUNCA é mostrado: PIN errado simplesmente volta (onResult(false)). */
@Composable
fun PinScreen(onResult: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val w = (ctx as Activity).window
        w.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (ctx is MainActivity) w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    val setup = remember { !Auth.has(ctx) }
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    fun submit() {
        if (setup) {
            if (pin.length < 6) { onResult(false); return }
            if (first == null) { first = pin; pin = "" }
            else if (first == pin) { Auth.set(ctx, pin); onResult(true) } else onResult(false)
        } else onResult(Auth.check(ctx, pin))
    }
    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        Arrangement.Center, Alignment.CenterHorizontally) {
        Text(if (setup) (if (first == null) "Defina um código (6+ dígitos)" else "Confirme o código") else "Digite o código",
            fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.padding(vertical = 28.dp).height(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(maxOf(6, pin.length)) { i -> Box(Modifier.size(14.dp).clip(CircleShape)
                .then(if (i < pin.length) Modifier.background(Color.White) else Modifier.border(1.5.dp, Color.White, CircleShape))) }
        }
        val keys = listOf("1","2","3","4","5","6","7","8","9","OK","0","⌫")
        keys.chunked(3).forEach { row ->
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { k ->
                    Box(Modifier.size(78.dp).clip(CircleShape).background(if (k.length == 1 && k[0].isDigit()) Color(0xFF2C2C2E) else Color.Transparent)
                        .clickable { when (k) { "⌫" -> pin = pin.dropLast(1); "OK" -> submit(); else -> if (pin.length < 12) pin += k } },
                        contentAlignment = Alignment.Center) {
                        Text(k, fontSize = if (k.length == 1 && k[0].isDigit()) 32.sp else 20.sp, color = if (k == "OK") Blue else Color.White)
                    }
                }
            }
        }
    }
}
