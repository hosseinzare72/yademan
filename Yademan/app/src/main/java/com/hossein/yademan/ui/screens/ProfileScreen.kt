// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/ProfileScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.services.SyncManager
import com.hossein.yademan.services.SyncResult
import com.hossein.yademan.services.SyncWorker

data class ProfileState(
    val name: String = "",
    val username: String? = null,
    val lastSync: String? = null,
    val lastError: String? = null
)

class ProfileViewModel : ViewModel() {
    private val meta = ServiceLocator.meta
    private val auth = ServiceLocator.auth
    private val authVersion = MutableStateFlow(0)

    var syncing by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    val state: StateFlow<ProfileState> = combine(
        meta.observe(MetaKeys.DISPLAY_NAME),
        meta.observe(MetaKeys.LAST_SYNC_LOCAL),
        meta.observe(MetaKeys.LAST_SYNC_ERROR),
        authVersion,
        TimeTicker.now
    ) { name, last, err, _, _ ->
        ProfileState(
            name = name?.takeIf { it.isNotBlank() } ?: auth.username ?: "دوست من",
            username = auth.username?.takeIf { auth.isLoggedIn() } ?: if (auth.isLoggedIn()) "" else null,
            lastSync = DateUtils.isoToMillis(last)?.let { DateUtils.relativeFa(it) },
            lastError = err
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState())

    fun refreshAuth() {
        authVersion.value = authVersion.value + 1
    }

    fun setName(name: String) {
        viewModelScope.launch { meta.set(MetaKeys.DISPLAY_NAME, name.trim()) }
    }

    fun syncNow() {
        if (syncing) return
        syncing = true
        viewModelScope.launch {
            val result = SyncManager.sync()
            message = when (result) {
                is SyncResult.Success -> "همگام‌سازی انجام شد ✓"
                is SyncResult.NotLoggedIn -> "اول وارد حساب شو"
                is SyncResult.NetworkError -> "اینترنت در دسترس نیست؛ بعد از اتصال خودکار سینک می‌شود"
                is SyncResult.ServerError -> result.message
            }
            // اگر آفلاین بودیم، Worker با شرط شبکه منتظر می‌ماند و بعد از اتصال سینک می‌کند
            if (result is SyncResult.NetworkError) SyncWorker.requestSoon(ServiceLocator.app, 5)
            syncing = false
        }
    }

    fun logout() {
        auth.logout()
        viewModelScope.launch {
            meta.delete(MetaKeys.LAST_SYNC)
            meta.delete(MetaKeys.LAST_SYNC_LOCAL)
            meta.delete(MetaKeys.LAST_SYNC_ERROR)
            refreshAuth()
            message = "از حساب خارج شدی. داده‌ها روی گوشی باقی می‌مانند."
        }
    }
}

/** وضعیت مجوزهای حیاتی یادآور */
private data class Health(val notifications: Boolean, val exact: Boolean, val battery: Boolean)

private fun readHealth(context: Context): Health {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return Health(
        notifications = NotificationHelper.areEnabled(context),
        exact = AlarmScheduler.canScheduleExact(context),
        battery = pm.isIgnoringBatteryOptimizations(context.packageName)
    )
}

@Composable
fun ProfileScreen(onLogin: () -> Unit, onOpenNotifications: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var health by remember { mutableStateOf(readHealth(context)) }
    var editName by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        health = readHealth(context)
        vm.refreshAuth()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        health = readHealth(context)
    }

    fun start(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun fixNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        } else {
            start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }

    fun fixExact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            start(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }

