package de.tanzfit.data

object Motivation {
    private val afterExercise = listOf(
        "Stark! Weiter so! 💪",
        "Sauber getanzt! 🔥",
        "Du bist on fire! 🔥",
        "Jede Wiederholung zählt! ✨",
        "Mega! Dein Körper dankt dir. 🙌",
        "Groove gehalten – nächste Runde! 🕺",
        "Wow, das sah gut aus! 💃",
        "Nicht aufhören, du schaffst das! 🚀",
        "Ein Schritt näher an deinem Ziel! 🎯",
        "Technik vor Tempo – perfekt! 👌",
        "Atme durch, du machst das großartig! 🌬️",
        "Der Beat gehört dir! 🎶",
    )

    private val finished = listOf(
        "Workout geschafft! Du bist ein Star! 🌟",
        "Fertig! Stolz auf dich! 🏆",
        "Training erledigt – du hast es gerockt! 🎉",
        "Geschafft! Morgen tanzt du noch besser. 💫",
    )

    private val daily = listOf(
        "Kleine Schritte, großer Groove. Los geht's! 💃",
        "Heute ist ein guter Tag zum Trainieren! ☀️",
        "Disziplin schlägt Motivation – fang einfach an! 🔑",
        "Jeder Profi hat mal als Anfänger angefangen. 🌱",
        "Dein zukünftiges Ich wird dir danken! 🙏",
        "10 Minuten sind besser als keine Minute! ⏱️",
        "Tanz, als ob dir keiner zuschaut – und trainier, als ob alle zuschauen! 🕺",
    )

    val halfway = listOf(
        "Halbzeit! Durchhalten! ⏳",
        "Schon die Hälfte – weiter! 💪",
        "Halb geschafft, bleib dran! 🔥",
    )

    fun afterExercise() = afterExercise.random()
    fun finished() = finished.random()
    fun daily() = daily.random()
    fun halfway() = halfway.random()
}
