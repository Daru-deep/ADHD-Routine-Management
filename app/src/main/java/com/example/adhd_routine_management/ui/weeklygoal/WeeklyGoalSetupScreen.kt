package com.example.adhd_routine_management.ui.weeklygoal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// ========== ViewModel ==========

class WeeklyGoalSetupViewModel(
    private val repository: WeeklyGoalRepository
) : ViewModel() {

    // 今週の月曜日
    val weekStart: String = LocalDate.now().with(DayOfWeek.MONDAY).toString()

    private val _title = MutableStateFlow("")
    // タスク入力フィールド（5つ固定）
    private val _taskNames = MutableStateFlow(List(5) { "" })
    private val _saved = MutableStateFlow(false)

    val title: StateFlow<String> = _title.asStateFlow()
    val taskNames: StateFlow<List<String>> = _taskNames.asStateFlow()
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    /** 入力が有効かどうか（タイトルと全5タスクが入力済み） */
    val isValid: Boolean
        get() = _title.value.isNotBlank() && _taskNames.value.all { it.isNotBlank() }

    fun onTitleChange(v: String) { _title.value = v }

    fun onTaskNameChange(index: Int, v: String) {
        _taskNames.value = _taskNames.value.toMutableList().also { it[index] = v }
    }

    fun save() {
        if (!isValid) return
        viewModelScope.launch {
            repository.createGoal(
                weekStart = weekStart,
                title = _title.value.trim(),
                taskNames = _taskNames.value.map { it.trim() }
            )
            _saved.value = true
        }
    }

    companion object {
        fun factory(repository: WeeklyGoalRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WeeklyGoalSetupViewModel(repository) as T
        }
    }
}

// ========== Screen ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyGoalSetupScreen(
    repository: WeeklyGoalRepository,
    onBack: () -> Unit
) {
    val vm: WeeklyGoalSetupViewModel = viewModel(
        factory = WeeklyGoalSetupViewModel.factory(repository)
    )
    val title by vm.title.collectAsStateWithLifecycle()
    val taskNames by vm.taskNames.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    // 保存完了したら戻る
    LaunchedEffect(saved) { if (saved) onBack() }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("今週の目標を設定", color = TextPrimary) },
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "今週の目標",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            // 目標タイトル入力
            OutlinedTextField(
                value = title,
                onValueChange = vm::onTitleChange,
                label = { Text("目標のタイトル", color = TextSecondary) },
                placeholder = { Text("例：今週は毎朝ストレッチを続ける", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = DarkSurfaceVar,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "目標タスク（5つ、すべて入力必須）",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "この目標を達成するための具体的な行動を5つ書きましょう",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // タスク入力フィールド（5つ）
            taskNames.forEachIndexed { index, name ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { vm.onTaskNameChange(index, it) },
                    label = { Text("タスク ${index + 1}", color = TextSecondary) },
                    placeholder = { Text("例：朝起きたらすぐストレッチ5分", color = TextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (index < 4) ImeAction.Next else ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = DarkSurfaceVar,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 保存ボタン（isValid を毎回チェックする必要があるため derived state を使う）
            val isValid by remember {
                derivedStateOf {
                    title.isNotBlank() && taskNames.all { it.isNotBlank() }
                }
            }

            Button(
                onClick = vm::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isValid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceVar,
                    disabledContentColor = TextHint
                )
            ) {
                Text("目標を設定する", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
