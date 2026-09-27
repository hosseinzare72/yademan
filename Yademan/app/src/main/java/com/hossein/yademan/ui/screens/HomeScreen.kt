// مسیر: app/src/main/java/com/hossein/yademan/ui/screens/HomeScreen.kt
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.ui.components.*
import com.hossein.yademan.ui.theme.*
import com.hossein.yademan.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** کلیدهای هاب خدمات */
object HubKey {
    const val INSTALLMENT = "installment"
    const val CHECK = "check"
    const val SUBSCRIPTION = "subscription"
    const val MEDICINE = "medicine"
    const val MEETING = "meeting"
    const val SHOPPING = "shopping"
    const val NOTE = "note"
}

/** آیتم ثابت هاب (مطابق home.html مخزن) */
private data class HubDef(val key: String, val label: String, val icon: ImageVector, val color: Color)

private val hubDefs = listOf(
    HubDef(HubKey.INSTALLMENT, "اقساط", YIcons.Installment, YColors.CatInstallment),
    HubDef(HubKey.CHECK, "چک‌ها", YIcons.Check, YColors.CatCheck),
    HubDef(HubKey.SUBSCRIPTION, "اشتراک‌ها", YIcons.Subscription, YColors.CatSubscription),
    HubDef("bills", "قبوض", YIcons.Bill, YColors.Info),
    HubDef("building", "شارژ ساختمان", YIcons.Building, YColors.Info),
    HubDef(HubKey.MEDICINE, "داروها", YIcons.Medicine, YColors.CatMedicine),
    HubDef(HubKey.MEETING, "قرارها", YIcons.Meeting, YColors.CatMeeting),
    HubDef("travel", "مسافرت", YIcons.Travel, YColors.CatShopping),
    HubDef(HubKey.SHOPPING, "خرید", YIcons.Cart, YColors.CatShopping),
    HubDef(HubKey.NOTE, "یادداشت‌ها", YIcons.Note, YColors.CatNote),
    HubDef("insurance", "بیمه‌ها", YIcons.Insurance, YColors.Info),
    HubDef("car", "سرویس خودرو", YIcons.Car, YColors.Warning),
    HubDef("salary", "حقوق", YIcons.Salary, YColors.CatShopping),
    HubDef("pocket", "پول توجیبی", YIcons.PocketMoney, YColors.Gold1),
    HubDef("birthday", "تولدها", YIcons.Birthday, YColors.CatMedicine),
    HubDef("anniversary", "سالگردها", YIcons.Anniversary, YColors.Danger)
)

/** بج هر کاشی: count=null یعنی نقطه سبز خالی */
data class HubBadge(val count: Int?, val color: Color)

data class TaskRow(
    val reminder: Reminder,
    val type: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val done: Boolean,
    val occurrence: Long
)

data class HomeState(
    val name: String = "",
    val date: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val importantLeft: Int = 0,
    val unread: Int = 0,
    val badges: Map<String, HubBadge> = emptyMap(),
    val tasks: List<TaskRow> = emptyList()
)

class HomeViewModel : ViewModel() {
    private val repo = ServiceLocator.reminders

