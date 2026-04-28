package com.example.adhd_routine_management.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.ui.dashboard.DashboardScreen
import com.example.adhd_routine_management.ui.home.HomeScreen
import com.example.adhd_routine_management.ui.progress.ProgressScreen
import com.example.adhd_routine_management.ui.settings.SettingsScreen
import com.example.adhd_routine_management.ui.task.AddEditTaskScreen
import com.example.adhd_routine_management.ui.task.TaskListScreen
import com.example.adhd_routine_management.ui.theme.*
import com.example.adhd_routine_management.ui.weeklygoal.WeeklyGoalScreen
import com.example.adhd_routine_management.ui.weeklygoal.WeeklyGoalSetupScreen

/** アプリ内の画面ルート定数 */
sealed class Screen(val route: String) {
    object Dashboard      : Screen("dashboard")
    object Settings       : Screen("settings")
    object Home           : Screen("home")
    object TaskList       : Screen("task_list")
    object AddTask        : Screen("add_task")
    object EditTask       : Screen("edit_task/{taskId}") {
        fun createRoute(taskId: Int) = "edit_task/$taskId"
    }
    object Progress       : Screen("progress")
    object WeeklyGoal     : Screen("weekly_goal")
    object WeeklyGoalSetup: Screen("weekly_goal_setup")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: TaskRepository,
    weeklyGoalRepository: WeeklyGoalRepository,
    modifier: Modifier = Modifier
) {
    // 現在のルートを監視してボトムナビの選択状態を同期する
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            // すべてのページでボトムナビを表示する
            AppBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    // タブ切り替え時は Dashboard までバックスタックをクリアして再利用する
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // innerPadding にボトムナビの高さが含まれるので、NavHost に渡してコンテンツが隠れないようにする
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    repository = repository,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository           = repository,
                    weeklyGoalRepository = weeklyGoalRepository,
                    onBack               = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    repository = repository,
                    weeklyGoalRepository = weeklyGoalRepository
                )
            }
            composable(Screen.TaskList.route) {
                TaskListScreen(
                    repository = repository,
                    onAddTask  = { navController.navigate(Screen.AddTask.route) },
                    onEditTask = { taskId -> navController.navigate(Screen.EditTask.createRoute(taskId)) }
                )
            }
            composable(Screen.AddTask.route) {
                AddEditTaskScreen(
                    repository = repository,
                    taskId = null,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.EditTask.route) { backStack ->
                val taskId = backStack.arguments?.getString("taskId")?.toIntOrNull()
                AddEditTaskScreen(
                    repository = repository,
                    taskId = taskId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Progress.route) {
                ProgressScreen(repository = repository)
            }
            composable(Screen.WeeklyGoal.route) {
                WeeklyGoalScreen(
                    repository        = weeklyGoalRepository,
                    onNavigateToSetup = { navController.navigate(Screen.WeeklyGoalSetup.route) }
                )
            }
            composable(Screen.WeeklyGoalSetup.route) {
                WeeklyGoalSetupScreen(
                    repository = weeklyGoalRepository,
                    onBack     = { navController.popBackStack() }
                )
            }
        }
    }
}

/** アプリ共通のボトムナビゲーションバー */
@Composable
private fun AppBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor   = PrimaryTeal,
        selectedTextColor   = PrimaryTeal,
        indicatorColor      = PrimaryTeal.copy(alpha = 0.15f),
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
    )
    NavigationBar(containerColor = DarkSurface) {
        NavigationBarItem(
            selected = currentRoute == Screen.Dashboard.route,
            onClick  = { onNavigate(Screen.Dashboard.route) },
            icon     = { Icon(Icons.Default.Dashboard, contentDescription = "概要") },
            label    = { Text("概要") },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick  = { onNavigate(Screen.Home.route) },
            icon     = { Icon(Icons.Default.Home, contentDescription = "ホーム") },
            label    = { Text("ホーム") },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.TaskList.route,
            onClick  = { onNavigate(Screen.TaskList.route) },
            icon     = { Icon(Icons.Default.List, contentDescription = "タスク") },
            label    = { Text("タスク") },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.WeeklyGoal.route,
            onClick  = { onNavigate(Screen.WeeklyGoal.route) },
            icon     = { Icon(Icons.Default.Star, contentDescription = "週目標") },
            label    = { Text("週目標") },
            colors   = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Progress.route,
            onClick  = { onNavigate(Screen.Progress.route) },
            icon     = { Icon(Icons.Default.BarChart, contentDescription = "進捗") },
            label    = { Text("進捗") },
            colors   = itemColors
        )
    }
}
