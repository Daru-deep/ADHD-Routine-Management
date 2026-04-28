package com.example.adhd_routine_management.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.DailyRecord
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.database.entity.TimeSlot
import com.example.adhd_routine_management.data.database.entity.UserProgress
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.domain.model.CharacterStage
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// ────────────────────────────────────────────
// ViewModel
// ────────────────────────────────────────────

class ProgressViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val progress: StateFlow<UserProgress?> = repository.getProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecords: StateFlow<List<DailyRecord>> = repository.getRecentRecords(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今月の体調記録（週次・月次カード表示用） */
    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val from = today.withDayOfMonth(1).toString()
            val to   = today.toString()
            _healthRecords.value = repository.getHealthRecordsForRange(from, to)
        }
    }

    companion object {
        fun factory(repository: TaskRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(repository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(repository: TaskRepository) {
    val vm: ProgressViewModel = viewModel(
        factory = ProgressViewModel.factory(repository)
    )
    val progress      by vm.progress.collectAsStateWithLifecycle()
    val records       by vm.recentRecords.collectAsStateWithLifecycle()
    val healthRecords by vm.healthRecords.collectAsStateWithLifecycle()

    var isRecordsExpanded by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("進捗・実績", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ステータスカード
            item {
                progress?.let { p ->
                    StatusCard(p)
                }
            }

            // キャラ成長バー
            item {
                progress?.let { p ->
                    CharacterGrowthCard(p.totalPoints)
                }
            }

            // 今週の体調カード
            item {
                WeeklyHealthCard(healthRecords = healthRecords)
            }

            // 今月の体調カード
            item {
                MonthlyHealthCard(healthRecords = healthRecords)
            }

            // 直近30日の記録（タップで折りたたみ/展開）
            if (records.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRecordsExpanded = !isRecordsExpanded }
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "直近の記録",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = if (isRecordsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRecordsExpanded) "折りたたむ" else "展開する",
                            tint = TextSecondary
                        )
                    }
                }
                if (isRecordsExpanded) {
                    items(records) { record ->
                        DailyRecordRow(record)
                    }
                }
            }
        }
    }
}

/**
 * 今週（月〜日）の体調記録をグリッド表示するカード。
 * 行 = スロット（朝/昼/夕）、列 = 曜日（月〜日）
 * スコアは 8以上→緑、5〜7→青緑、4以下→赤 でカラーコード。
 */
