package com.example.adhd_routine_management

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.adhd_routine_management.data.database.AppDatabase
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import com.example.adhd_routine_management.notification.NotificationHelper
import com.example.adhd_routine_management.notification.NudgeWorker
import com.example.adhd_routine_management.ui.navigation.AppNavHost
import com.example.adhd_routine_management.ui.theme.ADHDRoutineTheme

class MainActivity : ComponentActivity() {

    // Android 13以降で通知パーミッションを要求するためのランチャー
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 結果は問わず、許可されなくても基本機能は動く */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 通知チャンネル作成（Android 8.0以降で必要）
        NotificationHelper.createChannels(this)

        // Android 13以降は実行時に通知パーミッションが必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 催促通知（30分おき）を開始
        NudgeWorker.schedule(this)

        // DB と Repository を生成（シンプルな手動 DI）
        val db = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(
            taskDao           = db.routineTaskDao(),
            completionDao     = db.taskCompletionDao(),
            dailyRecordDao    = db.dailyRecordDao(),
            progressDao       = db.userProgressDao(),
            healthRecordDao   = db.healthRecordDao()
        )
        val weeklyGoalRepository = WeeklyGoalRepository(
            goalDao = db.weeklyGoalDao(),
            taskDao = db.weeklyGoalTaskDao()
        )

        setContent {
            ADHDRoutineTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController        = navController,
                    repository           = repository,
                    weeklyGoalRepository = weeklyGoalRepository,
                    modifier             = Modifier.fillMaxSize()
                )
            }
        }
    }
}
