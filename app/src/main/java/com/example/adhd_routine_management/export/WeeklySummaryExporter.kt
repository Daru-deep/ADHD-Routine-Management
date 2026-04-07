package com.example.adhd_routine_management.export

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.example.adhd_routine_management.data.database.entity.DailyRecord
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import com.example.adhd_routine_management.data.database.entity.TimeSlot
import com.example.adhd_routine_management.data.repository.WeeklyGoalWithTasks
import java.io.OutputStream

/**
 * 週次サマリーの画像とCSVを生成するクラス。
 * Android 標準の android.graphics.Canvas で Bitmap に直接描画する。
 * 外部ライブラリ不要。
 */
object WeeklySummaryExporter {

    // ------- 画像生成 -------

    /**
     * Bitmap に週次サマリーを描画して Uri に書き出す。
     * @return 成功なら true
     */
    fun exportImage(
        context: Context,
        uri: Uri,
        weekLabel: String,
        dailyRecords: List<DailyRecord>,
        weeklyGoal: WeeklyGoalWithTasks?,
        healthRecords: List<HealthRecord>
    ): Boolean {
        return try {
            val bmp = buildBitmap(weekLabel, dailyRecords, weeklyGoal, healthRecords)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bmp.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildBitmap(
        weekLabel: String,
        dailyRecords: List<DailyRecord>,
        weeklyGoal: WeeklyGoalWithTasks?,
        healthRecords: List<HealthRecord>
    ): Bitmap {
        val W = 900
        val H = 640
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // 色定義
        val bgColor   = Color.parseColor("#1A1A2E")
        val cardColor = Color.parseColor("#16213E")
        val teal      = Color.parseColor("#4DD0C4")
        val amber     = Color.parseColor("#FFC107")
        val green     = Color.parseColor("#4CAF50")
        val white     = Color.WHITE
        val gray      = Color.parseColor("#9E9E9E")
        val morning   = Color.parseColor("#FFB300")
        val noon      = Color.parseColor("#42A5F5")
        val evening   = Color.parseColor("#AB47BC")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 背景
        c.drawColor(bgColor)

        // ── タイトルカード ──
        drawCard(c, paint, cardColor, 24f, 20f, W - 24f, 100f)
        paint.color = teal; paint.textSize = 28f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("今週のまとめ", 48f, 56f, paint)
        paint.color = gray; paint.textSize = 20f; paint.typeface = Typeface.DEFAULT
        c.drawText(weekLabel, 48f, 84f, paint)

        // ── タスクカード ──
        val totalTasks     = dailyRecords.sumOf { it.totalTasks }
        val completedTasks = dailyRecords.sumOf { it.completedTasks }
        val rate = if (totalTasks > 0) completedTasks * 100 / totalTasks else 0
        drawCard(c, paint, cardColor, 24f, 116f, (W / 2 - 12).toFloat(), 230f)
        paint.color = teal; paint.textSize = 18f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("✓ 達成タスク", 44f, 148f, paint)
        paint.color = white; paint.textSize = 34f
        c.drawText("$completedTasks / $totalTasks", 44f, 196f, paint)
        paint.color = gray; paint.textSize = 18f; paint.typeface = Typeface.DEFAULT
        c.drawText("達成率 $rate%", 44f, 222f, paint)

        // ── 週間目標カード ──
        drawCard(c, paint, cardColor, (W / 2 + 12).toFloat(), 116f, W - 24f, 230f)
        paint.color = amber; paint.textSize = 18f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("★ 週間目標", (W / 2 + 32).toFloat(), 148f, paint)
        if (weeklyGoal != null) {
            val done  = weeklyGoal.tasks.count { it.isCompleted }
            val total = weeklyGoal.tasks.size
            paint.color = white; paint.textSize = 28f
            c.drawText("$done / $total 完了", (W / 2 + 32).toFloat(), 196f, paint)
            paint.color = gray; paint.textSize = 16f; paint.typeface = Typeface.DEFAULT
            val title = if (weeklyGoal.goal.title.length > 14)
                weeklyGoal.goal.title.take(14) + "…" else weeklyGoal.goal.title
            c.drawText(title, (W / 2 + 32).toFloat(), 222f, paint)
        } else {
            paint.color = gray; paint.textSize = 20f; paint.typeface = Typeface.DEFAULT
            c.drawText("未設定", (W / 2 + 32).toFloat(), 196f, paint)
        }

        // ── 体調グラフカード ──
        drawCard(c, paint, cardColor, 24f, 246f, W - 24f, H - 24f)
        paint.color = gray; paint.textSize = 18f; paint.typeface = Typeface.DEFAULT_BOLD
        c.drawText("体調グラフ（朝 / 昼 / 夕）", 44f, 278f, paint)

        drawHealthGraph(
            c, paint,
            left = 60f, top = 290f, right = W - 60f, bottom = H - 60f,
            healthRecords = healthRecords,
            morningColor = morning, noonColor = noon, eveningColor = evening,
            labelColor = gray, gridColor = Color.parseColor("#2A2A4A")
        )

        // 凡例
        val legendY = H - 32f
        listOf(
            Triple(morning, "朝", 60f),
            Triple(noon,    "昼", 140f),
            Triple(evening, "夕", 220f)
        ).forEach { (color, label, x) ->
            paint.color = color; paint.style = Paint.Style.FILL
            c.drawCircle(x, legendY, 7f, paint)
            paint.color = gray; paint.textSize = 16f; paint.typeface = Typeface.DEFAULT
            paint.style = Paint.Style.FILL
            c.drawText(label, x + 14f, legendY + 6f, paint)
        }

        return bmp
    }

    private fun drawCard(c: Canvas, paint: Paint, color: Int, l: Float, t: Float, r: Float, b: Float) {
        paint.color = color
        paint.style = Paint.Style.FILL
        c.drawRoundRect(l, t, r, b, 16f, 16f, paint)
    }

    private fun drawHealthGraph(
        c: Canvas, paint: Paint,
        left: Float, top: Float, right: Float, bottom: Float,
        healthRecords: List<HealthRecord>,
        morningColor: Int, noonColor: Int, eveningColor: Int,
        labelColor: Int, gridColor: Int
    ) {
        val dates = healthRecords.map { it.date }.distinct().sorted().takeLast(7)
        if (dates.isEmpty()) {
            paint.color = labelColor; paint.textSize = 16f
            c.drawText("データなし", left + 20f, (top + bottom) / 2, paint)
            return
        }

        val graphTop    = top + 20f
        val graphBottom = bottom - 30f
        val graphH      = graphBottom - graphTop
        val graphW      = right - left
        val stepX       = graphW / maxOf(dates.size - 1, 1).toFloat()

        // グリッド線（2,4,6,8,10）
        paint.strokeWidth = 1f; paint.style = Paint.Style.STROKE
        (2..10 step 2).forEach { score ->
            val y = graphBottom - (score - 1) / 9f * graphH
            paint.color = gridColor
            c.drawLine(left, y, right, y, paint)
            paint.color = labelColor; paint.textSize = 12f; paint.style = Paint.Style.FILL
            c.drawText(score.toString(), left - 20f, y + 4f, paint)
        }

        // 折れ線
        val slots = listOf(
            Triple(TimeSlot.MORNING, morningColor, "朝"),
            Triple(TimeSlot.NOON,    noonColor,    "昼"),
            Triple(TimeSlot.EVENING, eveningColor, "夕")
        )
        slots.forEach { (slot, color, _) ->
            val points = dates.map { date ->
                healthRecords.find { it.date == date && it.timeSlot == slot }?.score
            }
            val path = Path()
            var started = false
            points.forEachIndexed { i, score ->
                if (score != null) {
                    val x = left + i * stepX
                    val y = graphBottom - (score - 1) / 9f * graphH
                    if (!started) { path.moveTo(x, y); started = true }
                    else path.lineTo(x, y)
                    // 点
                    paint.color = color; paint.style = Paint.Style.FILL
                    c.drawCircle(x, y, 5f, paint)
                }
            }
            paint.color = color; paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
            c.drawPath(path, paint)
        }

        // X軸の日付ラベル（月火水木金土日）
        val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")
        dates.forEachIndexed { i, date ->
            val x = left + i * stepX
            val dow = try {
                java.time.LocalDate.parse(date).dayOfWeek.value - 1  // 0=月
            } catch (e: Exception) { i }
            paint.color = labelColor; paint.textSize = 14f
            paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.CENTER
            c.drawText(dayLabels.getOrElse(dow) { "" }, x, graphBottom + 20f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    // ------- CSV生成 -------

    /**
     * 週次サマリーCSVをOutputStreamに書き出す。
     * @return 成功なら true
     */
    fun exportCsv(
        context: Context,
        uri: Uri,
        dailyRecords: List<DailyRecord>,
        weeklyGoal: WeeklyGoalWithTasks?,
        healthRecords: List<HealthRecord>
    ): Boolean {
        return try {
            val csv = buildCsv(dailyRecords, weeklyGoal, healthRecords)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildCsv(
        dailyRecords: List<DailyRecord>,
        weeklyGoal: WeeklyGoalWithTasks?,
        healthRecords: List<HealthRecord>
    ): String {
        val dayNames = listOf("月", "火", "水", "木", "金", "土", "日")
        val sb = StringBuilder()

        // ヘッダー
        sb.appendLine("日付,曜日,達成タスク,合計タスク,達成率(%),体調(朝),体調(昼),体調(夕)")

        // 日毎の行
        val dates = (dailyRecords.map { it.date } + healthRecords.map { it.date })
            .distinct().sorted()
        dates.forEach { date ->
            val record = dailyRecords.find { it.date == date }
            val rate = if ((record?.totalTasks ?: 0) > 0)
                record!!.completedTasks * 100 / record.totalTasks else 0
            val morning = healthRecords.find { it.date == date && it.timeSlot == TimeSlot.MORNING }?.score ?: ""
            val noon    = healthRecords.find { it.date == date && it.timeSlot == TimeSlot.NOON    }?.score ?: ""
            val evening = healthRecords.find { it.date == date && it.timeSlot == TimeSlot.EVENING }?.score ?: ""
            val dow = try {
                java.time.LocalDate.parse(date).dayOfWeek.value - 1
            } catch (e: Exception) { 0 }
            sb.appendLine("$date,${dayNames.getOrElse(dow) { "" }},${record?.completedTasks ?: 0},${record?.totalTasks ?: 0},$rate,$morning,$noon,$evening")
        }

        // 週間目標サマリー
        sb.appendLine()
        sb.appendLine("週間目標,,,,,,,,")
        if (weeklyGoal != null) {
            val done  = weeklyGoal.tasks.count { it.isCompleted }
            val total = weeklyGoal.tasks.size
            sb.appendLine("タイトル,${weeklyGoal.goal.title},達成,$done/$total,,,,,")
            weeklyGoal.tasks.forEach { task ->
                sb.appendLine(",${task.name},${if (task.isCompleted) "✓" else ""},,,,,")
            }
        } else {
            sb.appendLine("未設定,,,,,,,")
        }

        return sb.toString()
    }
}
