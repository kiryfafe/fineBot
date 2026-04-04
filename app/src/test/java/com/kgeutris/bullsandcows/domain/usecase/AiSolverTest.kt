package com.kgeutris.bullsandcows.domain.usecase

import org.junit.Assert.*
import org.junit.Test

class AiSolverTest {

    @Test
    fun `debug - 4 быка и 1 корова должно кидать исключение`() {
        val solver = AiSolver()
        try {
            solver.validateFeedback("1234", 4, 1)
            fail("Должно было быть исключение!")
        } catch (e: IllegalArgumentException) {
            println("Ожидаемая ошибка: ${e.message}")
            assertTrue(e.message!!.contains("не может превышать 4"))
        }
    }

    @Test
    fun `AI угадывает любое 4-значное число за ≤7 попыток`() {
        val secrets = listOf(
            "1234", "5678", "9012", "3456", "7890", "1357", "2468", "1111" // ← "1111" вызовет ошибку валидации — проверим ниже
        )

        for (secret in secrets.filter { it.toSet().size == 4 }) { // только без повторов
            val solver = AiSolver()
            var attempts = 0
            var history = emptyList<Pair<String, Pair<Int, Int>>>()

            while (attempts < 10) {
                val guess = solver.nextGuess(history) ?: break
                attempts++
                val (bulls, cows) = BullsCowsEvaluator.evaluate(secret, guess)

                // Проверим, что оценка валидна (защита от некорректного ввода игрока)
                try {
                    solver.validateFeedback(guess, bulls, cows)
                } catch (e: IllegalArgumentException) {
                    fail("Некорректная оценка для $guess: $e")
                }

                if (bulls == 4) break
                history += guess to (bulls to cows)
            }

            assertTrue("Секрет $secret не угадан за 7 попыток", attempts <= 7)
        }
    }

    @Test
    fun `validateFeedback отклоняет невозможные оценки`() {
        val solver = AiSolver()

        // (4 быка, 1 корова) — невозможно: 4+1=5 > 4
        assertThrows(IllegalArgumentException::class.java) {
            solver.validateFeedback("1234", 4, 1)
        }

        // (5 быков) — невозможно
        assertThrows(IllegalArgumentException::class.java) {
            solver.validateFeedback("1234", 5, 0)
        }

        // (2 быка, 3 коровы) → 2+3=5 > 4
        assertThrows(IllegalArgumentException::class.java) {
            solver.validateFeedback("1234", 2, 3)
        }

        // (4 быка, 0 коров) — OK, но если потом скажут "и ещё 1 корова" — уже невозможно
        // ✅ Этот кейс не должен падать:
        solver.validateFeedback("1234", 4, 0)  // ← корректно

        // Дополнительно: если 4 быка — коров должно быть 0
        assertThrows(IllegalArgumentException::class.java) {
            solver.validateFeedback("1234", 4, 1)  // ← уже есть выше, но можно продублировать для ясности
        }
    }

    @Test
    fun `nextGuess возвращает null при противоречивой истории`() {
        val solver = AiSolver()
        // Игрок "соврал": сказал, что "1234" → (4,0), а потом "1235" → (4,0) — невозможно
        val history = listOf(
            "1234" to (4 to 0),
            "1235" to (4 to 0)
        )
        assertNull(solver.nextGuess(history))
    }
}