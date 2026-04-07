package com.example.adhd_routine_management.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import com.example.adhd_routine_management.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<RoutineTask>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTask(task: RoutineTask) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun toggleActive(task: RoutineTask) {
        viewModelScope.launch { repository.updateTask(task.copy(isActive = !task.isActive)) }
    }

    companion object {
        fun factory(repository: TaskRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TaskListViewModel(repository) as T
        }
    }
}
