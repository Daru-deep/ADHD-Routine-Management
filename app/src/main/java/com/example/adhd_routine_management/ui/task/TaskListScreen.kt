package com.example.adhd_routine_management.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    repository: TaskRepository,
    onAddTask: () -> Unit,
    onEditTask: (Int) -> Unit
) {
    val vm: TaskListViewModel = viewModel(factory = TaskListViewModel.factory(repository))
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var taskToDelete by remember { mutableStateOf<RoutineTask?>(null) }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            containerColor = DarkSurface,
            title = { Text("タスクを削除", color = TextPrimary) },
            text = { Text("「${task.name}」を削除しますか？\n過去の記録は残ります。", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTask(task); taskToDelete = null }) {
                    Text("削除", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("キャンセル", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("ルーティン管理", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = PrimaryTeal,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ルーティンがありません", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("右下の ＋ ボタンで追加できます", color = TextHint, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onEdit = { onEditTask(task.id) },
                        onDelete = { taskToDelete = task },
                        onToggleActive = { vm.toggleActive(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(
    task: RoutineTask,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isActive) DarkSurface else DarkSurface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isActive) TextPrimary else TextHint
                )
                Text(
                    text = "%02d:%02d".format(task.hourOfDay, task.minute),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.isActive) SecondaryBlue else TextHint
                )
            }
            // 有効/無効スイッチ
            Switch(
                checked = task.isActive,
                onCheckedChange = { onToggleActive() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryTeal,
                    checkedTrackColor = PrimaryTeal.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextHint,
                    uncheckedTrackColor = DarkSurfaceVar
                )
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "編集", tint = TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除", tint = ErrorRed.copy(alpha = 0.7f))
            }
        }
    }
}