    val state: StateFlow<HomeState> = combine(
        repo.observeAll(),
        ServiceLocator.notifications.observeUnreadCount(),
        ServiceLocator.meta.observe(MetaKeys.DISPLAY_NAME),
        TimeTicker.now
    ) { list, unread, name, now -> build(list, unread, name, now) }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun build(list: List<Reminder>, unread: Int, name: String?, now: Long): HomeState {
        val todayStart = DateUtils.startOfDay(now)
        val todayKey = DateUtils.dayKey(now)
        val tasks = list.mapNotNull { r ->
            val type = ReminderMeta.type(r)
            if (type == ReminderType.SHOPPING || type == ReminderType.NOTE) return@mapNotNull null
            val occ = Recurrence.occurrenceOnDay(r, todayStart) ?: return@mapNotNull null
            TaskRow(r, type, r.title, reminderSubtitle(r), DateUtils.timeFa(occ), ReminderMeta.isDoneOn(r, todayKey), occ)
        }.sortedBy { it.occurrence }

        val bills = list.filter { BillsLogic.isBill(it) || BillsLogic.isSubscription(it) }.map { BillsLogic.info(it, now) }
        val urgentBills = bills.filter { !it.paid && it.daysLeft <= 7 }
        val importantTasks = tasks.count {
            !it.done && it.reminder.priority >= 2 && !BillsLogic.isBill(it.reminder) && !BillsLogic.isSubscription(it.reminder)
        }

        fun billBadge(type: String): HubBadge {
            val items = urgentBills.filter { it.type == type }
            return if (items.isEmpty()) HubBadge(null, YColors.Success)
            else HubBadge(items.size, if (items.any { it.daysLeft <= 1 }) YColors.Danger else YColors.Warning)
        }
        val medsLeft = tasks.count { it.type == ReminderType.MEDICINE && !it.done }
        val meetingsLeft = tasks.count { it.type == ReminderType.MEETING && !it.done && it.occurrence >= now }
        val shoppingOpen = list.count { ReminderMeta.type(it) == ReminderType.SHOPPING && !it.isDone }
        val badges = mapOf(
            HubKey.INSTALLMENT to billBadge(ReminderType.INSTALLMENT),
            HubKey.CHECK to billBadge(ReminderType.CHECK),
            HubKey.SUBSCRIPTION to billBadge(ReminderType.SUBSCRIPTION),
            HubKey.MEDICINE to if (medsLeft > 0) HubBadge(medsLeft, YColors.Warning) else HubBadge(null, YColors.Success),
            HubKey.MEETING to if (meetingsLeft > 0) HubBadge(meetingsLeft, YColors.Danger) else HubBadge(null, YColors.Success),
            HubKey.SHOPPING to if (shoppingOpen > 0) HubBadge(shoppingOpen, YColors.Warning) else HubBadge(null, YColors.Success)
        )

        return HomeState(
            name = name?.takeIf { it.isNotBlank() } ?: ServiceLocator.auth.username ?: "دوست من",
            date = JalaliConverter.fromMillis(now).formatLong(),
            done = tasks.count { it.done },
            total = tasks.size,
            importantLeft = urgentBills.size + importantTasks,
            unread = unread,
            badges = badges,
            tasks = tasks
        )
    }

    fun toggle(row: TaskRow) {
        viewModelScope.launch {
            repo.setDoneOn(row.reminder, DateUtils.dayKey(row.occurrence), !row.done)
        }
    }
}

