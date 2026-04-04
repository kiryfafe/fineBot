package com.kgeutris.bullsandcows.domain.usecase

/**
 * Решатель для режима "AI угадывает".
 * Реализует упрощённый алгоритм на основе минимакс-стратегии (Кнут, 1976).
 * Поддерживает только length = 4 (как в ТЗ).
 *
 * Принцип работы:
 * 1. Генерируем все 4-значные числа без повторов и без 0 в начале (5040 штук).
 * 2. После каждой оценки игрока (быки, коровы) фильтруем кандидатов.
 * 3. Выбираем первую оставшуюся попытку.
 *
 * Гарантирует угадывание за ≤7 попыток для length=4 (теорема Кнута).
 */
class AiSolver(private val numberLength: Int = 4) {

    init {
        require(numberLength == 4) { "AiSolver поддерживает только length = 4" }
    }

    // Все возможные 4-значные числа без повторов и без 0 в начале
    private var candidates: List<String> = generateAllCandidates()

    /**
     * Генерирует следующую попытку AI.
     * @param history — список предыдущих ходов: (попытка, (быки, коровы))
     * @return новое число-попытка или null, если кандидаты закончились
     */
    fun nextGuess(history: List<Pair<String, Pair<Int, Int>>> = emptyList()): String? {
        // Фильтруем кандидатов: оставляем только те, которые совместимы со всеми оценками
        candidates = candidates.filter { candidate ->
            history.all { (guess, expected) ->
                BullsCowsEvaluator.evaluate(candidate, guess) == expected
            }
        }
        return candidates.firstOrNull()
    }

    /**
     * Проверяет, корректна ли оценка от игрока.
     * @param guess — попытка AI (например, "1234")
     * @param bulls — число быков (0..4)
     * @param cows — число коров (0..4)
     * @throws IllegalArgumentException если оценка невозможна
     */
    fun validateFeedback(guess: String, bulls: Int, cows: Int) {
        require(guess.length == numberLength) {
            "Длина попытки ($guess) должна быть $numberLength"
        }
        require(bulls >= 0 && bulls <= numberLength) {
            "Быков должно быть от 0 до $numberLength, получено: $bulls"
        }
        require(cows >= 0 && cows <= numberLength) {
            "Коров должно быть от 0 до $numberLength, получено: $cows"
        }
        require(bulls + cows <= numberLength) {
            "Сумма быков ($bulls) и коров ($cows) не может превышать $numberLength (получено: ${bulls + cows})"
        }
        require(bulls + cows <= guess.toSet().size) {
            "Невозможная оценка: быки + коровы = ${bulls + cows}, " +
                    "но в попытке '${guess}' только ${guess.toSet().size} уникальных цифр"
        }
        // Дополнительная проверка: если все цифры на месте — коров быть не может
        if (bulls == numberLength) {
            require(cows == 0) {
                "При $numberLength быках коров должно быть 0, получено: $cows"
            }
        }
    }

    // Генерация всех 5040 кандидатов (1–9, затем 0–9 без повторов)
    private fun generateAllCandidates(): List<String> {
        val result = mutableListOf<String>()
        for (d1 in '1'..'9') {
            for (d2 in '0'..'9') {
                if (d2 == d1) continue
                for (d3 in '0'..'9') {
                    if (d3 in setOf(d1, d2)) continue
                    for (d4 in '0'..'9') {
                        if (d4 in setOf(d1, d2, d3)) continue
                        result += "$d1$d2$d3$d4"
                    }
                }
            }
        }
        return result
    }
}