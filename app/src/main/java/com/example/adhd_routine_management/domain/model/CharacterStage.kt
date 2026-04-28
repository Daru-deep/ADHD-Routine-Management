package com.example.adhd_routine_management.domain.model

/**
 * キャラクターの成長段階。
 * totalPoints によって段階が決まる。
 *
 * @param stage      段階番号（1〜9）
 * @param label      表示名
 * @param minPoints  この段階に必要な最低ポイント
 * @param emoji      キャラクターを表す絵文字（画像の代わり）
 * @param phase      第何段階か（1〜3）
 */
enum class CharacterStage(
    val stage: Int,
    val label: String,
    val minPoints: Int,
    val emoji: String,
    val phase: Int = 1
) {
    // ── 第一段階：ひよこ成長ルート ──────────────────
    EGG(1,   "タマゴ期",   0,     "🥚",         1),
    BABY(2,  "赤ちゃん期", 100,   "🐣",         1),
    CHILD(3, "子ども期",   300,   "🐥",         1),
    TEEN(4,  "青年期",     700,   "🐔",         1),
    ADULT(5, "大人期",     1500,  "✨🐔✨",     1),

    // ── 第二段階：鷹への進化ルート ───────────────────
    EVOLVED(6, "進化期",       2500,  "🦅",         2),
    MASTER(7,  "マスター期",   4500,  "✨🦅✨",     2),

    // ── 第三段階：龍への昇華ルート ───────────────────
    LEGEND(8, "伝説期",        7000,  "🐉",         3),
    GOD(9,    "神化期",        12000, "✨🐉✨",     3);

    companion object {
        /** ポイント数からステージを取得する */
        fun fromPoints(points: Int): CharacterStage {
            return entries.reversed().firstOrNull { points >= it.minPoints } ?: EGG
        }

        /** 次のステージまで何ポイント必要か（最終ステージなら null） */
        fun pointsToNextStage(points: Int): Int? {
            val current = fromPoints(points)
            val next = entries.firstOrNull { it.stage == current.stage + 1 }
            return next?.let { it.minPoints - points }
        }
    }
}
