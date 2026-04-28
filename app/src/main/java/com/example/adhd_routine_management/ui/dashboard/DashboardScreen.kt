package com.example.adhd_routine_management.ui.dashboard

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.database.entity.TimeSlot
import com.example.adhd_routine_management.data.database.entity.UserProgress
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ────────────────────────────────────────────
// データクラス
// ────────────────────────────────────────────

/** デバイスカレンダーから読み取った1件のイベント */
data class CalendarEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

// ────────────────────────────────────────────
// ViewModel
// ────────────────────────────────────────────

class DashboardViewModel(private val repository: TaskRepository) : ViewModel() {

    /** ユーザー進捗（無遅刻streak を含む） */
    val progress: StateFlow<UserProgress?> = repository.getProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 今日の体調記録（朝/昼/夕） */
    val todayHealth: StateFlow<List<HealthRecord>> =
        repository.getHealthRecordsForDate(LocalDate.now().toString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun factory(repository: TaskRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(repository) as T
        }
    }
}

// ────────────────────────────────────────────
// 画面
// ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: TaskRepository,
    onNavigateToSettings: () -> Unit
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(repository))
    val progress    by vm.progress.collectAsStateWithLifecycle()
    val todayHealth by vm.todayHealth.collectAsStateWithLifecycle()
    val context     = LocalContext.current

    // カレンダー権限の状態管理
    var calendarGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        calendarGranted = isGranted
    }

    // 権限が付与されたらカレンダーイベントを取得する
    LaunchedEffect(calendarGranted) {
        if (calendarGranted) {
            calendarEvents = withContext(Dispatchers.IO) {
                loadTodayCalendarEvents(context)
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("今日の概要", color = TextPrimary)
                        Text(
                            LocalDate.now().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "設定",
                            tint = TextSecondary
                        )
                    }
                },
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
            // 無遅刻streak カード
            item {
                progress?.let { p ->
                    PunctualStreakSummaryCard(
                        punctualStreak    = p.punctualStreak,
                        maxPunctualStreak = p.maxPunctualStreak
                    )
                }
            }

            // 今日の体調サマリーカード
            item {
                TodayHealthSummaryCard(healthRecords = todayHealth)
            }

            // カレンダー予定カード
            item {
                CalendarCard(
                    isGranted = calendarGranted,
                    events    = calendarEvents,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                )
            }
        }
    }
}

// ────────────────────────────────────────────
// コンポーザブル
// ────────────────────────────────────────────

/**
 * 無遅刻連続記録をダッシュボード用に大きく表示するカード。
 * HomeScreen の PunctualityCard は入力用、こちらは表示専用。
 */
@Composable
private fun PunctualStreakSummaryCard(
    punctualStreak: Int,
    maxPunctualStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "無遅刻連続記録",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (punctualStreak > 0) {
                Text(
                    text = "${punctualStreak}",
                    style = MaterialTheme.typography.displayLarge,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "日連続！🏃",
                    style = MaterialTheme.typography.titleLarge,
                    color = SuccessGreen
                )
            } else {
                Text(text = "🏃", fontSize = 48.sp)
                Text(
                    text = "記録を始めよう",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHint
                )
            }
            if (maxPunctualStreak > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "最高記録: ${maxPunctualStreak}日",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }
    }
}

/**
 * 今日の体調記録（朝/昼/夕）のサマリーカード。
 * 記録済みのスコアを表示し、未記録のスロットは「未記録」と表示する。
 */
@Composable
private fun TodayHealthSummaryCard(healthRecords: List<HealthRecord>) {
    val healthMap = healthRecords.associate { it.timeSlot to it.score }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("今日の体調", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TimeSlot.all.forEach { slot ->
                    val score = healthMap[slot]
                    val label = TimeSlot.label(slot)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextHint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (score != null) {
                            val scoreColor = when {
                                score >= 8 -> SuccessGreen
                                score >= 5 -> PrimaryTeal
                                else       -> ErrorRed
                            }
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.headlineLarge,
                                color = scoreColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "/ 10",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint
                            )
                        } else {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextHint.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "未記録",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextHint.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 今日のカレンダー予定を表示するカード。
 * 権限未付与の場合はリクエストボタンを表示する。
 */
@Composable
private fun CalendarCard(
    isGranted: Boolean,
    events: List<CalendarEvent>,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("今日の予定", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!isGranted) {
                // 権限がない場合：説明 + リクエストボタン
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "カレンダーへのアクセスを許可すると\n今日の予定がここに表示されます",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
                ) {
                    Text("カレンダーへのアクセスを許可する")
                }
            } else if (events.isEmpty()) {
                // 予定なし
                Text(
                    "今日の予定はありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHint
                )
            } else {
                // 予定一覧
                events.forEach { event ->
                    CalendarEventRow(event)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/** カレンダーイベント1件の表示行 */
@Composable
private fun CalendarEventRow(event: CalendarEvent) {
    val timeText = if (event.allDay) {
        "終日"
    } else {
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = java.time.Instant.ofEpochMilli(event.startMillis)
            .atZone(zone).toLocalTime().format(formatter)
        val endTime = java.time.Instant.ofEpochMilli(event.endMillis)
            .atZone(zone).toLocalTime().format(formatter)
        "$startTime〜$endTime"
    }

    Row(verticalAlignment = Alignment.Top) {
        // 時刻バッジ
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryTeal,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

// ────────────────────────────────────────────
// カレンダー読み取りユーティリティ
// ────────────────────────────────────────────

/**
 * デバイス内カレンダーから今日のイベントを取得する。
 * CalendarContract.Instances を使うことで繰り返しイベントも正しく展開される。
 * このメソッドは ContentResolver をブロックするので IO スレッドで呼ぶこと。
 *
 * READ_CALENDAR 権限が必要（呼び出し前に確認済みであること）。
 */
@Suppress("MissingPermission")
fun loadTodayCalendarEvents(context: Context): List<CalendarEvent> {
    val today   = LocalDate.now()
    val zone    = ZoneId.systemDefault()
    val startMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMs   = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    // Instances URI に期間を埋め込む（繰り返しイベントを展開するため）
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
        ContentUris.appendId(it, startMs)
        ContentUris.appendId(it, endMs)
        it.build()
    }

    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY
    )
    // Instances は有効なイベントのみ返すため selection は不要

    val events = mutableListOf<CalendarEvent>()
    val cursor = context.contentResolver.query(
        uri, projection, null, null,
        CalendarContract.Instances.BEGIN + " ASC"
    )
    cursor?.use {
        while (it.moveToNext()) {
            val title = it.getString(0) ?: continue
            val start = it.getLong(1)
            val end   = it.getLong(2)
            val allDay = it.getInt(3) == 1
            events.add(CalendarEvent(title, start, end, allDay))
        }
    }
    return events
}
