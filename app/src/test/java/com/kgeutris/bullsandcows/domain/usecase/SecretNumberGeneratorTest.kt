package com.kgeutris.bullsandcows.domain.usecase

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class SecretNumberGeneratorTest {

    // ✅ Тест 1: Проверка корректности при length=4 (без конкретных значений)
    @Test
    fun `generate при length=4 даёт корректное число`() {
        val number = SecretNumberGenerator.generate(4)
        assertEquals(4, number.length)
        assertEquals(4, number.toSet().size) // уникальность
        assertNotEquals('0', number[0])     // не начинается с 0
        assertTrue(number.all { it.isDigit() })
    }

    // ✅ Тест 2: Проверка исключений
    @Test(expected = IllegalArgumentException::class)
    fun `ошибка при length=2`() {
        SecretNumberGenerator.generate(2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ошибка при length=7`() {
        SecretNumberGenerator.generate(7)
    }

    // ✅ Тест 3: Одинаковый seed → одинаковый результат (без hardcode '873')
    @Test
    fun `одинаковые seed дают одинаковые результаты`() {
        val seed = 12345L
        val r1 = Random(seed)
        val r2 = Random(seed)
        val n1 = SecretNumberGenerator.generate(4, r1)
        val n2 = SecretNumberGenerator.generate(4, r2)
        assertEquals(n1, n2) // ← достаточно проверить равенство, не зная конкретного значения
    }

    // ✅ Тест 4: Разные seed → разные результаты (с высокой вероятностью)
    @Test
    fun `разные seed дают разные результаты`() {
        val n1 = SecretNumberGenerator.generate(4, Random(1))
        val n2 = SecretNumberGenerator.generate(4, Random(2))
        assertNotEquals(n1, n2)
    }

    // ✅ Тест 5: length=3 — тоже работает
    @Test
    fun `generate при length=3 корректно`() {
        val number = SecretNumberGenerator.generate(3)
        assertEquals(3, number.length)
        assertEquals(3, number.toSet().size)
        assertNotEquals('0', number[0])
    }

}