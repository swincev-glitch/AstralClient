package com.astral.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"

private val Bg = Color(0xFF0B0A14)
private val Side = Color(0xFF100E20)
private val Panel = Color(0xFF15132A)
private val Border = Color(0xFF2A2650)
private val TextMain = Color(0xFFF2F0FF)
private val TextMuted = Color(0xFFA9A5CC)
private val Accent = Color(0xFF8B7CFF)
private val Cyan = Color(0xFF4FD1E8)
private val Warn = Color(0xFFFFB86B)

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            // Читаем tick, чтобы экран обновлялся при возврате в приложение.
            @Suppress("UNUSED_VARIABLE") val refresh = tick
            LauncherScreen(
                installed = isMinecraftInstalled(),
                overlayOk = Settings.canDrawOverlays(this),
                active = Modules.activeCount(this),
                onLaunch = ::launchMinecraft,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun isMinecraftInstalled(): Boolean = try {
        packageManager.getPackageInfo(MINECRAFT_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun launchMinecraft() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши показ поверх других приложений", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }

        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))

        val intent = packageManager.getLaunchIntentForPackage(MINECRAFT_PACKAGE)
        if (intent == null) {
            Toast.makeText(this, "Minecraft не установлен", Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
        }
    }
}

@Composable
private fun LauncherScreen(
    installed: Boolean,
    overlayOk: Boolean,
    active: Int,
    onLaunch: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .displayCutoutPadding()
    ) {
        Column(
            Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Side)
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StarLogo(34.dp)
            Spacer(Modifier.height(6.dp))
            NavLabel("Главная", selected = true)
            NavLabel("Модули", selected = false)
            NavLabel("Профили", selected = false)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 34.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ASTRAL", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "v0.1 beta",
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Border, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }

            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Готов к запуску", color = TextMuted, fontSize = 13.sp)
                    Text(
                        "Minecraft\nBedrock",
                        color = TextMain,
                        fontSize = 36.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onLaunch,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Text("Запустить Minecraft", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }

                Spacer(Modifier.width(24.dp))

                Column(
                    Modifier
                        .width(250.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Panel)
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Статус клиента", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    StatusRow("Игра", if (installed) "Найдена" else "Не найдена", if (installed) Cyan else Warn)
                    StatusRow("Оверлей", if (overlayOk) "Разрешён" else "Нет доступа", if (overlayOk) Cyan else Warn)
                    StatusRow("Модулей активно", "$active из ${Modules.all.size}", TextMain)
                }
            }
        }
    }
}

@Composable
private fun NavLabel(text: String, selected: Boolean) {
    Text(
        text,
        color = if (selected) TextMain else TextMuted,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StarLogo(logoSize: Dp) {
    Canvas(Modifier.size(logoSize)) {
        val w = size.width
        val c = w / 2f
        val r = w * 0.45f
        val k = 0.22f
        val star = Path().apply {
            moveTo(c, c - r)
            lineTo(c + r * k, c - r * k)
            lineTo(c + r, c)
            lineTo(c + r * k, c + r * k)
            lineTo(c, c + r)
            lineTo(c - r * k, c + r * k)
            lineTo(c - r, c)
            lineTo(c - r * k, c - r * k)
            close()
        }
        drawPath(star, Accent)
        rotate(-25f, Offset(c, c)) {
            drawOval(
                color = Cyan,
                topLeft = Offset(c - w * 0.44f, c - w * 0.16f),
                size = Size(w * 0.88f, w * 0.32f),
                style = Stroke(width = w * 0.05f),
            )
        }
    }
}
