package com.example.adhd_routine_management.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.adhd_routine_management.data.database.entity.CompletionStatus
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import com.example.adhd_routine_management.data.database.entity.TaskCompletion
import com.example.adhd_routine_management.data.database.entity.UserProgress
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.domain.model.CharacterDialogue
import com.example.adhd_routine_management.domain.model.CharacterStage
import com.example.adhd_routine_management.domain.model.DialogueType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/** ホーム画面の UI 状態をまとめたデータクラス */
data class HomeUiState(
    val todayTasks: List<RoutineTask> = emptyList(),
    val completions: List<TaskCompletion> = emptyList(),
    val progress: UserProgress = UserProgress(),
    val characterStage: CharacterStage = CharacterStage.EGG,
    val dialogue: String = "……",
    val todayDate: String = "",
    val isGivenUp: Boolean = false,
    val showStreakBanner: Boolean = false,
    val streakMilestone: Int = 0,
    /** 今日の体調記録（朝/昼/夕）。スロットがキー、スコアが値 */
    val healthRecords: Map<String, Int> = emptyMap()
) {
    val completedCount: Int get() = completions.count { it.status == CompletionStatus.COMPLETED }
    val totalCount: Int get() = completions.size
    val allDone: Boolean get() = totalCount > 0 && completedCount == totalCount
}

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    private val _uiState = MutableStateFlow(HomeUiState(todayDate = today))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 今日のタスクレコードを初期化（まだなければ作成）
        viewModelScope.launch {
            repository.ensureTodayRecords(today)
        }
        // タスク・完了記録・進捗・体調記録を監視して UI を更新
        viewModelScope.launch {
            combine(
                repository.getActiveTasks(),
                repository.getCompletionsForDate(today),
                repository.getProgress().filterNotNull(),
                repository.getRecordForDate(today),
                repository.getHealthRecordsForDate(today)
            ) { tasks, completions, progress, record, healthRecords ->
                val stage = CharacterStage.fromPoints(progress.totalPoints)
                val givenUp = record?.isGivenUp ?: false
                val dialogueType = when {
                    givenUp -> DialogueType.GIVE_UP
                    completions.all { it.status == CompletionStatus.COMPLETED } && completions.isNotEmpty() ->
                        DialogueType.COMPLETED
                    completions.any { it.status == CompletionStatus.PENDING } -> DialogueType.NORMAL
                    else -> DialogueType.NORMAL
                }
                HomeUiState(
                    todayTasks = tasks,
                    completions = completions,
                    progress = progress,
                    characterStage = stage,
                    dialogue = CharacterDialogue.get(stage, dialogueType),
                    todayDate = today,
                    isGivenUp = givenUp,
                    healthRecords = healthRecords.associate { it.timeSlot to it.score }
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // 初回のみ UserProgress を作成
        viewModelScope.launch {
            val p = repository.getProgressOnce()
            if (p.lastActiveDate.isEmpty()) {
                repository.saveProgress(UserProgress())
            }
        }
    }

    /** タスクを完了にする */
    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isGivenUp) return@launch

            repository.markCompleted(taskId, today)

            // 完了後にポイントを付与
            val currentCompletions = _uiState.value.completions
                .count { it.status == CompletionStatus.COMPLETED } + 1
            val total = _uiState.value.totalCount

            // タスク1つ完了: +10pt、全完了ボーナス: +50pt
            val points = 10 + if (currentCompletions == total) 50 else 0

            repository.updateProgressAfterCompletion(today, currentCompletions, total, points)

            // streak 更新後の値を取得してマイルストーン確認
            val updatedProgress = repository.getProgressOnce()
            val newStreak = updatedProgress.currentStreak
            val milestone = when {
                newStreak > 0 && newStreak % 100 == 0 -> newStreak
                newStreak > 0 && newStreak % 30  == 0 -> newStreak
                newStreak > 0 && newStreak % 7   == 0 -> newStreak
                else -> 0
            }
            if (milestone > 0) {
                _uiState.update { it.copy(showStreakBanner = true, streakMilestone = milestone) }
            }
        }
    }

    /** 今日はあきらめる（全タスク） */
    fun giveUpToday() {
        viewModelScope.launch {
            repository.giveUpToday(today)
            repository.updateProgressForGiveUp(today)
        }
    }

    /** 特定のタスクだけあきらめる */
    fun giveUpTask(taskId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isGivenUp) return@launch
            repository.giveUpTask(taskId, today)
        }
    }

    /** 体調スコアを保存する（同じ日・スロットなら上書き） */
    fun saveHealthRecord(timeSlot: String, score: Int) {
        viewModelScope.launch {
            repository.saveHealthRecord(
                HealthRecord(date = today, timeSlot = timeSlot, score = score)
            )
        }
    }

    fun dismissStreakBanner() {
        _uiState.update { it.copy(showStreakBanner = false) }
    }

    companion object {
        fun factory(repository: TaskRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(repository) as T
        }
    }
}
