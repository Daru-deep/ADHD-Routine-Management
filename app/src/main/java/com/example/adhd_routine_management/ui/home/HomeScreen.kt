package com.example.adhd_routine_management.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.CompletionStatus
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import com.example.adhd_routine_management.data.database.entity.TaskCompletion
import com.example.adhd_routine_management.data.database.entity.TimeSlot
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.domain.model.CharacterStage
import com.example.adhd_routine_management.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun HomeScreen(
    repository: TaskRepository,
    weeklyGoalRepository: WeeklyGoalRepository
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showGiveUpDialog by remember { mutableStateOf(false) }
    var showMondayPrompt by remember { mutableStateOf(false) }

    // 月曜日かつ今週の目標が未設定の場合にプロンプトを表示
    // LaunchedEffect はすでにコルーチン内で動くので、直接 suspend 関数を呼べる
    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        if (today.dayOfWeek == DayOfWeek.MONDAY) {
            val goal = weeklyGoalRepository.getGoalForWeekOnce(today.toString())
            if (goal == null) {
                showMondayPrompt = true
            }
        }
    }

    // 月曜日の目標設定プロンプト
    if (showMondayPrompt) {
        MondayGoalPromptDialog(
            onConfirm = { showMondayPrompt = false },
            onDismiss = { showMondayPrompt = false }
        )
    }

    // あきらめる確認ダイアログ
    if (showGiveUpDialog) {
        GiveUpDialog(
            onConfirm = {
                vm.giveUpToday()
                showGiveUpDialog = false
            },
            onDismiss = { showGiveUpDialog = false }
        )
    }

    // Streak マイルストーンバナー
    if (state.showStreakBanner) {
        StreakBannerDialog(
            streak = state.streakMilestone,
            onDismiss = { vm.dismissStreakBanner() }
        )
    }

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ヘッダー（streak + 日付）
            item {
                HeaderSection(
                    streak = state.progress.currentStreak,
                    date = state.todayDate
                )
            }

            // キャラクターエリア
            item {
                CharacterSection(
                    stage = state.characterStage,
                    dialogue = state.dialogue,
                    totalPoints = state.progress.totalPoints
                )
            }

            // 進捗バー
            item {
                ProgressSection(
                    completed = state.completedCount,
                    total = state.totalCount,
                    isGivenUp = state.isGivenUp
                )
            }

            // 体調記録カード
            item {
                HealthRecordCard(
                    healthRecords = state.healthRecords,
                    onRecordScore = { slot, score -> vm.saveHealthRecord(slot, score) }
                )
            }

            // あきらめるボタン
            if (!state.isGivenUp && state.totalCount > 0) {
                item {
                    GiveUpButton(onClick = { showGiveUpDialog = true })
                }
            }

            // タスクリスト
            if (state.todayTasks.isEmpty()) {
                item {
                    EmptyTasksHint()
                }
            } else {
                items(state.todayTasks, key = { it.id }) { task ->
                    val completion = state.completions.find { it.taskId == task.id }
                    TaskRow(
                        task = task,
                        completion = completion,
                        isGivenUp = state.isGivenUp,
                        onComplete = { vm.completeTask(task.id) },
                        onGiveUp = { vm.giveUpTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(streak: Int, date: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            if (streak > 0) {
                Text(
                    text = "${streak}日連続達成中！",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "今日からスタート！",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
        if (streak > 0) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(WarningAmber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔥", fontSize = 24.sp)
            }
        }
    }
}

@Composable
private fun CharacterSection(
    stage: CharacterStage,
    dialogue: String,
    totalPoints: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // キャラクター絵文字（大きく表示）
            Text(text = stage.emoji, fontSize = 72.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stage.label,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryTeal
            )
            Spacer(modifier = Modifier.height(12.dp))
            // セリフ吹き出し
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVar)
                    .padding(12.dp)
            ) {
                Text(
                    text = "「$dialogue」",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "累計 $totalPoints pt",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            // 次のステージまでのバー
            val nextPoints = CharacterStage.pointsToNextStage(totalPoints)
            if (nextPoints != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val next = CharacterStage.entries
                    .firstOrNull { it.minPoints > totalPoints }
                val progress = if (next != null) {
                    val prev = CharacterStage.entries
                        .lastOrNull { it.minPoints <= totalPoints }?.minPoints ?: 0
                    (totalPoints - prev).toFloat() / (next.minPoints - prev)
                } else 1f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PrimaryTeal,
                    trackColor = DarkSurfaceVar
                )
                Text(
                    text = "次のステージまで ${nextPoints}pt",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(completed: Int, total: Int, isGivenUp: Boolean) {
    if (total == 0) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isGivenUp) "今日はおやすみ" else "今日の進捗",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "$completed / $total",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (completed == total) SuccessGreen else PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) completed.toFloat() / total else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isGivenUp) GiveUpGray else if (completed == total) SuccessGreen else PrimaryTeal,
                trackColor = DarkSurfaceVar
            )
        }
    }
}

