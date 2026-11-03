name: Build APK

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Generar proyecto
        run: |
          rm -rf app settings.gradle.kts build.gradle.kts gradle.properties
          mkdir -p app/src/main/java/com/example/gameboost
          cat > settings.gradle.kts << 'EOF'
          pluginManagement {
              repositories {
                  google()
                  mavenCentral()
                  gradlePluginPortal()
              }
          }
          dependencyResolutionManagement {
              repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
              repositories {
                  google()
                  mavenCentral()
              }
          }
          rootProject.name = "GameBoost"
          include(":app")
          EOF
          cat > build.gradle.kts << 'EOF'
          plugins {
              id("com.android.application") version "8.5.2" apply false
              id("org.jetbrains.kotlin.android") version "2.0.20" apply false
              id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
          }
          EOF
          cat > gradle.properties << 'EOF'
          org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
          android.useAndroidX=true
          kotlin.code.style=official
          EOF
          cat > app/build.gradle.kts << 'EOF'
          plugins {
              id("com.android.application")
              id("org.jetbrains.kotlin.android")
              id("org.jetbrains.kotlin.plugin.compose")
          }

          android {
              namespace = "com.example.gameboost"
              compileSdk = 34

              defaultConfig {
                  applicationId = "com.example.gameboost"
                  minSdk = 29
                  targetSdk = 34
                  versionCode = 1
                  versionName = "1.0"
              }

              compileOptions {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              kotlinOptions { jvmTarget = "17" }
              buildFeatures { compose = true }
          }

          dependencies {
              implementation(platform("androidx.compose:compose-bom:2024.09.00"))
              implementation("androidx.compose.material3:material3")
              implementation("androidx.compose.foundation:foundation")
              implementation("androidx.compose.ui:ui")
              implementation("androidx.activity:activity-compose:1.9.2")
              implementation("androidx.core:core-ktx:1.13.1")
          }
          EOF
          cat > app/src/main/AndroidManifest.xml << 'EOF'
          <?xml version="1.0" encoding="utf-8"?>
          <manifest xmlns:android="http://schemas.android.com/apk/res/android">

              <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
              <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
              <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
              <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
              <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
              <uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
              <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
              <uses-permission android:name="android.permission.WRITE_SETTINGS" />
              <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

              <application
                  android:allowBackup="true"
                  android:label="GameBoost"
                  android:icon="@android:drawable/sym_def_app_icon"
                  android:theme="@android:style/Theme.DeviceDefault.NoActionBar">

                  <activity
                      android:name=".MainActivity"
                      android:exported="true">
                      <intent-filter>
                          <action android:name="android.intent.action.MAIN" />
                          <category android:name="android.intent.category.LAUNCHER" />
                      </intent-filter>
                  </activity>

                  <service
                      android:name=".GameBoostService"
                      android:exported="false"
                      android:foregroundServiceType="specialUse|mediaProjection">
                      <property
                          android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                          android:value="game_overlay" />
                  </service>
              </application>
          </manifest>
          EOF
          cat > app/src/main/java/com/example/gameboost/Cfg.kt << 'EOF'
          package com.example.gameboost

          import android.content.Context
          import java.text.SimpleDateFormat
          import java.util.Date
          import java.util.Locale

          object Cfg {
              private fun p(ctx: Context) = ctx.getSharedPreferences("gb", Context.MODE_PRIVATE)

              fun flag(ctx: Context, pkg: String, key: String, def: Boolean): Boolean =
                  p(ctx).getBoolean("$pkg:$key", def)

              fun setFlag(ctx: Context, pkg: String, key: String, value: Boolean) {
                  p(ctx).edit().putBoolean("$pkg:$key", value).apply()
              }

              private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

              fun addTime(ctx: Context, pkg: String, ms: Long) {
                  val k = "time:$pkg:${today()}"
                  p(ctx).edit().putLong(k, p(ctx).getLong(k, 0L) + ms).apply()
              }

              fun timeToday(ctx: Context, pkg: String): Long =
                  p(ctx).getLong("time:$pkg:${today()}", 0L)
          }
          EOF
          cat > app/src/main/java/com/example/gameboost/MainActivity.kt << 'EOF'
          package com.example.gameboost

          import android.Manifest
          import android.app.Activity
          import android.content.Context
          import android.content.Intent
          import android.media.projection.MediaProjectionManager
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

          fun startBoost(ctx: Context, pkg: String, rc: Int?, data: Intent?) {
              val i = Intent(ctx, GameBoostService::class.java).putExtra("pkg", pkg)
              if (rc != null && data != null) {
                  i.putExtra("rc", rc)
                  i.putExtra("data", data)
              }
              ContextCompat.startForegroundService(ctx, i)
              ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
          }

          @Composable
          fun ProfileSwitch(pkg: String, key: String, label: String, def: Boolean) {
              val ctx = LocalContext.current
              var on by remember { mutableStateOf(Cfg.flag(ctx, pkg, key, def)) }
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Text(label, Modifier.weight(1f))
                  Switch(checked = on, onCheckedChange = { on = it; Cfg.setFlag(ctx, pkg, key, it) })
              }
          }

          @Composable
          fun ProfileDialog(app: AppItem, onClose: () -> Unit) {
              AlertDialog(
                  onDismissRequest = onClose,
                  confirmButton = { TextButton(onClick = onClose) { Text("Listo") } },
                  title = { Text(app.label) },
                  text = {
                      Column {
                          ProfileSwitch(app.pkg, "panel", "Barra lateral flotante", true)
                          ProfileSwitch(app.pkg, "capture", "Captura y grabación", true)
                          ProfileSwitch(app.pkg, "dnd", "No molestar", true)
                          ProfileSwitch(app.pkg, "ram", "Limpiar RAM al abrir", true)
                          ProfileSwitch(app.pkg, "brightness", "Bloquear brillo", false)
                      }
                  }
              )
          }

          @Composable
          fun GameBoostScreen() {
              val ctx = LocalContext.current
              val apps = remember { loadApps(ctx) }
              val prefs = remember { ctx.getSharedPreferences("games", Context.MODE_PRIVATE) }
              var games by remember { mutableStateOf(prefs.getStringSet("set", emptySet())!!.toSet()) }
              var profileApp by remember { mutableStateOf<AppItem?>(null) }
              var pending by remember { mutableStateOf<String?>(null) }

              val notifLauncher = rememberLauncherForActivityResult(
                  ActivityResultContracts.RequestPermission()
              ) {}
              LaunchedEffect(Unit) {
                  if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }

              val projLauncher = rememberLauncherForActivityResult(
                  ActivityResultContracts.StartActivityForResult()
              ) { res ->
                  val pkg = pending
                  pending = null
                  if (pkg != null) {
                      val data = res.data
                      if (res.resultCode == Activity.RESULT_OK && data != null) {
                          startBoost(ctx, pkg, res.resultCode, data)
                      } else {
                          Toast.makeText(ctx, "Sin permiso de captura: inicio sin captura ni grabación", Toast.LENGTH_LONG).show()
                          startBoost(ctx, pkg, null, null)
                      }
                  }
              }

              val play: (String) -> Unit = { pkg ->
                  if (!Settings.canDrawOverlays(ctx)) {
                      Toast.makeText(ctx, "Primero da el permiso de overlay", Toast.LENGTH_LONG).show()
                  } else if (Cfg.flag(ctx, pkg, "capture", true)) {
                      pending = pkg
                      val mpm = ctx.getSystemService(MediaProjectionManager::class.java)
                      projLauncher.launch(mpm.createScreenCaptureIntent())
                  } else {
                      startBoost(ctx, pkg, null, null)
                  }
              }

              Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                  Text("GameBoost", style = MaterialTheme.typography.headlineMedium)
                  Spacer(Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      OutlinedButton(
                          onClick = {
                              ctx.startActivity(
                                  Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                              )
                          },
                          modifier = Modifier.weight(1f),
                          contentPadding = PaddingValues(horizontal = 4.dp)
                      ) { Text("Overlay", maxLines = 1) }
                      OutlinedButton(
                          onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                          modifier = Modifier.weight(1f),
                          contentPadding = PaddingValues(horizontal = 4.dp)
                      ) { Text("No molestar", maxLines = 1) }
                      OutlinedButton(
                          onClick = {
                              ctx.startActivity(
                                  Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                              )
                          },
                          modifier = Modifier.weight(1f),
                          contentPadding = PaddingValues(horizontal = 4.dp)
                      ) { Text("Brillo", maxLines = 1) }
                  }
                  Spacer(Modifier.height(8.dp))
                  Text(
                      "1) Da los 3 permisos.  2) Marca ★ tus juegos.  3) ⚙ configura cada juego.  4) Pulsa Jugar.",
                      style = MaterialTheme.typography.bodySmall
                  )
                  Spacer(Modifier.height(8.dp))

                  LazyColumn {
                      items(apps.sortedByDescending { it.pkg in games }, key = { it.pkg }) { app ->
                          val isGame = app.pkg in games
                          Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                  Text(app.label, Modifier.weight(1f))
                                  TextButton(onClick = {
                                      games = if (isGame) games - app.pkg else games + app.pkg
                                      prefs.edit().putStringSet("set", games).apply()
                                  }) { Text(if (isGame) "★" else "☆") }
                                  if (isGame) {
                                      TextButton(onClick = { profileApp = app }) { Text("⚙") }
                                      Button(onClick = { play(app.pkg) }) { Text("Jugar") }
                                  }
                              }
                              if (isGame) {
                                  val mins = Cfg.timeToday(ctx, app.pkg) / 60000
                                  Text("Hoy: $mins min", style = MaterialTheme.typography.bodySmall)
                              }
                          }
                      }
                  }
                  profileApp?.let { ProfileDialog(it) { profileApp = null } }
              }
          }
          EOF
          cat > app/src/main/java/com/example/gameboost/GameBoostService.kt << 'EOF'
          package com.example.gameboost

          import android.app.*
          import android.content.ContentValues
          import android.content.Intent
          import android.content.IntentFilter
          import android.content.pm.ApplicationInfo
          import android.content.pm.ServiceInfo
          import android.graphics.Bitmap
          import android.graphics.Color
          import android.graphics.PixelFormat
          import android.hardware.display.DisplayManager
          import android.hardware.display.VirtualDisplay
          import android.media.ImageReader
          import android.media.MediaRecorder
          import android.media.projection.MediaProjection
          import android.media.projection.MediaProjectionManager
          import android.os.BatteryManager
          import android.os.Build
          import android.os.Environment
          import android.os.Handler
          import android.os.IBinder
          import android.os.Looper
          import android.os.ParcelFileDescriptor
          import android.os.PowerManager
          import android.os.SystemClock
          import android.provider.MediaStore
          import android.provider.Settings
          import android.util.DisplayMetrics
          import android.view.Display
          import android.view.Gravity
          import android.view.MotionEvent
          import android.view.View
          import android.view.WindowManager
          import android.widget.LinearLayout
          import android.widget.TextView
          import android.widget.Toast
          import androidx.core.app.ServiceCompat
          import kotlin.math.abs

          class GameBoostService : Service() {

              companion object {
                  const val MAIN_CH = "gameboost"
                  const val ALERT_CH = "gameboost_alert"
              }

              private lateinit var wm: WindowManager
              private lateinit var am: ActivityManager
              private lateinit var nm: NotificationManager
              private val handler = Handler(Looper.getMainLooper())

              private var root: LinearLayout? = null
              private var header: TextView? = null
              private var panel: LinearLayout? = null
              private var recBtn: TextView? = null
              private var brightBtn: TextView? = null
              private var dndBtn: TextView? = null
              private var expanded = false

              private var currentPkg: String? = null
              private var startedAt = SystemClock.elapsedRealtime()
              private var previousFilter = NotificationManager.INTERRUPTION_FILTER_ALL
              private var dndChanged = false
              private var brightLocked = false
              private var prevBrightMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
              private var lastAlertAt = 0L

              private var projection: MediaProjection? = null
              private var vDisplay: VirtualDisplay? = null
              private var reader: ImageReader? = null
              private var recorder: MediaRecorder? = null
              private var recPfd: ParcelFileDescriptor? = null
              private var capW = 0
              private var capH = 0

              private val ticker = object : Runnable {
                  override fun run() {
                      refreshHeader()
                      checkAlerts()
                      handler.postDelayed(this, 1000)
                  }
              }

              override fun onBind(intent: Intent?): IBinder? = null

              override fun onCreate() {
                  super.onCreate()
                  wm = getSystemService(WindowManager::class.java)
                  am = getSystemService(ActivityManager::class.java)
                  nm = getSystemService(NotificationManager::class.java)
                  nm.createNotificationChannel(
                      NotificationChannel(MAIN_CH, "GameBoost", NotificationManager.IMPORTANCE
