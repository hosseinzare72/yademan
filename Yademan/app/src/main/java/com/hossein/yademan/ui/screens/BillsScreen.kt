// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/BillsScreen.kt
package com.hossein.yademan.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

data class BillsState(
    val monthTitle: String = "",
    val activeCount: Int = 0,
    val remaining: Long = 0,
    val paidPercent: Int = 0,
    val nearCount: Int = 0,
    val installments: List<BillInfo> = emptyList(),
    val checks: List<BillInfo> = emptyList(),
    val unpaidOnly: Boolean = false
)

class BillsViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders
    private val unpaidOnly = MutableStateFlow(false)

    val state: StateFlow<BillsState> = combine(repo.observeAll(), TimeTicker.now, unpaidOnly) { list, now, onlyUnpaid ->
        val bills = list.filter { BillsLogic.isBill(it) }.map { BillsLogic.info(it, now) }
            .sortedWith(compareBy<BillInfo> { it.finished }.thenBy { it.paid }.thenBy { it.occurrence })
        val month = bills.filter { it.inThisMonth || (!it.paid && it.daysLeft < 0) }
        val remaining = month.filter { !it.paid }.sumOf { it.reminder.amountToman }
        val paidSum = month.filter { it.paid && it.inThisMonth }.sumOf { it.reminder.amountToman }
        val percent = if (remaining + paidSum == 0L) 0 else ((paidSum * 100) / (remaining + paidSum)).toInt()
        val visible = if (onlyUnpaid) bills.filter { !it.paid } else bills
        BillsState(
            monthTitle = JalaliConverter.fromMillis(now).formatMonthYear(),
            activeCount = bills.count { !it.paid },
            remaining = remaining,
            paidPercent = percent,
            nearCount = bills.count { !it.paid && it.daysLeft in 0..7 },
            installments = visible.filter { it.type == ReminderType.INSTALLMENT },
            checks = visible.filter { it.type == ReminderType.CHECK },
            unpaidOnly = onlyUnpaid
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default) // محاسبات از نخ اصلی خارج شد (قبلاً باعث لگ هنگام ورود به صفحه بود)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsState())

    fun toggleFilter() {
        unpaidOnly.value = !unpaidOnly.value
    }

    /** پرداخت: برای تکراری‌ها فقط وقوع همین ماه علامت می‌خورد؛ یک‌بارها is_done=true */
    fun pay(b: BillInfo) {
        viewModelScope.launch { repo.setDoneOn(b.reminder, DateUtils.dayKey(b.occurrence), true) }
    }

    fun undoPay(b: BillInfo) {
        viewModelScope.launch { repo.setDoneOn(b.reminder, DateUtils.dayKey(b.occurrence), false) }
    }

    fun delete(b: BillInfo) {
        viewModelScope.launch { repo.delete(b.reminder.id) }
    }
}

@Composable
fun BillsScreen(
    initialTab: Int,
    onBack: () -> Unit,
    onAdd: (type: String) -> Unit,
    vm: BillsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var toDelete by remember { mutableStateOf<BillInfo?>(null) }
    val visibleBills = if (tab == 0) state.installments else state.checks

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "اقساط و چک‌ها",
                subtitle = "${state.monthTitle} • ${state.activeCount.fa()} مورد فعال",
                leading = { HeaderIconButton(icon = YIcons.ChevronRight, contentDescription = "بازگشت", onClick = onBack) },
                actions = {
                    HeaderIconButton(
                        icon = YIcons.Filter,
                        contentDescription = "فیلتر",
                        onClick = vm::toggleFilter,
                        showDot = state.unpaidOnly
                    )
                }
            )
        }
        item {
            AppCard(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .fillMaxWidth(),
                brush = YBrush.HeroCard
            ) {
                Column {
                    Text(text = "مانده پرداخت این ماه", style = YText.Sub)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(text = state.remaining.fa(), style = YText.Title.copy(fontSize = 26.sp, color = YColors.Gold1))
                        Text(text = " تومان", style = YText.Sub, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                    }
                    LinearProgress(progress = state.paidPercent / 100f, modifier = Modifier.padding(top = 14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${PersianDigits.percent(state.paidPercent)} پرداخت شده", style = YText.MetaBold)
                        Chip(
                            label = "${state.nearCount.fa()} سررسید نزدیک",
                            color = if (state.nearCount > 0) YColors.Warning else YColors.Success
                        )
                    }
                }
            }
        }
        item {
            SegmentedRow(
                options = listOf("اقساط (${state.installments.size.fa()})", "چک‌ها (${state.checks.size.fa()})"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
            )
        }
        if (visibleBills.isEmpty()) {
            item {
                EmptyState(
                    message = if (tab == 0) "قسطی ثبت نشده" else "چکی ثبت نشده",
                    icon = if (tab == 0) YIcons.Installment else YIcons.Check
                )
            }
        } else {
            items(visibleBills, key = { it.reminder.id }) { b ->
                BillRow(
                    b = b,
                    onPay = { vm.pay(b) },
                    onUndo = { vm.undoPay(b) },
                    onLongPress = { toDelete = b },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
        item {
            GhostButton(
                text = if (tab == 0) "+ قسط جدید" else "+ چک جدید",
                onClick = { onAdd(if (tab == 0) ReminderType.INSTALLMENT else ReminderType.CHECK) },
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            )
        }
    }

    toDelete?.let { b ->
        ConfirmDialog(
            title = "حذف «${b.reminder.title}»؟",
            message = "این مورد و یادآورهایش حذف می‌شود.",
            onConfirm = {
                vm.delete(b)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillRow(
    b: BillInfo,
    onPay: () -> Unit,
    onUndo: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val r = b.reminder
    val color = typeColor(b.type)
    val chipColor = when {
        b.paid -> YColors.Success
        b.daysLeft <= 0 -> YColors.Danger
        b.daysLeft <= 3 -> YColors.Warning
        else -> YColors.Info
    }
    val bank = ReminderMeta.str(r, "bank")?.takeIf { it.isNotBlank() } ?: ReminderType.labelFa(b.type)
    val shape = RoundedCornerShape(YRadius.Md)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(color = color, icon = typeIcon(b.type), size = 44.dp, cornerRadius = 14.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = r.title, style = YText.Name, maxLines = 1)
                Text(
                    text = "$bank • ${Recurrence.labelFa(r)} • ${DateUtils.dateShortFa(b.occurrence)}",
                    style = YText.Meta,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = r.amountToman.fa(), style = YText.Amount)
                Text(text = "تومان", style = YText.Meta)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip(label = BillsLogic.statusText(b), color = chipColor, icon = if (b.paid) YIcons.CheckMark else YIcons.Clock)
                b.progressText?.let { Chip(label = (if (b.type == ReminderType.INSTALLMENT) "قسط " else "") + it, color = if (b.finished) YColors.Success else YColors.Gold2) }
            }
            if (b.paid) {
                GhostButton(text = "برگرداندن", onClick = onUndo)
            } else {
                GoldButton(text = "پرداخت", onClick = onPay, small = true)
            }
        }
    }
}
