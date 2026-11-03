package com.example.gameboost

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class AppItem(val label: String, val pkg: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { GameBoostScreen() }
            }
        }
    }
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun launchGame(ctx: Context, pkg: String) {
    if (!Settings.canDrawOverlays(ctx)) {
        Toast.makeText(ctx, "Primero da el permiso de overlay", Toast.LENGTH_LONG).show()
        return
    }
    ContextCompat.startForegroundService(
        ctx, Intent(ctx, GameBoostService::class.java).putExtra("pkg", pkg)
    )
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
}

@Composable
fun GameBoostScreen() {
    val ctx = LocalContext.current
    val apps = remember { loadApps(ctx) }
    val prefs = remember { ctx.getSharedPreferences("games", Context.MODE_PRIVATE) }
    var games by remember { mutableStateOf(prefs.getStringSet("set", emptySet())!!.toSet()) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
        Text("GameBoost", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                )
            }) { Text("Overlay") }
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }) { Text("No molestar") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Marca ★ tus juegos y pulsa Jugar.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(apps.sortedByDescending { it.pkg in games }, key = { it.pkg }) { app ->
                val isGame = app.pkg in games
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, Modifier.weight(1f))
                    TextButton(onClick = {
                        games = if (isGame) games - app.pkg else games + app.pkg
                        prefs.edit().putStringSet("set", games).apply()
                    }) { Text(if (isGame) "★" else "☆") }
                    if (isGame) Button(onClick = { launchGame(ctx, app.pkg) }) { Text("Jugar") }
                }
            }
        }
    }
}