    @Suppress("BatteryLife")
    fun fixBattery() {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        val ok = runCatching { context.startActivity(direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        if (!ok) start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item { ScreenHeader(title = "من", subtitle = "حساب کاربری و تنظیمات") }
        item {
            AppCard(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth(),
                brush = YBrush.HeroCard,
                onClick = { editName = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(YBrush.Gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.name.take(1), style = YText.Title, color = YColors.OnGold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val user = state.username
                        Text(text = state.name, style = YText.CardTitle)
                        Text(
                            text = when {
                                user == null -> "وارد نشده • داده‌ها فقط روی این گوشی"
                                user.isBlank() -> "وارد شده"
                                else -> "@$user"
                            },
                            style = YText.Sub
                        )
                    }
                    Icon(imageVector = YIcons.Edit, contentDescription = "ویرایش نام", tint = YColors.TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }

        item { SectionTitle(title = "همگام‌سازی") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.username == null) {
                    InfoRow(
                        icon = YIcons.Sync,
                        color = YColors.Info,
                        title = "سینک بین دستگاه‌ها",
                        subtitle = "با حساب kafezare.ir وارد شو تا یادآورها پشتیبان‌گیری شوند"
                    )
                    GoldButton(text = "ورود / ثبت‌نام", onClick = onLogin)
                } else {
                    InfoRow(
                        icon = YIcons.Sync,
                        color = if (state.lastError != null) YColors.Warning else YColors.Success,
                        title = if (vm.syncing) "در حال همگام‌سازی…" else "آخرین همگام‌سازی: ${state.lastSync ?: "هنوز انجام نشده"}",
                        subtitle = state.lastError ?: "سینک خودکار هر ۱۵ دقیقه و بعد از هر تغییر"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton(
                            text = "همگام‌سازی الان",
                            onClick = vm::syncNow,
                            enabled = !vm.syncing,
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(text = "خروج", onClick = { confirmLogout = true }, modifier = Modifier.height(48.dp))
                    }
                }
                vm.message?.let { Text(text = it, style = YText.Meta, color = YColors.Gold2) }
            }
        }

        item { SectionTitle(title = "سلامت یادآورها") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HealthRow(
                    icon = YIcons.Bell,
                    title = "اجازه نمایش اعلان",
                    ok = health.notifications,
                    okText = "فعال",
                    fixText = "فعال‌سازی",
                    onFix = { fixNotifications() }
                )
                HealthRow(
                    icon = YIcons.Clock,
                    title = "آلارم دقیق",
                    ok = health.exact,
                    okText = "مجاز",
                    fixText = "اجازه بده",
                    onFix = { fixExact() }
                )
                HealthRow(
                    icon = YIcons.Battery,
                    title = "بهینه‌سازی باتری",
                    subtitle = "برای شیائومی، سامسونگ و هواوی ضروری است",
                    ok = health.battery,
                    okText = "معاف",
                    fixText = "معاف کن",
                    onFix = { fixBattery() }
                )
                GhostButton(
                    text = "ارسال اعلان آزمایشی (۱۰ ثانیه دیگر)",
                    onClick = {
                        AlarmScheduler.scheduleTest(context, 10)
                        vm.message = "۱۰ ثانیه دیگر اعلان می‌آید؛ می‌توانی اپ را کامل ببندی."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )
                QuickLinkRow(icon = YIcons.Bell, title = "اعلان‌ها و تنظیمات یادآوری", onClick = onOpenNotifications)
            }
        }
        item {
            Text(
                text = "یادمان • نسخه ۱٫۱٫۰",
                style = YText.Meta,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (editName) {
        var value by remember { mutableStateOf(state.name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            containerColor = YColors.Card,
            title = { Text(text = "نام نمایشی", style = YText.CardTitle) },
            text = { YTextField(value = value, onValueChange = { value = it.take(30) }, placeholder = "نام تو") },
            confirmButton = {
                TextButton(onClick = {
                    vm.setName(value)
                    editName = false
                }) { Text(text = "ذخیره", style = YText.Name, color = YColors.Gold2) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text(text = "انصراف", style = YText.Name, color = YColors.TextSecondary) }
            }
        )
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب؟",
            message = "داده‌های روی گوشی پاک نمی‌شوند ولی دیگر سینک نمی‌شوند.",
            confirmText = "خروج",
            onConfirm = {
                confirmLogout = false
                vm.logout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, color: Color, title: String, subtitle: String) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = color, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YText.Name)
            Text(text = subtitle, style = YText.Meta)
        }
    }
}

@Composable
private fun HealthRow(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    okText: String,
    fixText: String,
    onFix: () -> Unit,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(
            color = if (ok) YColors.Success else YColors.Danger,
            icon = icon,
            size = 40.dp,
            cornerRadius = 13.dp,
            iconSize = 20.dp,
            bgAlpha = 0.16f
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = YText.Name)
            if (subtitle != null) Text(text = subtitle, style = YText.Meta)
        }
        if (ok) Chip(label = okText, color = YColors.Success, icon = YIcons.CheckMark)
        else GoldButton(text = fixText, onClick = onFix, small = true)
    }
}

@Composable
private fun QuickLinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(YRadius.Md)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = YColors.Gold2, icon = icon, size = 40.dp, cornerRadius = 13.dp, iconSize = 20.dp, bgAlpha = 0.16f)
        Text(text = title, style = YText.Name, modifier = Modifier.weight(1f))
        Icon(imageVector = YIcons.ChevronLeft, contentDescription = null, tint = YColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}
