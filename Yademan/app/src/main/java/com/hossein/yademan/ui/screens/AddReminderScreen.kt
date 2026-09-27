// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/AddReminderScreen.kt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType

/** گزینه‌های گرید نوع (مطابق new-reminder.html) */
private data class TypeOption(val type: String, val label: String, val icon: ImageVector, val color: Color)

private val typeOptions = listOf(
    TypeOption(ReminderType.INSTALLMENT, "قسط", YIcons.Installment, YColors.CatInstallment),
    TypeOption(ReminderType.CHECK, "چک", YIcons.Check, YColors.CatCheck),
    TypeOption(ReminderType.SUBSCRIPTION, "اشتراک", YIcons.Subscription, YColors.CatSubscription),
    TypeOption(ReminderType.MEETING, "قرار", YIcons.Meeting, YColors.CatMeeting),
    TypeOption(ReminderType.MEDICINE, "دارو", YIcons.Medicine, YColors.CatMedicine),
    TypeOption(ReminderType.SHOPPING, "خرید", YIcons.Cart, YColors.CatShopping),
    TypeOption(ReminderType.OTHER, "سایر", YIcons.Other, YColors.Gold3)
)

private val repeatRules = listOf("none", "daily", "weekly", "monthly", "yearly")
private val beforeMinutes = listOf(15, 60, 1440)

class AddReminderViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    var type by mutableStateOf(ReminderType.OTHER)
        private set
    var title by mutableStateOf("")
    var amount by mutableStateOf("")
    var detail by mutableStateOf("")
    var dayMillis by mutableLongStateOf(DateUtils.todayStart())
    var hour by mutableIntStateOf(9)
    var minute by mutableIntStateOf(0)
    var repeatIndex by mutableIntStateOf(0)
    /** تعداد دفعات تکرار (مثلاً ۱۰ قسط)؛ خالی = نامحدود */
    var countText by mutableStateOf("")
    var beforeIndex by mutableIntStateOf(0)
    var priorityIndex by mutableIntStateOf(1)
    var error by mutableStateOf<String?>(null)
    /** جلوگیری از ذخیره دوباره با دو بار لمس سریع */
    var saving by mutableStateOf(false)
        private set
    private var initialized = false

    /** مقداردهی اولیه از آرگومان‌های مسیر (فقط یک‌بار) */
    fun setup(initialType: String?, initialTitle: String?, initialDay: Long?) {
        if (initialized) return
        initialized = true
        val now = System.currentTimeMillis()
        // ساعت پیش‌فرض: یک ساعت بعد، گرد به ۵ دقیقه
        val next = now + DateUtils.HOUR
        hour = DateUtils.hourOf(next)
        minute = (DateUtils.minuteOf(next) / 5) * 5
        dayMillis = if (initialDay != null && initialDay > 0) DateUtils.startOfDay(initialDay) else DateUtils.startOfDay(next)
        selectType(initialType ?: ReminderType.OTHER)
        if (!initialTitle.isNullOrBlank()) title = initialTitle
    }

    fun selectType(t: String) {
        type = t
        // پیش‌فرض‌های منطقی هر نوع
        when (t) {
            ReminderType.INSTALLMENT -> { repeatIndex = 3; beforeIndex = 2; priorityIndex = 2 }
            ReminderType.CHECK -> { repeatIndex = 0; beforeIndex = 2; priorityIndex = 2 }
            ReminderType.SUBSCRIPTION -> { repeatIndex = 3; beforeIndex = 2; priorityIndex = 1 }
            ReminderType.MEDICINE -> { repeatIndex = 1; beforeIndex = 0; priorityIndex = 1 }
            ReminderType.MEETING -> { repeatIndex = 0; beforeIndex = 1; priorityIndex = 1 }
            else -> Unit
        }
    }

    fun shiftDay(d: Int) {
        val candidate = DateUtils.addDays(dayMillis, d)
        if (DateUtils.epochDay(candidate) >= DateUtils.epochDay(System.currentTimeMillis())) dayMillis = candidate
    }

    fun shiftHour(d: Int) {
        hour = Math.floorMod(hour + d, 24)
    }

    fun shiftMinute(d: Int) {
        minute = Math.floorMod(minute + d, 60)
    }

    val dueMillis: Long get() = DateUtils.atTime(dayMillis, hour, minute)

    /** پیش‌نمایش تاریخ آخرین وقوع برای فیلد «تعداد دفعات» */
    fun lastOccurrencePreview(count: Int): Long? {
        val rule = repeatRules.getOrNull(repeatIndex) ?: return null
        if (rule == "none") return null
        val probe = Reminder(
            id = "preview", title = "", dueAt = DateUtils.millisToIso(dueMillis), repeatRule = rule,
            createdAt = "", updatedAt = "", deviceId = ""
        )
        return Recurrence.nthOccurrence(probe, count)
    }

    fun save(onDone: () -> Unit) {
        if (saving) return
        val t = title.trim()
        if (t.isEmpty()) {
            error = "عنوان را وارد کن"
            return
        }
        error = null
        val due = if (type == ReminderType.SHOPPING) System.currentTimeMillis() else dueMillis
        val extras: Map<String, Any?> = when (type) {
            ReminderType.INSTALLMENT, ReminderType.CHECK -> mapOf("bank" to detail.trim())
            ReminderType.SUBSCRIPTION -> mapOf("plan" to detail.trim())
            ReminderType.MEETING, ReminderType.OTHER -> mapOf("location" to detail.trim())
            ReminderType.MEDICINE -> mapOf("dose" to detail.trim())
            ReminderType.SHOPPING -> mapOf("group" to detail.trim().ifBlank { ShoppingViewModel.DEFAULT_GROUP })
            else -> emptyMap()
        }
        val rule = if (type == ReminderType.SHOPPING) "none" else repeatRules[repeatIndex]
        val count = if (rule == "none") null else PersianDigits.parseLong(countText)?.toInt()?.takeIf { it in 1..999 }
        val allExtras = if (count != null) extras + ("total_count" to count) else extras
        val base = repo.newReminder(type, t, due)
        val draft = base.copy(
            repeatRule = rule,
            alarmEnabled = type != ReminderType.SHOPPING,
            alarmOffsetMinutes = beforeMinutes[beforeIndex],
            priority = priorityIndex,
            amountToman = PersianDigits.parseLong(amount) ?: 0L,
            metaJson = ReminderMeta.build(type, allExtras)
        )
        // تعداد محدود → end_at = پایان روزِ آخرین وقوع؛ بعد از آن نه وقوعی ساخته می‌شود نه هشداری
        val rec = if (count != null) {
            val last = Recurrence.nthOccurrence(draft, count)
            if (last != null) draft.copy(endAt = DateUtils.millisToIso(DateUtils.addDays(DateUtils.startOfDay(last), 1) - 1000)) else draft
        } else draft
        saving = true
        viewModelScope.launch {
            try {
                repo.create(rec)
                onDone()
            } finally {
                saving = false
            }
        }
    }
}

