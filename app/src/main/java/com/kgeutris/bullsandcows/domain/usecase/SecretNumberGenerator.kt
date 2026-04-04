package com.kgeutris.bullsandcows.domain.usecase

import kotlin.random.Random

object SecretNumberGenerator {
    /**
     * Генерирует число заданной длины без повторяющихся цифр.
     *
     * Правила:
     * - Длина: 3, 4, 5 или 6
     * - Цифры уникальны
     * - Число не начинается с '0', если length > 1
     *
     * @param length длина числа (3–6)
     * @param random источник случайности (по умолчанию — Random.Default)
     * @return строка из уникальных цифр, например "5189"
     */
    fun generate(length: Int, random: Random = Random.Default): String {
        require(length in 3..6) { "Длина должна быть от 3 до 6, получено: $length" }

        val digits = ('0'..'9').toMutableList()
        val result = StringBuilder(length)

        // Шаг 1: Выбираем первую цифру — не '0', если length > 1
        val firstDigitPool = digits.filter { it != '0' }
        val first = firstDigitPool.random(random)
        result.append(first)
        digits.remove(first)

        // Шаг 2: Добавляем оставшиеся (length - 1) уникальных цифр
        repeat(length - 1) {
            val next = digits.random(random)
            result.append(next)
            digits.remove(next)
        }

        return result.toString()
    }
}