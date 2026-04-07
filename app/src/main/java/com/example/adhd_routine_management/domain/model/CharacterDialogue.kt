package com.example.adhd_routine_management.domain.model

/**
 * キャラクターのセリフを管理するオブジェクト。
 * AI に考えてもらった段階別・状態別のセリフを事前登録する。
 *
 * 状態：
 *  NORMAL    : 通常時（ホーム画面表示時）
 *  COMPLETED : タスク完了時
 *  NEGLECTED : タスクを放置している時（催促通知）
 *  GIVE_UP   : 今日はあきらめた時
 *  STREAK    : 連続達成報告時
 */
enum class DialogueType { NORMAL, COMPLETED, NEGLECTED, GIVE_UP, STREAK }

object CharacterDialogue {

    private val dialogues: Map<Pair<CharacterStage, DialogueType>, List<String>> = mapOf(

        // ===== タマゴ期 =====
        Pair(CharacterStage.EGG, DialogueType.NORMAL) to listOf(
            "……（まだ眠い）",
            "……ごそごそ",
            "……（中で何かが動いている気配）",
            "……ぽかぽか",
            "……（今日も一緒にいるよ）",
            "……（ゆっくり揺れている）"
        ),
        Pair(CharacterStage.EGG, DialogueType.COMPLETED) to listOf(
            "……！（嬉しそうに揺れた）",
            "……ぴこ",
            "……（ぬくもりを感じた）",
            "……！（ぴょんと跳ねた）",
            "……（光が見えた気がした）",
            "……ぴか！（輝いた気がする）"
        ),
        Pair(CharacterStage.EGG, DialogueType.NEGLECTED) to listOf(
            "……（冷たくなってきた）",
            "……（ひびが入りそうで心配）",
            "……たすけて（かすかな声）",
            "……（静かになってしまった）",
            "……（中が暗くなってきた）",
            "……（揺れが弱くなってきた）"
        ),
        Pair(CharacterStage.EGG, DialogueType.GIVE_UP) to listOf(
            "……（しょんぼり揺れた）",
            "……（また明日ね）",
            "……（大丈夫、明日がある）",
            "……（今日はゆっくりしよ）",
            "……（休むのも大事だよ）"
        ),
        Pair(CharacterStage.EGG, DialogueType.STREAK) to listOf(
            "……！！（激しく揺れた）",
            "……！！！（今にも割れそうなくらい喜んでいる）",
            "……（こんなに嬉しいのははじめて）",
            "……ぴかぴか！！（眩しいくらい光っている）"
        ),

        // ===== 赤ちゃん期 =====
        Pair(CharacterStage.BABY, DialogueType.NORMAL) to listOf(
            "ぴよ！今日もよろしく！",
            "ぴよぴよ～！いっしょにがんばろ！",
            "ねむい…でもいっしょにいるよ！",
            "ぴよ！きょうもいいひになりそう！",
            "ぴよ～…おはよう！ぴよ！",
            "きょうも会えてうれしい！ぴよぴよ！"
        ),
        Pair(CharacterStage.BABY, DialogueType.COMPLETED) to listOf(
            "ぴよ！すごいすごい！！",
            "やったー！！ぴよぴよぴよ！",
            "えらい！ほんとえらい！！",
            "ぴよ！！きらきらしてる！",
            "すごすぎる！！ぴよぴよぴよぴよ！！",
            "ぴよぴよ！！だいすき！！"
        ),
        Pair(CharacterStage.BABY, DialogueType.NEGLECTED) to listOf(
            "ぴよ…わすれてない…？",
            "ぴよぴよ…さみしいよ…",
            "ねえねえ…ちょっとだけでいいから…",
            "ぴよ…おなかすいてる…",
            "まだかな…まだかな…ぴよ…",
            "ぴよぴよ…まってるよ…"
        ),
        Pair(CharacterStage.BABY, DialogueType.GIVE_UP) to listOf(
            "ぴよ…（うなだれた）また明日いっしょにやろ？",
            "今日はむずかしかったんだね。ぴよ。",
            "ぴよ…つかれたんだね。ゆっくりしてね。",
            "だいじょうぶだよ。ぴよ！またあした！",
            "ぴよぴよ…休むのもだいじだよ。"
        ),
        Pair(CharacterStage.BABY, DialogueType.STREAK) to listOf(
            "ぴよぴよ！！すごい連続記録だよ！！！",
            "ぴよぴよぴよぴよ！！！止まらない！！",
            "ぴよ！！！きせきだよ！！！",
            "ぴよぴよ！！！だいすきだよ！！！"
        ),

        // ===== 子ども期 =====
        Pair(CharacterStage.CHILD, DialogueType.NORMAL) to listOf(
            "よーし！今日もがんばるぞ！",
            "お、今日もきたね！いっしょにやっつけよう！",
            "タスクなんてちょろいちょろい！（たぶん）",
            "今日もよろしく！絶対やるぞ！",
            "ぼくここにいるからね！一緒にがんばろう！",
            "今日はなんか調子よさそう！やれる気がする！"
        ),
        Pair(CharacterStage.CHILD, DialogueType.COMPLETED) to listOf(
            "やったじゃん！！かっこいい！！",
            "さすがだよ！！ぼくみてたよ！！",
            "完璧！！次もできる！！",
            "やば！！！本当にすごい！！",
            "ぼくが自慢したい！！めちゃくちゃえらい！！",
            "天才！！！ほんとに天才！！！"
        ),
        Pair(CharacterStage.CHILD, DialogueType.NEGLECTED) to listOf(
            "ねえ、まだ終わってないよ？",
            "ぼく待ってるんだけど…ねえってば！",
            "このままじゃぼく元気なくなっちゃう…本当に…",
            "ちょっとだけでいいよ…やってみよ？",
            "ねえ！起きてる！？ねえ！！",
            "ぼくのこと、忘れてない…？"
        ),
        Pair(CharacterStage.CHILD, DialogueType.GIVE_UP) to listOf(
            "そっか、今日は難しかったか。しょうがないよ。",
            "無理しなくていいよ。でも明日はいっしょにやろうね。",
            "疲れてるんだね。ゆっくり休んでね。",
            "今日はここまでにしよう。また明日元気出そうね！",
            "休むのも作戦だよ！明日また一緒にやろ！"
        ),
        Pair(CharacterStage.CHILD, DialogueType.STREAK) to listOf(
            "連続記録更新！！！ぼくめちゃくちゃ嬉しい！！",
            "やば！！こんな記録みたことない！！すごすぎ！！",
            "もうぼく感動して泣きそう！！！えらすぎる！！！",
            "ぼくもっとがんばるね！！負けないぞ！！"
        ),

        // ===== 青年期 =====
        Pair(CharacterStage.TEEN, DialogueType.NORMAL) to listOf(
            "今日もやるか。一緒にな。",
            "まあ、ぼちぼちやっていこ。",
            "お前ならできる。俺が保証する。",
            "別に、心配してるわけじゃないけど…一緒にいるから。",
            "さて、始めるか。",
            "お前のこと、信じてるから。"
        ),
        Pair(CharacterStage.TEEN, DialogueType.COMPLETED) to listOf(
            "…やるじゃん。正直見直した。",
            "うん、よくやった。本当に。",
            "その調子。続けていこう。",
            "…ちゃんと見てたぞ。よくやった。",
            "言うことなし。本当に。",
            "お前が頑張ってるの、ちゃんと見えてるから。"
        ),
        Pair(CharacterStage.TEEN, DialogueType.NEGLECTED) to listOf(
            "なあ、まだ終わってないぞ。",
            "…心配して損した。早くやれって。",
            "頼むから、ちゃんとやってくれ。本当に頼む。",
            "…俺が怒ったとこ、見たくないだろ？早くやれ。",
            "お前のことが心配なんだよ。ちゃんとやれ。",
            "放置はよくない。ちょっとだけでいいから動いてくれ。"
        ),
        Pair(CharacterStage.TEEN, DialogueType.GIVE_UP) to listOf(
            "今日は無理か。たまにはしょうがない。",
            "まあ、休む日も必要だよ。明日また一緒にやろ。",
            "…疲れたんなら休めよ。無理すんな。",
            "今日はここまで。明日、俺が待ってる。",
            "しょうがない。でも明日はやるぞ。約束な。"
        ),
        Pair(CharacterStage.TEEN, DialogueType.STREAK) to listOf(
            "…すごいな。正直、尊敬する。",
            "…お前のこと、誇りに思う。本当に。",
            "こんな記録、俺も初めて見た。本物だよ、お前は。",
            "…何も言えない。ただ、すごい。"
        ),

        // ===== 大人期 =====
        Pair(CharacterStage.ADULT, DialogueType.NORMAL) to listOf(
            "今日もよろしく。ずっと一緒にいるよ。",
            "お互い、長い付き合いになったね。今日もがんばろう。",
            "ここまで来たのは、あなたの力だよ。",
            "今日という日は、今日しかない。一緒に行こう。",
            "あなたのそばにいられて、私は幸せだよ。",
            "どんな日でも、あなたと一緒なら大丈夫。"
        ),
        Pair(CharacterStage.ADULT, DialogueType.COMPLETED) to listOf(
            "ありがとう。本当に、ありがとう。",
            "続けてきた結果だよ。誇っていい。",
            "一緒にここまで来られて、嬉しいよ。",
            "あなたが頑張るたびに、私も強くなれた気がする。",
            "積み上げてきた日々が、今日も輝いてる。",
            "こうして頑張る姿を見られて、本当によかった。"
        ),
        Pair(CharacterStage.ADULT, DialogueType.NEGLECTED) to listOf(
            "心配してる。大丈夫？",
            "無理してるなら言って。でも、できるなら一歩だけ踏み出してほしい。",
            "ここまで続けてきたんだから。あともう少しだけ。",
            "あなたのペースでいい。でも、諦めないで。",
            "一人じゃないよ。私がここにいる。",
            "小さな一歩でいい。それだけで、十分だから。"
        ),
        Pair(CharacterStage.ADULT, DialogueType.GIVE_UP) to listOf(
            "今日は休もう。あなたはよくやってる。",
            "休息も成長のうち。明日また一緒に。",
            "疲れた時は休んでいい。それも強さの一つだよ。",
            "今日はゆっくりしよう。あなたの心が一番大切だから。",
            "無理しなくていい。あなたがいてくれるだけで十分だよ。"
        ),
        Pair(CharacterStage.ADULT, DialogueType.STREAK) to listOf(
            "…言葉にならないよ。本当にすごい。",
            "これだけの日々を積み重ねてきた。あなたは本当に強い。",
            "ありがとう。一緒に歩んでくれて、本当にありがとう。",
            "この記録は、あなたが諦めなかった証だよ。"
        )
    )

    /**
     * ステージと状態に応じたセリフをランダムに1つ返す。
     * 対応するセリフがなければデフォルトセリフを返す。
     */
    fun get(stage: CharacterStage, type: DialogueType): String {
        return dialogues[Pair(stage, type)]?.random()
            ?: "…"
    }
}