@Composable
fun AddReminderScreen(
    initialType: String?,
    initialTitle: String?,
    initialDay: Long?,
    onClose: () -> Unit,
    vm: AddReminderViewModel = viewModel()
) {
    LaunchedEffect(Unit) { vm.setup(initialType, initialTitle, initialDay) }
    val isShopping = vm.type == ReminderType.SHOPPING
    val showAmount = vm.type == ReminderType.INSTALLMENT || vm.type == ReminderType.CHECK ||
        vm.type == ReminderType.SUBSCRIPTION || isShopping
    val detailPlaceholder = when (vm.type) {
        ReminderType.INSTALLMENT, ReminderType.CHECK -> "بانک (مثلاً بانک ملت)"
        ReminderType.SUBSCRIPTION -> "سرویس یا پلن (مثلاً فیلیمو خانوادگی)"
        ReminderType.MEDICINE -> "دوز (مثلاً ۵۰۰ میلی‌گرم بعد از غذا)"
        ReminderType.SHOPPING -> "گروه (مثلاً لبنیات)"
        ReminderType.MEETING -> "مکان (اختیاری)"
        else -> "توضیح یا مکان (اختیاری)"
    }
    val j = JalaliConverter.fromMillis(vm.dayMillis)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader(
            title = "یادآوری جدید",
            actions = { HeaderIconButton(icon = YIcons.Close, contentDescription = "بستن", onClick = onClose) }
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // گرید ۶ کارت نوع (۳ ستون × ۲ ردیف)
            typeOptions.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { opt ->
                        TypeCard(
                            option = opt,
                            selected = vm.type == opt.type,
                            onClick = { vm.selectType(opt.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            Text(text = "عنوان", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
            YTextField(value = vm.title, onValueChange = { vm.title = it; vm.error = null }, placeholder = "مثلاً قسط وام مسکن")
            if (vm.error != null) Text(text = vm.error ?: "", style = YText.Meta, color = YColors.Danger)

            if (showAmount) {
                YTextField(
                    value = vm.amount,
                    onValueChange = { v -> vm.amount = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(13) },
                    placeholder = if (isShopping) "قیمت (تومان)" else "مبلغ (تومان)",
                    keyboardType = KeyboardType.Number,
                    trailing = {
                        val n = PersianDigits.parseLong(vm.amount)
                        if (n != null) Text(text = n.fa(), style = YText.MetaBold, color = YColors.Gold2)
                    }
                )
            }
            YTextField(value = vm.detail, onValueChange = { vm.detail = it }, placeholder = detailPlaceholder)

            if (!isShopping) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    StepperBox(
                        label = "تاریخ",
                        value = "${JalaliConverter.weekdayNameFa(j.weekday)} ${j.formatShort()}",
                        modifier = Modifier.weight(1.3f),
                        steppers = listOf(
                            StepperAction("روز −") { vm.shiftDay(-1) },
                            StepperAction("روز +") { vm.shiftDay(1) }
                        )
                    )
                    StepperBox(
                        label = "ساعت",
                        value = "%02d:%02d".format(java.util.Locale.US, vm.hour, vm.minute).fa(),
                        modifier = Modifier.weight(1f),
                        steppers = listOf(
                            StepperAction("ساعت −") { vm.shiftHour(-1) },
                            StepperAction("ساعت +") { vm.shiftHour(1) },
                            StepperAction("دقیقه −۵") { vm.shiftMinute(-5) },
                            StepperAction("دقیقه +۵") { vm.shiftMinute(5) }
                        )
                    )
                }

                Text(text = "تکرار", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(listOf("یک‌بار", "روزانه", "هفتگی", "ماهانه", "سالانه"), vm.repeatIndex, { vm.repeatIndex = it })

                if (vm.repeatIndex != 0) {
                    val unit = when (vm.type) {
                        ReminderType.INSTALLMENT -> "قسط"
                        ReminderType.CHECK -> "چک"
                        ReminderType.SUBSCRIPTION -> "دوره"
                        else -> "بار"
                    }
                    Text(text = "تعداد دفعات", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                    YTextField(
                        value = vm.countText,
                        onValueChange = { v -> vm.countText = v.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }.take(3) },
                        placeholder = "خالی = نامحدود (مثلاً ۱۰ $unit)",
                        keyboardType = KeyboardType.Number,
                        trailing = {
                            val n = PersianDigits.parseLong(vm.countText)
                            if (n != null && n > 0) Text(text = "${n.fa()} $unit", style = YText.MetaBold, color = YColors.Gold2)
                        }
                    )
                    val n = PersianDigits.parseLong(vm.countText)?.toInt()
                    if (n != null && n > 0) {
                        val last = vm.lastOccurrencePreview(n)
                        if (last != null) {
                            Text(
                                text = "آخرین $unit: ${DateUtils.dateShortFa(last)} • بعد از پرداخت آن، تکرار و هشدار خودکار تمام می‌شود",
                                style = YText.Meta,
                                color = YColors.TextSecondary
                            )
                        }
                    }
                }

                Text(text = "یادآوری قبل", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(
                    listOf("${15.fa()} دقیقه", "${1.fa()} ساعت", "${1.fa()} روز"),
                    vm.beforeIndex,
                    { vm.beforeIndex = it }
                )

                Text(text = "اولویت", style = YText.Section, modifier = Modifier.padding(top = 6.dp))
                SegmentedRow(listOf("کم", "متوسط", "زیاد"), vm.priorityIndex, { vm.priorityIndex = it })
            }

            GoldButton(
                text = "ذخیره یادآوری",
                onClick = { vm.save(onClose) },
                enabled = !vm.saving,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun TypeCard(option: TypeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) YColors.Gold2.copy(alpha = 0.08f) else YColors.Card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) YColors.Gold2 else YColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconTile(color = option.color, icon = option.icon, size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Text(
            text = option.label,
            style = YText.Name,
            color = if (selected) YColors.Gold1 else YColors.TextSecondary
        )
    }
}

class StepperAction(val label: String, val action: () -> Unit)

/** کادر تاریخ/ساعت با دکمه‌های کم/زیاد */
@Composable
private fun StepperBox(label: String, value: String, steppers: List<StepperAction>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, style = YText.Meta)
        Text(text = value, style = YText.CardTitle.copy(color = YColors.Gold1), maxLines = 1)
        steppers.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(YColors.Card2)
                            .clickable(onClick = s.action)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = s.label, style = YText.Chip, color = YColors.TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}
