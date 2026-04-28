package com.example.adhd_routine_management.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.backup.BackupManager
import com.example.adhd_routine_management.data.database.entity.DailyRecord
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalWithTasks
import com.example.adhd_routine_management.export.WeeklySummaryExporter
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// ────────────────────────────────────────────
// データクラス
// ────────────────────────────────────────────

/** 週次サマリーエクスポート用のデータをまとめたデータクラス */
data class WeeklySummaryData(
    val weekLabel: String,
    val dailyRecords: List<DailyRecord>,
    val weeklyGoal: WeeklyGoalWithTasks?,
    val healthRecords: List<HealthRecord>
)

// ────────────────────────────────────────────
// ViewModel
// ────────────────────────────────────────────

class SettingsViewModel(
    private val repository: TaskRepository,
    private val weeklyGoalRepository: WeeklyGoalRepository
) : ViewModel() {

    /**
     * 今週（月〜日）の集計データを取得する。
     * 週次サマリーの画像・CSV エクスポートに使用する。
     */
    suspend fun loadWeeklySummary(): WeeklySummaryData {
        val today     = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd   = weekStart.plusDays(6)
        val from = weekStart.toString()
        val to   = weekEnd.toString()

        val dailyRecords = repository.getRecentRecords(7)
            .first()
            .filter { it.date >= from && it.date <= to }
        val goal = weeklyGoalRepository.getGoalForWeekOnce(weekStart.toString())
        val weeklyGoal: WeeklyGoalWithTasks? = if (goal != null) {
            val tasks = weeklyGoalRepository.getTasksForGoal(goal.id).first()
            WeeklyGoalWithTasks(goal, tasks)
        } else null
        val healthRecords = repository.getHealthRecordsForRange(from, to)
        val weekLabel = "${weekStart.monthValue}/${weekStart.dayOfMonth}（月）〜" +
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
                SettingsViewModel(repository, weeklyGoalRepository) as T
        }
    }
}

// ────────────────────────────────────────────
// 画面
// ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: TaskRepository,
    weeklyGoalRepository: WeeklyGoalRepository,
    onBack: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(repository, weeklyGoalRepository)
    )
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 週次サマリー関連の状態
    var showSummaryDialog by remember { mutableStateOf(false) }
    var summaryData       by remember { mutableStateOf<WeeklySummaryData?>(null) }

    // PNG 保存ランチャー
    val imageSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
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

    // CSV 保存ランチャー
    val csvSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
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

    // 形式選択ダイアログ
    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            containerColor = DarkSurface,
            title = { Text("今週のまとめを保存", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("保存する形式を選んでください。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSummaryDialog = false
                    imageSaveLauncher.launch("weekly_summary_${LocalDate.now()}.png")
                }) { Text("画像（PNG）", color = PrimaryTeal) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSummaryDialog = false
                    csvSaveLauncher.launch("weekly_summary_${LocalDate.now()}.csv")
                }) { Text("CSV", color = SecondaryBlue) }
            }
        )
    }

    // バックアップ：エクスポートランチャー
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
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

    // バックアップ：インポートランチャー
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
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
            text  = { Text("現在のデータはすべてバックアップファイルの内容で上書きされます。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("復元する", color = ErrorRed) }
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
                title = { Text("設定", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = TextPrimary)
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
            // 週次サマリーカード
            item {
                SettingsWeeklySummaryCard(
                    onGenerateSummary = {
                        scope.launch {
                            summaryData = vm.loadWeeklySummary()
                            showSummaryDialog = true
                        }
                    }
                )
            }

            // データ管理カード
            item {
                SettingsBackupCard(
                    onExport = { exportLauncher.launch("adhd_backup_${LocalDate.now()}.json") },
                    onImport = { showRestoreConfirm = true }
                )
            }
        }
    }
}

// ────────────────────────────────────────────
// コンポーザブル
// ────────────────────────────────────────────

@Composable
private fun SettingsWeeklySummaryCard(onGenerateSummary: () -> Unit) {
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
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("まとめを生成・保存する", color = DarkBackground)
            }
        }
    }
}

@Composable
private fun SettingsBackupCard(onExport: () -> Unit, onImport: () -> Unit) {
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
