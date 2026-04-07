package com.example.adhd_routine_management.ui.weeklygoal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.WeeklyGoal
import com.example.adhd_routine_management.data.database.entity.WeeklyGoalTask
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalWithTasks
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ========== ViewModel ==========

data class WeeklyGoalUiState(
    val currentGoal: WeeklyGoal? = null,
    val currentTasks: List<WeeklyGoalTask> = emptyList(),
    val pastGoals: List<WeeklyGoal> = emptyList(),
    val pastGoalTasksMap: Map<Int, List<WeeklyGoalTask>> = emptyMap(),
    val weekStart: String = ""
) {
    val completedCount: Int get() = currentTasks.count { it.isCompleted }
    val achievementRate: Float get() =
        if (currentTasks.isEmpty()) 0f else completedCount.toFloat() / currentTasks.size
}

class WeeklyGoalViewModel(private val repository: WeeklyGoalRepository) : ViewModel() {

    // 今週の月曜日の日付文字列
    val weekStart: String = run {
        val today = LocalDate.now()
        today.with(DayOfWeek.MONDAY).toString()
    }

    private val _uiState = MutableStateFlow(WeeklyGoalUiState(weekStart = weekStart))
    val uiState: StateFlow<WeeklyGoalUiState> = _uiState.asStateFlow()

    init {
        // 今週の目標とタスクを監視。
        // flatMapLatest を使うことで、目標が変わったときに内側のタスク Flow も自動的に切り替わる。
        viewModelScope.launch {
            repository.getGoalForWeek(weekStart)
                .flatMapLatest { goal ->
                    if (goal != null) {
                        repository.getTasksForGoal(goal.id).map { tasks -> goal to tasks }
                    } else {
                        flowOf(null to emptyList())
                    }
                }
                .collect { (goal, tasks) ->
                    _uiState.update { it.copy(currentGoal = goal, currentTasks = tasks) }
                }
        }

        // 過去の目標をすべて監視（今週を除く）
        viewModelScope.launch {
            repository.getAllGoals().collect { goals ->
                val pastGoals = goals.filter { it.weekStart != weekStart }
                _uiState.update { it.copy(pastGoals = pastGoals) }

                // 過去の各目標のタスクを一度だけ取得（履歴は変化しないため first() で十分）
                val tasksMap = mutableMapOf<Int, List<WeeklyGoalTask>>()
                pastGoals.forEach { goal ->
                    tasksMap[goal.id] = repository.getTasksForGoal(goal.id).first()
                }
                _uiState.update { it.copy(pastGoalTasksMap = tasksMap) }
            }
        }
    }

    /** タスクの達成状態をトグルする */
    fun toggleTask(task: WeeklyGoalTask) {
        viewModelScope.launch {
            repository.toggleTask(task)
        }
    }

    companion object {
        fun factory(repository: WeeklyGoalRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WeeklyGoalViewModel(repository) as T
        }
    }
}

// ========== Screen ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalScreen(
    repository: WeeklyGoalRepository,
    onNavigateToSetup: () -> Unit
) {
    val vm: WeeklyGoalViewModel = viewModel(factory = WeeklyGoalViewModel.factory(repository))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("週間目標", color = TextPrimary) },
                actions = {
                    // 今週の目標がまだない場合は追加ボタンを表示
                    if (state.currentGoal == null) {
                        IconButton(onClick = onNavigateToSetup) {
                            Icon(Icons.Default.Add, contentDescription = "目標を設定", tint = PrimaryTeal)
                        }
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
            // === 今週の目標セクション ===
            item {
                Text(
                    text = "今週の目標",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.currentGoal == null) {
                // 目標が未設定の場合
                item {
                    NoGoalCard(onSetupClick = onNavigateToSetup)
                }
            } else {
                // 今週の目標カード
                item {
                    CurrentGoalCard(
                        goal = state.currentGoal!!,
                        tasks = state.currentTasks,
                        completedCount = state.completedCount,
                        achievementRate = state.achievementRate,
                        onTaskToggle = { vm.toggleTask(it) }
                    )
                }
            }

            // === 過去の目標セクション ===
            if (state.pastGoals.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "過去の目標",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.pastGoals, key = { it.id }) { goal ->
                    val tasks = state.pastGoalTasksMap[goal.id] ?: emptyList()
                    PastGoalCard(goal = goal, tasks = tasks)
                }
            }
        }
    }
}

// ========== 今週の目標カード ==========

@Composable
private fun CurrentGoalCard(
    goal: WeeklyGoal,
    tasks: List<WeeklyGoalTask>,
    completedCount: Int,
    achievementRate: Float,
    onTaskToggle: (WeeklyGoalTask) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 目標タイトル
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryTeal,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatWeekRange(goal.weekStart),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 達成度バー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "達成度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "$completedCount / ${tasks.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (completedCount == tasks.size && tasks.isNotEmpty()) SuccessGreen else PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { achievementRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (achievementRate >= 1f) SuccessGreen else PrimaryTeal,
                trackColor = DarkSurfaceVar
            )

            Spacer(modifier = Modifier.height(12.dp))

            // タスクリスト（チェックボックス付き）
            tasks.forEach { task ->
                GoalTaskRow(task = task, onToggle = { onTaskToggle(task) })
                if (task != tasks.last()) {
                    HorizontalDivider(color = DarkSurfaceVar, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun GoalTaskRow(task: WeeklyGoalTask, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle
                              else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "達成済み" else "未達成",
                tint = if (task.isCompleted) SuccessGreen else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isCompleted) TextHint else TextPrimary
        )
    }
}

// ========== 目標未設定カード ==========

@Composable
private fun NoGoalCard(onSetupClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📋", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "今週の目標はまだ設定されていません",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSetupClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("今週の目標を設定する")
            }
        }
    }
}

// ========== 過去の目標カード ==========

@Composable
private fun PastGoalCard(goal: WeeklyGoal, tasks: List<WeeklyGoalTask>) {
    val completedCount = tasks.count { it.isCompleted }
    val achievementRate = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatWeekRange(goal.weekStart),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                // 達成率バッジ
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                achievementRate >= 1f -> SuccessGreen.copy(alpha = 0.2f)
                                achievementRate >= 0.6f -> PrimaryTeal.copy(alpha = 0.2f)
                                else -> GiveUpGray.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$completedCount/${tasks.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            achievementRate >= 1f -> SuccessGreen
                            achievementRate >= 0.6f -> PrimaryTeal
                            else -> GiveUpGray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { achievementRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    achievementRate >= 1f -> SuccessGreen
                    achievementRate >= 0.6f -> PrimaryTeal
                    else -> GiveUpGray
                },
                trackColor = DarkSurfaceVar
            )
        }
    }
}

// ========== ユーティリティ ==========

/** "2024-04-01" → "4/1（月）〜4/7（日）" 形式に変換 */
private fun formatWeekRange(weekStart: String): String {
    return try {
        val monday = LocalDate.parse(weekStart)
        val sunday = monday.plusDays(6)
        val fmt = DateTimeFormatter.ofPattern("M/d")
        "${monday.format(fmt)}（月）〜${sunday.format(fmt)}（日）"
    } catch (e: Exception) {
        weekStart
    }
}