@Composable
fun HomeScreen(
    onBellClick: () -> Unit,
    onPlanClick: () -> Unit,
    onHubClick: (key: String, label: String) -> Unit,
    onManageClick: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            ScreenHeader(
                title = "سلام، ${state.name}",
                subtitle = state.date,
                modifier = Modifier.padding(top = 4.dp),
                leading = { Avatar(letter = state.name.take(1)) },
                actions = {
                    HeaderIconButton(
                        icon = YIcons.Bell,
                        contentDescription = "اعلان‌ها",
                        onClick = onBellClick,
                        showDot = state.unread > 0
                    )
                }
            )
        }
        item {
            ProgressCard(
                done = state.done,
                total = state.total,
                importantLeft = state.importantLeft,
                onPlanClick = onPlanClick
            )
        }
        item {
            SectionTitle(title = "خدمات یادمان", trailing = "مدیریت +", onTrailingClick = onManageClick)
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val cells: List<HubDef?> = hubDefs + listOf(null) // null = کاشی «افزودن»
                cells.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { def ->
                            if (def != null) {
                                HubCell(
                                    def = def,
                                    badge = state.badges[def.key] ?: HubBadge(null, YColors.Success),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onHubClick(def.key, def.label) }
                                )
                            } else {
                                AddCell(Modifier.weight(1f), onClick = { onHubClick("add", "") })
                            }
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            SectionTitle(
                title = "کارهای امروز",
                trailingChip = { Chip(label = "${state.tasks.size.fa()} کار", color = YColors.Gold2) }
            )
        }
        if (state.tasks.isEmpty()) {
            item { EmptyState(message = "برای امروز کاری ثبت نشده. با دکمه + یکی بساز!", icon = YIcons.Calendar) }
        } else {
            items(state.tasks, key = { it.reminder.id }) { row ->
                TaskRowView(
                    row = row,
                    onToggle = { vm.toggle(row) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun Avatar(letter: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(YColors.Gold2.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(YBrush.Gold),
            contentAlignment = Alignment.Center
        ) {
            Text(text = letter, style = YText.Title.copy(fontSize = 20.sp), color = YColors.OnGold)
        }
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int, importantLeft: Int, onPlanClick: () -> Unit) {
    val progress = if (total == 0) 0f else done.toFloat() / total
    val percent = (progress * 100).toInt()
    val shape = RoundedCornerShape(YRadius.Lg)
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(YBrush.HeroCard)
            .border(1.dp, YColors.Line, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProgressRing(progress = progress, size = 96.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = PersianDigits.percent(percent), style = YText.RingValue)
                Text(text = "پیشرفت", style = YText.RingCaption)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "پیشرفت امروز", style = YText.CardTitle)
            Text(
                text = "${done.fa()} از ${total.fa()} کار انجام شد",
                style = YText.Sub,
                modifier = Modifier.padding(top = 4.dp)
            )
            Chip(
                label = "${importantLeft.fa()} سررسید مهم باقی مانده",
                color = if (importantLeft > 0) YColors.Warning else YColors.Success,
                modifier = Modifier.padding(top = 6.dp)
            )
            GoldButton(
                text = "مشاهده برنامه",
                onClick = onPlanClick,
                small = true,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun HubCell(def: HubDef, badge: HubBadge, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(58.dp)) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(YColors.Card)
                    .border(1.dp, YColors.Line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = def.icon, contentDescription = null, tint = def.color, modifier = Modifier.size(24.dp))
            }
            val badgeShape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-3).dp)
                    .defaultMinSize(minWidth = 18.dp)
                    .height(18.dp)
                    .clip(badgeShape)
                    .background(badge.color)
                    .border(2.dp, YColors.Bg0, badgeShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (badge.count != null) Text(text = badge.count.fa(), style = YText.Badge)
            }
        }
        Text(text = def.label, style = YText.Label, maxLines = 1)
    }
}

@Composable
private fun AddCell(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(YColors.Card)
                .drawBehind {
                    drawCircle(
                        color = YColors.TextMuted.copy(alpha = 0.35f),
                        radius = size.minDimension / 2f - 1.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = YIcons.Plus, contentDescription = null, tint = YColors.TextMuted, modifier = Modifier.size(22.dp))
        }
        Text(text = "افزودن", style = YText.Label, color = YColors.TextMuted, maxLines = 1)
    }
}

/** ردیف کار امروز: کاشی، عنوان، زیرعنوان، چیپ ساعت، دایره چک */
@Composable
fun TaskRowView(row: TaskRow, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(YRadius.Md)
    val color = typeColor(row.type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(YColors.Card)
            .border(1.dp, YColors.Line, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(color = color, icon = typeIcon(row.type), size = 42.dp, cornerRadius = 14.dp, iconSize = 21.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = YText.Name,
                color = if (row.done) YColors.TextMuted else YColors.TextPrimary,
                textDecoration = if (row.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )
            Text(text = row.subtitle, style = YText.Meta, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        Chip(label = row.time, color = if (row.done) YColors.TextMuted else color, icon = YIcons.Clock)
        CheckCircle(checked = row.done, onToggle = onToggle)
    }
}
