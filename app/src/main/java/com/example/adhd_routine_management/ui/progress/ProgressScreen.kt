package com.example.adhd_routine_management.ui.progress

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.backup.BackupManager
import com.example.adhd_routine_management.data.database.entity.DailyRecord
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.database.entity.UserProgress
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalWithTasks
import com.example.adhd_routine_management.domain.model.CharacterStage
import com.example.adhd_routine_management.export.WeeklySummaryExporter
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// ────────────────────────────────────────────
// ViewModel
// ────────────────────────────────────────────

/** 週次サマリー用のデータをまとめたデータクラス */
data class WeeklySummaryData(
    val weekLabel: String,
    val dailyRecords: List<DailyRecord>,
    val weeklyGoal: WeeklyGoalWithTasks?,
    val healthRecords: List<HealthRecord>
)

class ProgressViewModel(
    private val repository: TaskRepository,
    private val weeklyGoalRepository: WeeklyGoalRepository
) : ViewModel() {

    val progress: StateFlow<UserProgress?> = repository.getProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecords: StateFlow<List<DailyRecord>> = repository.getRecentRecords(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 今週のまとめデータ（月〜日）を取得する。
     * suspend 関数として呼び出し側がコルーチンで使う。
     */
    suspend fun loadWeeklySummary(): WeeklySummaryData {
        val today = LocalDate.now()
        // 今週の月曜日を起点にする（DayOfWeek.MONDAY = 1）
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd   = weekStart.plusDays(6)
        val from = weekStart.toString()
        val to   = weekEnd.toString()

        val dailyRecords  = repository.getRecentRecords(7)
            .first()
            .filter { it.date >= from && it.date <= to }
        val goal = weeklyGoalRepository.getGoalForWeekOnce(weekStart.toString())
        val weeklyGoal: WeeklyGoalWithTasks? = if (goal != null) {
            val tasks = weeklyGoalRepository.getTasksForGoal(goal.id).first()
            WeeklyGoalWithTasks(goal, tasks)
        } else null
        val healthRecords = repository.getHealthRecordsForRange(from, to)
        val weekLabel     = "${weekStart.monthValue}/${weekStart.dayOfMonth}（月）〜" +
                            "${weekEnd.monthValue}/${weekEnd.dayOfMonth}（日）"

        return WeeklySummaryData(weekLabel, dailyRecords, weeklyGoal, healthRecords)
    }

    companion object {
        fun factory(
            repository: TaskRepository,
            weeklyGoalRepository: WeeklyGoalRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(repository, weeklyGoalRepository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: TaskRepository,
    weeklyGoalRepository: WeeklyGoalRepository
) {
    val vm: ProgressViewModel = viewModel(
        factory = ProgressViewModel.factory(repository, weeklyGoalRepository)
    )
    val progress by vm.progress.collectAsStateWithLifecycle()
    val records  by vm.recentRecords.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    // バックアップ結果のスナックバー表示用
    val snackbarHostState = remember { SnackbarHostState() }

    // 週次サマリー：保存形式選択ダイアログの表示状態
    var showSummaryDialog by remember { mutableStateOf(false) }
    // 週次サマリーデータ（ダイアログ表示時にロード）
    var summaryData by remember { mutableStateOf<WeeklySummaryData?>(null) }

    // 画像保存ランチャー（PNG）
    val imageSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val data = summaryData ?: return@launch
                val ok = WeeklySummaryExporter.exportImage(
                    context, uri, data.weekLabel,
                    data.dailyRecords, data.weeklyGoal, data.healthRecords
                )
                snackbarHostState.showSnackbar(
                    if (ok) "まとめ画像を保存しました" else "画像の保存に失敗しました"
                )
            }
        }
    }

    // CSV保存ランチャー
    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val data = summaryData ?: return@launch
                val ok = WeeklySummaryExporter.exportCsv(
                    context, uri,
                    data.dailyRecords, data.weeklyGoal, data.healthRecords
                )
                snackbarHostState.showSnackbar(
                    if (ok) "CSVを保存しました" else "CSVの保存に失敗しました"
                )
            }
        }
    }

    // 週次サマリー形式選択ダイアログ
    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            containerColor = DarkSurface,
            title = { Text("今週のまとめを保存", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("保存する形式を選んでください。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSummaryDialog = false
                    val fileName = "weekly_summary_${LocalDate.now()}.png"
                    imageSaveLauncher.launch(fileName)
                }) {
                    Text("画像（PNG）", color = PrimaryTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSummaryDialog = false
                    val fileName = "weekly_summary_${LocalDate.now()}.csv"
                    csvSaveLauncher.launch(fileName)
                }) {
                    Text("CSV", color = SecondaryBlue)
                }
            }
        )
    }

    // エクスポート：ファイル保存先をユーザーに選ばせるランチャー
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = BackupManager.exportToUri(context, uri, repository, weeklyGoalRepository)
                snackbarHostState.showSnackbar(
                    if (ok) "バックアップを保存しました" else "バックアップに失敗しました"
                )
            }
        }
    }

    // インポート：ファイルをユーザーに選ばせるランチャー
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = BackupManager.importFromUri(context, uri, repository, weeklyGoalRepository)
                snackbarHostState.showSnackbar(
                    if (ok) "データを復元しました" else "復元に失敗しました（ファイル形式を確認してください）"
                )
            }
        }
    }

    // リストア確認ダイアログ
    var showRestoreConfirm by remember { mutableStateOf(false) }
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = DarkSurface,
            title = { Text("データを復元しますか？", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("現在のデータはすべてバックアップファイルの内容で上書きされます。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text("復元する", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("キャンセル", color = PrimaryTeal)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // 週次サマリーカード
            item {
                WeeklySummaryCard(
                    onGenerateSummary = {
                        scope.launch {
                            summaryData = vm.loadWeeklySummary()
                            showSummaryDialog = true
                        }
                    }
                )
            }

            // バックアップカード
            item {
                BackupCard(
                    onExport = {
                        val fileName = "adhd_backup_${LocalDate.now()}.json"
                        exportLauncher.launch(fileName)
                    },
                    onImport = { showRestoreConfirm = true }
                )
            }

            // 直近30日の記録
            if (records.isNotEmpty()) {
                item {
                    Text(
                        "直近の記録",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(records) { record ->
                    DailyRecordRow(record)
                }
            }
        }
    }
}

/**
 * 今週のまとめを画像またはCSVで保存するカード。
 * ボタンを押すと形式選択ダイアログが開く。
 */
@Composable
private fun WeeklySummaryCard(onGenerateSummary: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "今週のまとめ",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "達成タスク・週間目標・体調グラフを画像またはCSVで保存できます。",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGenerateSummary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("まとめを生成・保存する", color = DarkBackground)
            }
        }
    }
}

@Composable
private fun BackupCard(onExport: () -> Unit, onImport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("データ管理", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // バックアップボタン
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("バックアップ")
                }
                // リストアボタン
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryBlue)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("リストア")
                }
            }
            Text(
                text = "JSON形式でエクスポート。Google DriveやFilesに保存できます。",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                modifier = Modifier.padding(top = 6.dp)
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