@Composable
private fun GiveUpButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GiveUpGray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GiveUpGray)
    ) {
        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("今日はあきらめる？")
    }
}

@Composable
private fun TaskRow(
    task: RoutineTask,
    completion: TaskCompletion?,
    isGivenUp: Boolean,
    onComplete: () -> Unit,
    onGiveUp: () -> Unit
) {
    val isCompleted = completion?.status == CompletionStatus.COMPLETED
    val isSkipped   = completion?.status == CompletionStatus.SKIPPED
    val isPending   = completion?.status == CompletionStatus.PENDING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> SuccessGreen.copy(alpha = 0.1f)
                isSkipped   -> GiveUpGray.copy(alpha = 0.1f)
                else        -> DarkSurface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // チェックボタン
            IconButton(
                onClick = { if (!isCompleted && !isGivenUp && !isSkipped) onComplete() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> SuccessGreen.copy(alpha = 0.2f)
                            else        -> DarkSurfaceVar
                        }
                    )
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "完了",
                    tint = when {
                        isCompleted -> SuccessGreen
                        isSkipped   -> GiveUpGray
                        else        -> TextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSkipped) TextHint else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = "%02d:%02d".format(task.hourOfDay, task.minute),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            // タスク単体の「あきらめる」ボタン（PENDING状態かつ全体あきらめ前のみ表示）
            if (isPending && !isGivenUp) {
                TextButton(
                    onClick = onGiveUp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "あきらめ",
                        style = MaterialTheme.typography.labelSmall,
                        color = GiveUpGray
                    )
                }
            }
            if (isSkipped) {
                Text("スキップ", style = MaterialTheme.typography.labelSmall, color = GiveUpGray)
            }
        }
    }
}

/**
 * 今日の体調記録カード。
 * 朝・昼・夕の3スロットごとに1〜10の数字ボタンを並べる。
 * 5が基準（ノーマル）で、記録済みのスコアはハイライト表示する。
 */
@Composable
private fun HealthRecordCard(
    healthRecords: Map<String, Int>,
    onRecordScore: (slot: String, score: Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "今日の体調",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            TimeSlot.all.forEach { slot ->
                HealthSlotRow(
                    label       = TimeSlot.label(slot),
                    currentScore = healthRecords[slot],
                    onSelect    = { score -> onRecordScore(slot, score) }
                )
                if (slot != TimeSlot.EVENING) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 1スロット（朝/昼/夕）の行。
 * ラベル + 1〜10の数字チップを横並びで表示する。
 * 選択済みは PrimaryTeal でハイライト、5はデフォルト基準色。
 */
@Composable
private fun HealthSlotRow(
    label: String,
    currentScore: Int?,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.width(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // スクロール不要にするため 1〜10 を横に均等配置
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..10).forEach { score ->
                val isSelected = currentScore == score
                val isBase     = score == 5
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isSelected -> PrimaryTeal
                                isBase     -> DarkSurfaceVar
                                else       -> DarkBackground
                            }
                        )
                        .then(
                            Modifier.clickable { onSelect(score) }
                        )
                ) {
                    Text(
                        text  = score.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isSelected -> DarkBackground
                            isBase     -> TextSecondary
                            else       -> TextHint
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("タスクがまだありません", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("下のタブから「タスク」を開いて追加しましょう", color = TextHint, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
private fun GiveUpDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("今日はあきらめる？", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "今日のタスクをスキップします。\nstreakは維持されますが、ポイントは獲得できません。",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("あきらめる", color = GiveUpGray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("もう少しやってみる", color = PrimaryTeal)
            }
        }
    )
}

@Composable
private fun MondayGoalPromptDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("今週の目標を設定しませんか？", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "月曜日は新しいスタートの日！\n今週の短期目標を設定して、一週間を充実させましょう。",
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("目標を設定する", color = PrimaryTeal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("後で", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun StreakBannerDialog(streak: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("${streak}日連続達成！！", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥".repeat(3), fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("すごい！継続は力なり！", color = TextSecondary)
                Text("+100pt ボーナス獲得！", color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("やったー！", color = PrimaryTeal)
            }
        }
    )
}
