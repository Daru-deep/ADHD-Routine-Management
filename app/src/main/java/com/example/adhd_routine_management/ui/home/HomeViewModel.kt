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
    val healthRecords: Map<String, Int> = emptyMap(),
    /** 今日の無遅刻ステータス（"on_time" / "late" / "no_appointment" / ""） */
    val punctualityStatus: String = "",
    /** 現在の無遅刻連続記録 */
    val punctualStreak: Int = 0,
    /** 過去最高の無遅刻連続記録 */
    val maxPunctualStreak: Int = 0
) {
    val completedCount: Int get() = completions.count { it.status == CompletionStatus.COMPLETED }
    val totalCount: Int get() = completions.size
    val allDone: Boolean get() = totalCount > 0 && completedCount == totalCount
}

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    // today を StateFlow で管理することで、日付が変わった際に DB 監視を再接続できる
    private val _todayDate = MutableStateFlow(LocalDate.now().toString())

    private val _uiState = MutableStateFlow(HomeUiState(todayDate = _todayDate.value))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 今日のタスクレコードを初期化（まだなければ作成）
        viewModelScope.launch {
            repository.ensureTodayRecords(_todayDate.value)
        }
        // 日付が変わるたびに DB の監視を再接続して UI を更新する
        // flatMapLatest: _todayDate が変わると古い combine をキャンセルして新しい監視を開始する
        viewModelScope.launch {
            _todayDate.flatMapLatest { currentDate ->
                combine(
                    repository.getActiveTasks(),
                    repository.getCompletionsForDate(currentDate),
                    repository.getProgress().filterNotNull(),
                    repository.getRecordForDate(currentDate),
                    repository.getHealthRecordsForDate(currentDate)
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
                        todayDate = currentDate,
                        isGivenUp = givenUp,
                        healthRecords = healthRecords.associate { it.timeSlot to it.score },
                        punctualityStatus = record?.punctualityStatus ?: "",
                        punctualStreak    = progress.punctualStreak,
                        maxPunctualStreak = progress.maxPunctualStreak
                    )
                }
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

    /**
     * 画面表示時に日付をチェックし、日付が変わっていたら DB 監視を切り替える。
     * HomeScreen の LaunchedEffect から呼ぶ。
     */
    fun refreshDate() {
        val newDate = LocalDate.now().toString()
        if (newDate != _todayDate.value) {
            _todayDate.value = newDate
            viewModelScope.launch {
                repository.ensureTodayRecords(newDate)
            }
        }
    }

    /** タスクを完了にする */
    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isGivenUp) return@launch
            val today = _todayDate.value

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
            val today = _todayDate.value
            repository.giveUpToday(today)
            repository.updateProgressForGiveUp(today)
        }
    }

    /** 特定のタスクだけあきらめる */
    fun giveUpTask(taskId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isGivenUp) return@launch
            repository.giveUpTask(taskId, _todayDate.value)
        }
    }

    /** 体調スコアを保存する（同じ日・スロットなら上書き） */
    fun saveHealthRecord(timeSlot: String, score: Int) {
        viewModelScope.launch {
            repository.saveHealthRecord(
                HealthRecord(date = _todayDate.value, timeSlot = timeSlot, score = score)
            )
        }
    }

    /** 無遅刻ステータスを保存する（"on_time" / "late" / "no_appointment"） */
    fun savePunctuality(status: String) {
        viewModelScope.launch {
            repository.savePunctualityStatus(_todayDate.value, status)
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
