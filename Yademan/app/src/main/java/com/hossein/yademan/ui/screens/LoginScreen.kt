// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/LoginScreen.kt
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.services.ApiClient
import com.hossein.yademan.services.ApiException
import com.hossein.yademan.services.SyncManager
import com.hossein.yademan.services.SyncWorker
import com.hossein.yademan.services.str
import com.hossein.yademan.services.obj
import java.io.IOException

class LoginViewModel : ViewModel() {
    var mode by mutableIntStateOf(0) // ۰=ورود، ۱=ثبت‌نام
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun submit(onSuccess: () -> Unit) {
        if (loading) return
        val u = username.trim()
        val p = password
        if (u.length < 3) {
            error = "نام کاربری حداقل ۳ حرف باشد"
            return
        }
        if (p.length < 6) {
            error = "رمز عبور حداقل ۶ کاراکتر باشد"
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val auth = ServiceLocator.auth
                val device = auth.deviceId
                var resp = if (mode == 1) ApiClient.register(u, p, device) else ApiClient.login(u, p, device)
                var token = resp.str("token")
                // اگر ثبت‌نام توکن برنگرداند، بلافاصله وارد می‌شویم
                if (token.isNullOrBlank() && mode == 1) {
                    resp = ApiClient.login(u, p, device)
                    token = resp.str("token")
                }
                if (token.isNullOrBlank()) throw ApiException("توکن از سرور دریافت نشد")
                val userId = resp.str("user_id") ?: resp.obj("user")?.str("id")
                auth.saveAuth(token, userId, u)

                val meta = ServiceLocator.meta
                // اولین سینک بعد از ورود: کامل (pull همه رکوردهای حساب + push داده‌های محلی)
                meta.delete(MetaKeys.LAST_SYNC)
                if (meta.get(MetaKeys.DISPLAY_NAME).isNullOrBlank()) meta.set(MetaKeys.DISPLAY_NAME, u)
                SyncWorker.schedulePeriodic(ServiceLocator.app)
                SyncManager.sync()
                onSuccess()
            } catch (e: ApiException) {
                error = e.message ?: "خطای سرور"
            } catch (e: IOException) {
                error = "اتصال به اینترنت برقرار نیست"
            } catch (e: Exception) {
                error = e.message ?: "خطای ناشناخته"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun LoginScreen(onClose: () -> Unit, vm: LoginViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader(
            title = if (vm.mode == 0) "ورود به حساب" else "ساخت حساب",
            subtitle = "همگام‌سازی با kafezare.ir",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose) }
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(YColors.Gold2.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = YIcons.Bell, contentDescription = null, tint = YColors.Gold2, modifier = Modifier.size(40.dp))
            }
            Text(text = "یادمان", style = YText.Title.copy(fontSize = 28.sp, color = YColors.Gold1))
            Text(text = "یادآورهایت روی همه گوشی‌ها، همیشه همراهت", style = YText.Sub)

            SegmentedRow(
                options = listOf("ورود", "ثبت‌نام"),
                selectedIndex = vm.mode,
                onSelect = {
                    vm.mode = it
                    vm.error = null
                },
                modifier = Modifier.padding(top = 10.dp)
            )
            YTextField(
                value = vm.username,
                onValueChange = { vm.username = it.trim() },
                placeholder = "نام کاربری",
                imeAction = ImeAction.Next
            )
            YTextField(
                value = vm.password,
                onValueChange = { vm.password = it },
                placeholder = "رمز عبور",
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                onImeAction = { vm.submit(onClose) }
            )
            vm.error?.let { Text(text = it, style = YText.Meta, color = YColors.Danger) }
            GoldButton(
                text = when {
                    vm.loading -> "لطفاً صبر کن…"
                    vm.mode == 0 -> "ورود"
                    else -> "ثبت‌نام"
                },
                onClick = { vm.submit(onClose) },
                enabled = !vm.loading
            )
            Text(
                text = "بدون حساب هم می‌توانی از اپ استفاده کنی؛ داده‌ها روی گوشی ذخیره می‌شوند.",
                style = YText.Meta,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
