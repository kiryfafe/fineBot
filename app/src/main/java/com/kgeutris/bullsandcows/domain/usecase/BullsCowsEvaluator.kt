package com.kgeutris.bullsandcows.domain.usecase

object BullsCowsEvaluator {
    fun evaluate(secret: String, guess: String): Pair<Int, Int> {
        require(secret.length == guess.length) { "Длина чисел должна совпадать" }
        require(secret.all { it.isDigit() } && guess.all { it.isDigit() }) { "Число должно содержать только цифры" }
        require(secret.toSet().size == secret.length) { "Секретное число не должно содержать повторяющихся цифр" }
        require(guess.toSet().size == guess.length) { "Попытка не должна содержать повторяющихся цифр" }

        val bulls = secret.zip(guess).count { (s, g) -> s == g }
        val commonDigits = guess.toSet().intersect(secret.toSet()).size
        val cows = commonDigits - bulls
        return bulls to cows
    }
}