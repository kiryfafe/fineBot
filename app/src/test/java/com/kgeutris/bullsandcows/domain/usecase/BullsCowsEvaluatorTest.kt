package com.kgeutris.bullsandcows.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BullsCowsEvaluatorTest {

    // Тест 1: полное совпадение → 4 быка
    @Test
    fun `полное совпадение 1234 vs 1234`() {
        val (bulls, cows) = BullsCowsEvaluator.evaluate("1234", "1234")
        assertEquals(4, bulls)  // ожидаем 4 быка
        assertEquals(0, cows)   // и 0 коров
    }

    // Тест 2: ничего не совпадает
    @Test
    fun `ничего не совпадает 1234 vs 5678`() {
        val (bulls, cows) = BullsCowsEvaluator.evaluate("1234", "5678")
        assertEquals(0, bulls)
        assertEquals(0, cows)
    }

    // Тест 3: классический пример из ТЗ — 1 бык, 2 коровы
    @Test
    fun `1 бык и 2 коровы 1234 vs 1325`() {
        val (bulls, cows) = BullsCowsEvaluator.evaluate("1234", "1325")
        assertEquals(1, bulls)  // '1' на своём месте
        assertEquals(2, cows)   // '2' и '3' — есть, но не на месте
    }

    // Тест 4: проверка ошибок — повтор в секрете
    @Test(expected = IllegalArgumentException::class)
    fun `ошибка- повторяющаяся цифра в секрете`() {
        BullsCowsEvaluator.evaluate("1123", "4567")
    }

    // Тест 5: повтор в попытке
    @Test(expected = IllegalArgumentException::class)
    fun `ошибка- повторяющаяся цифра в попытке`() {
        BullsCowsEvaluator.evaluate("1234", "1122")
    }

    // Тест 6: разная длина
    @Test(expected = IllegalArgumentException::class)
    fun `ошибка- разная длина чисел`() {
        BullsCowsEvaluator.evaluate("123", "1234")
    }
}