package com.example.adhd_routine_management.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel をここに定義（ファイル分割しない）
class AddEditTaskViewModel(
    private val repository: TaskRepository,
    private val taskId: Int?
) : ViewModel() {

    private val _name   = MutableStateFlow("")
    private val _hour   = MutableStateFlow(7)
    private val _minute = MutableStateFlow(0)
    private val _snooze = MutableStateFlow(10)
    private val _saved  = MutableStateFlow(false)

    val name:   StateFlow<String>  = _name.asStateFlow()
    val hour:   StateFlow<Int>     = _hour.asStateFlow()
    val minute: StateFlow<Int>     = _minute.asStateFlow()
    val snooze: StateFlow<Int>     = _snooze.asStateFlow()
    val saved:  StateFlow<Boolean> = _saved.asStateFlow()

    init {
        if (taskId != null) {
            viewModelScope.launch {
                repository.getAllTasks().collect { tasks ->
                    tasks.find { it.id == taskId }?.let { t ->
                        _name.value   = t.name
                        _hour.value   = t.hourOfDay
                        _minute.value = t.minute
                        _snooze.value = t.snoozeDurationMinutes
                    }
                }
            }
        }
    }

    fun onNameChange(v: String)   { _name.value = v }
    fun onHourChange(v: Int)      { _hour.value = v.coerceIn(0, 23) }
    fun onMinuteChange(v: Int)    { _minute.value = v.coerceIn(0, 59) }
    fun onSnoozeChange(v: Int)    { _snooze.value = v }

    fun save() {
        val n = _name.value.trim()
        if (n.isEmpty()) return
        viewModelScope.launch {
            val task = RoutineTask(
                id                   = taskId ?: 0,
                name                 = n,
                hourOfDay            = _hour.value,
                minute               = _minute.value,
                snoozeDurationMinutes = _snooze.value
            )
            repository.insertTask(task)
            _saved.value = true
        }
    }

    companion object {
        fun factory(repository: TaskRepository, taskId: Int?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AddEditTaskViewModel(repository, taskId) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    repository: TaskRepository,
    taskId: Int?,
    onBack: () -> Unit
) {
    val vm: AddEditTaskViewModel = viewModel(
        factory = AddEditTaskViewModel.factory(repository, taskId)
    )
    val name   by vm.name.collectAsStateWithLifecycle()
    val hour   by vm.hour.collectAsStateWithLifecycle()
    val minute by vm.minute.collectAsStateWithLifecycle()
    val snooze by vm.snooze.collectAsStateWithLifecycle()
    val saved  by vm.saved.collectAsStateWithLifecycle()

    // 保存完了したら自動で戻る
    LaunchedEffect(saved) { if (saved) onBack() }

    val isEdit = taskId != null

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "タスクを編集" else "タスクを追加", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // タスク名入力
            OutlinedTextField(
                value = name,
                onValueChange = vm::onNameChange,
                label = { Text("タスク名", color = TextSecondary) },
                placeholder = { Text("例：朝ご飯を食べる", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor       = TextPrimary,
                    unfocusedTextColor     = TextPrimary,
                    focusedBorderColor     = PrimaryTeal,
                    unfocusedBorderColor   = DarkSurfaceVar,
                    focusedContainerColor  = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )

            // 時刻選択
            Text("通知時刻", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker(
                    value = hour,
                    range = 0..23,
                    label = "時",
                    onValueChange = vm::onHourChange
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NumberPicker(
                    value = minute,
                    range = 0..59,
                    label = "分",
                    onValueChange = vm::onMinuteChange
                )
            }

            // スヌーズ時間選択
            Text("スヌーズ時間", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "通知を後回しにする時間を選んでください",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            val snoozeOptions = listOf(5, 10, 15, 20, 30)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snoozeOptions) { option ->
                    FilterChip(
                        selected = snooze == option,
                        onClick = { vm.onSnoozeChange(option) },
                        label = { Text("${option}分") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryTeal,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 保存ボタン
            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = DarkBackground
                )
            ) {
                Text(if (isEdit) "変更を保存" else "追加する", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** 数値を ＋／－ ボタンで増減する簡易ピッカー */
@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = { onValueChange(if (value < range.last) value + 1 else range.first) }
        ) {
            Text("▲", color = PrimaryTeal)
        }
        Text(
            text = "%02d".format(value),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        IconButton(
            onClick = { onValueChange(if (value > range.first) value - 1 else range.last) }
        ) {
            Text("▼", color = PrimaryTeal)
        }
    }
}