@Composable
private fun WeeklyHealthCard(healthRecords: List<HealthRecord>) {
    val today     = LocalDate.now()
    val weekStart = today.with(DayOfWeek.MONDAY)
    val days      = (0..6).map { weekStart.plusDays(it.toLong()) }
    val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")
    val slots     = listOf(TimeSlot.MORNING, TimeSlot.NOON, TimeSlot.EVENING)
    val slotLabels = listOf("朝", "昼", "夕")

    // 日付 → スロット → スコア の2段マップを作成
    val recordMap = healthRecords
        .filter { it.date >= weekStart.toString() }
        .groupBy { it.date }
        .mapValues { (_, recs) -> recs.associate { it.timeSlot to it.score } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今週の体調", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            // ヘッダー行（曜日）
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(24.dp))
                days.forEachIndexed { i, day ->
                    Text(
                        text = dayLabels[i],
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day == today) PrimaryTeal else TextHint,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // データ行（スロットごと）
            slots.forEachIndexed { slotIdx, slot ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slotLabels[slotIdx],
                        modifier = Modifier.width(24.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    days.forEach { day ->
                        val score = recordMap[day.toString()]?.get(slot)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (score != null) {
                                val scoreColor = when {
                                    score >= 8 -> SuccessGreen
                                    score >= 5 -> PrimaryTeal
                                    else       -> ErrorRed
                                }
                                Text(
                                    text = "$score",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scoreColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextHint.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                if (slotIdx < slots.size - 1) Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 今月の体調を週ごとの平均で表示するカード。
 * 各週の朝/昼/夕の平均スコアをコンパクトな行で表示する。
 */
@Composable
private fun MonthlyHealthCard(healthRecords: List<HealthRecord>) {
    val today      = LocalDate.now()
    val monthStart = today.withDayOfMonth(1)

    // 今月の記録のみ絞り込む
    val monthRecords = healthRecords.filter { it.date >= monthStart.toString() }

    // 今月に含まれる週（月曜始まり）を列挙する
    val weeks = mutableListOf<LocalDate>()
    var w = monthStart.with(DayOfWeek.MONDAY)
    if (w.isAfter(monthStart)) w = w.minusWeeks(1) // 月初が月曜でない場合の前週も含む
    while (!w.isAfter(today)) {
        if (w.plusDays(6) >= monthStart) weeks.add(w)
        w = w.plusWeeks(1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今月の体調平均", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            if (monthRecords.isEmpty()) {
                Text(
                    "まだ記録がありません",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            } else {
                // ヘッダー
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    listOf("朝", "昼", "夕").forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.width(44.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextHint
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                weeks.forEach { weekStart ->
                    val weekEnd = weekStart.plusDays(6)
                    val weekRecs = monthRecords.filter {
                        it.date >= weekStart.toString() && it.date <= weekEnd.toString()
                    }
                    if (weekRecs.isNotEmpty()) {
                        WeekAverageRow(weekStart, weekEnd, weekRecs, today)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/** 週の平均スコアを1行で表示するサブコンポーザブル */
@Composable
private fun WeekAverageRow(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    records: List<HealthRecord>,
    today: LocalDate
) {
    val displayEnd = if (weekEnd.isAfter(today)) today else weekEnd
    val label = "${weekStart.monthValue}/${weekStart.dayOfMonth}〜" +
                "${displayEnd.monthValue}/${displayEnd.dayOfMonth}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        listOf(TimeSlot.MORNING, TimeSlot.NOON, TimeSlot.EVENING).forEach { slot ->
            val avg = records.filter { it.timeSlot == slot }
                .map { it.score }
                .takeIf { it.isNotEmpty() }
                ?.average()
            val color = when {
                avg == null -> TextHint.copy(alpha = 0.4f)
                avg >= 8.0  -> SuccessGreen
                avg >= 5.0  -> PrimaryTeal
                else        -> ErrorRed
            }
            Text(
                text = if (avg != null) "%.1f".format(avg) else "-",
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = if (avg != null) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


@Composable
private fun StatusCard(progress: UserProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("現在の状況", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(label = "現在streak", value = "${progress.currentStreak}日", color = WarningAmber)
                StatItem(label = "最高streak", value = "${progress.maxStreak}日", color = SecondaryBlue)
                StatItem(label = "累計ポイント", value = "${progress.totalPoints}pt", color = PrimaryTeal)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun CharacterGrowthCard(totalPoints: Int) {
    val currentStage = CharacterStage.fromPoints(totalPoints)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("キャラクター成長", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CharacterStage.entries.forEach { stage ->
                    val isReached = totalPoints >= stage.minPoints
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stage.emoji,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isReached) TextPrimary else TextHint.copy(alpha = 0.3f)
                        )
                        if (stage == currentStage) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PrimaryTeal)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "現在：${currentStage.label}（${totalPoints}pt）",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            val next = CharacterStage.entries.firstOrNull { it.minPoints > totalPoints }
            if (next != null) {
                Text(
                    text = "次：${next.label} まで ${next.minPoints - totalPoints}pt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Text("最高ステージ達成！", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
            }
        }
    }
}

@Composable
private fun DailyRecordRow(record: DailyRecord) {
    val rate = if (record.totalTasks > 0)
        record.completedTasks.toFloat() / record.totalTasks else 0f
    val color = when {
        record.isGivenUp              -> GiveUpGray
        rate >= 1f                    -> SuccessGreen
        rate >= 0.5f                  -> PrimaryTeal
        else                          -> ErrorRed
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(record.date, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.width(110.dp))
            if (record.isGivenUp) {
                Text("おやすみ", style = MaterialTheme.typography.bodyMedium, color = GiveUpGray)
            } else {
                Text(
                    "${record.completedTasks}/${record.totalTasks}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp)
                )
                LinearProgressIndicator(
                    progress = { rate },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = DarkSurfaceVar
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("+${record.pointsEarned}pt", style = MaterialTheme.typography.labelSmall, color = PrimaryTeal)
            }
        }
    }
}
