package com.example.adhd_routine_management.domain.model

/**
 * キャラクターの成長段階。
 * totalPoints によって段階が決まる。
 *
 * @param stage      段階番号（1〜5）
 * @param label      表示名
 * @param minPoints  この段階に必要な最低ポイント
 * @param emoji      キャラクターを表す絵文字（画像の代わり）
 */
enum class CharacterStage(
    val stage: Int,
    val label: String,
    val minPoints: Int,
    val emoji: String
) {
    EGG(1, "タマゴ期", 0, "🥚"),
    BABY(2, "赤ちゃん期", 100, "🐣"),
    CHILD(3, "子ども期", 300, "🐥"),
    TEEN(4, "青年期", 700, "🐔"),
    ADULT(5, "大人期", 1500, "✨🐔✨");

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
