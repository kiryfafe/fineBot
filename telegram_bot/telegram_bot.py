"""
Telegram Bot для игры "Быки и Коровы"

Этот бот позволяет играть в классическую игру "Быки и Коровы".
Компьютер загадывает число, а игрок должен его угадать.

Для запуска:
1. Установите зависимости: pip install python-telegram-bot
2. Получите токен бота от @BotFather в Telegram
3. Запустите: python telegram_bot.py YOUR_BOT_TOKEN
"""

import random
import logging
from typing import Dict, Tuple

from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application,
    CommandHandler,
    MessageHandler,
    CallbackQueryHandler,
    ContextTypes,
    filters,
)

# Настройка логирования
logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO,
)
logger = logging.getLogger(__name__)


class GameState:
    """Класс для хранения состояния игры для каждого пользователя."""

    def __init__(self, secret_number: str, number_length: int = 4):
        self.secret_number = secret_number
        self.number_length = number_length
        self.attempts = 0
        self.history: list[Tuple[str, int, int]] = []  # (guess, bulls, cows)
        self.is_game_over = False


def generate_secret_number(length: int = 4) -> str:
    """
    Генерирует случайное число заданной длины без повторяющихся цифр.
    
    Правила:
    - Длина: 3, 4, 5 или 6
    - Цифры уникальны
    - Число не начинается с '0', если длина > 1
    
    :param length: длина числа (по умолчанию 4)
    :return: строка из уникальных цифр, например "5189"
    """
    if not 3 <= length <= 6:
        raise ValueError("Длина должна быть от 3 до 6")

    digits = list("0123456789")
    result = []

    # Выбираем первую цифру — не '0', если длина > 1
    first_digit_pool = [d for d in digits if d != "0"]
    first = random.choice(first_digit_pool)
    result.append(first)
    digits.remove(first)

    # Добавляем оставшиеся уникальные цифры
    for _ in range(length - 1):
        next_digit = random.choice(digits)
        result.append(next_digit)
        digits.remove(next_digit)

    return "".join(result)


def evaluate_guess(secret: str, guess: str) -> Tuple[int, int]:
    """
    Оценивает попытку игрока, возвращая количество быков и коров.
    
    :param secret: загаданное число
    :param guess: предположение игрока
    :return: кортеж (быки, коровы)
    """
    if len(secret) != len(guess):
        raise ValueError("Длина чисел должна совпадать")
    if not secret.isdigit() or not guess.isdigit():
        raise ValueError("Число должно содержать только цифры")
    if len(set(secret)) != len(secret):
        raise ValueError("Секретное число не должно содержать повторяющихся цифр")
    if len(set(guess)) != len(guess):
        raise ValueError("Попытка не должна содержать повторяющихся цифр")

    # Быки: цифры на правильных позициях
    bulls = sum(s == g for s, g in zip(secret, guess))

    # Коровы: общие цифры, но не на своих местах
    common_digits = len(set(secret) & set(guess))
    cows = common_digits - bulls

    return bulls, cows


# Хранилище состояний игр для всех пользователей
user_games: Dict[int, GameState] = {}


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /start."""
    user = update.effective_user
    await update.message.reply_html(
        f"Привет, {user.mention_html()}! 🎮\n\n"
        "Я бот для игры <b>«Быки и Коровы»</b>!\n\n"
        "<b>Правила игры:</b>\n"
        "• Я загадываю число с уникальными цифрами\n"
        "• Ты пытаешься его угадать\n"
        "• После каждой попытки я говорю:\n"
        "  🐂 <b>Быки</b> — цифры на правильных местах\n"
        "  🐄 <b>Коровы</b> — правильные цифры не на своих местах\n\n"
        "<b>Команды:</b>\n"
        "/newgame — начать новую игру (число из 4 цифр)\n"
        "/help — показать справку\n"
        "/cancel — прекратить текущую игру\n\n"
        "Просто отправь мне число, чтобы сделать ход!"
    )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /help."""
    await update.message.reply_text(
        "📖 <b>Справка по игре «Быки и Коровы»</b>\n\n"
        "<b>Как играть:</b>\n"
        "1. Используй /newgame для начала новой игры\n"
        "2. Отправь число (например, 1234) как попытку угадать загаданное число\n"
        "3. Получи ответ в формате: 🐂 X 🐄 Y\n"
        "4. Продолжай, пока не угадаешь всё число!\n\n"
        "<b>Пример:</b>\n"
        "Загадано: 5189\n"
        "Твоя попытка: 5281\n"
        "Ответ: 🐂 2 🐄 2 (5 и 8 на правильных местах, 1 и 2 есть, но не там)\n\n"
        "<b>Команды:</b>\n"
        "/newgame [3-6] — новая игра с длиной числа 3-6 (по умолчанию 4)\n"
        "/cancel — закончить текущую игру\n"
        "/stats — показать статистику текущей игры",
        parse_mode="HTML",
    )


async def new_game(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /newgame."""
    user_id = update.effective_user.id

    # Определяем длину числа из аргументов
    number_length = 4
    if context.args and context.args[0].isdigit():
        try:
            number_length = int(context.args[0])
            if not 3 <= number_length <= 6:
                await update.message.reply_text(
                    "⚠️ Длина числа должна быть от 3 до 6. Использую значение по умолчанию: 4"
                )
                number_length = 4
        except ValueError:
            pass

    # Генерируем секретное число
    secret_number = generate_secret_number(number_length)

    # Создаём новое состояние игры
    user_games[user_id] = GameState(secret_number, number_length)

    await update.message.reply_text(
        f"🎮 <b>Новая игра началась!</b>\n\n"
        f"Я загадал число из <b>{number_length}</b> цифр.\n"
        "Попробуй угадать его!\n\n"
        "Отправь число, чтобы сделать ход.",
        parse_mode="HTML",
    )

    logger.info(f"User {user_id} started a new game with {number_length}-digit number")


async def cancel_game(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /cancel."""
    user_id = update.effective_user.id

    if user_id in user_games:
        del user_games[user_id]
        await update.message.reply_text(
            "❌ Игра завершена.\nИспользуй /newgame, чтобы начать заново."
        )
        logger.info(f"User {user_id} cancelled the game")
    else:
        await update.message.reply_text(
            "У тебя нет активной игры.\nИспользуй /newgame, чтобы начать."
        )


async def show_stats(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /stats."""
    user_id = update.effective_user.id

    if user_id not in user_games:
        await update.message.reply_text(
            "У тебя нет активной игры.\nИспользуй /newgame, чтобы начать."
        )
        return

    game = user_games[user_id]

    stats_text = (
        f"📊 <b>Статистика игры</b>\n\n"
        f"Длина числа: {game.number_length}\n"
        f"Попыток: {game.attempts}\n\n"
    )

    if game.history:
        stats_text += "<b>История ходов:</b>\n"
        for i, (guess, bulls, cows) in enumerate(game.history[-10:], 1):
            stats_text += f"{i}. {guess} → 🐂{bulls} 🐄{cows}\n"

        if len(game.history) > 10:
            stats_text += f"... и ещё {len(game.history) - 10} ходов"
    else:
        stats_text += "Пока нет ходов. Сделай первый ход!"

    await update.message.reply_text(stats_text, parse_mode="HTML")


async def handle_guess(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик попыток игрока угадать число."""
    user_id = update.effective_user.id
    guess = update.message.text.strip()

    # Проверяем, есть ли активная игра
    if user_id not in user_games:
        await update.message.reply_text(
            "🤔 У тебя нет активной игры.\n"
            "Используй /newgame, чтобы начать новую игру."
        )
        return

    game = user_games[user_id]

    # Проверяем, не закончена ли игра
    if game.is_game_over:
        await update.message.reply_text(
            "✅ Эта игра уже завершена!\n"
            "Используй /newgame, чтобы начать новую."
        )
        return

    # Валидация ввода
    if not guess.isdigit():
        await update.message.reply_text(
            "❌ Пожалуйста, отправь только цифры (без пробелов и других символов)."
        )
        return

    if len(guess) != game.number_length:
        await update.message.reply_text(
            f"❌ Число должно содержать ровно {game.number_length} цифр."
        )
        return

    if len(set(guess)) != len(guess):
        await update.message.reply_text(
            "❌ Цифры в числе не должны повторяться."
        )
        return

    # Оцениваем попытку
    bulls, cows = evaluate_guess(game.secret_number, guess)
    game.attempts += 1
    game.history.append((guess, bulls, cows))

    # Формируем ответ
    if bulls == game.number_length:
        game.is_game_over = True
        
        # Формируем историю всех попыток для отображения
        history_text = ""
        if game.history:
            history_text = "\n\n<b>История попыток:</b>\n"
            for i, (attempt_guess, attempt_bulls, attempt_cows) in enumerate(game.history, 1):
                history_text += f"{i}. <code>{attempt_guess}</code> - 🐂{attempt_bulls} | 🐄{attempt_cows}\n"
        
        response = (
            f"🎉 <b>Поздравляю! Ты выиграл!</b> 🎉\n\n"
            f"Загаданное число: <code>{game.secret_number}</code>\n"
            f"Количество попыток: <b>{game.attempts}</b>"
            f"{history_text}"
            f"\nХочешь сыграть ещё? Используй /newgame"
        )
        logger.info(f"User {user_id} won in {game.attempts} attempts")
    else:
        # Подсказки для разных ситуаций
        hints = []
        if game.attempts >= 1 and bulls == 0 and cows == 0:
            hints.append("💡 Подсказка: ни одна цифра не подходит. Попробуй другие цифры!")
        elif game.attempts >= 15:
            hints.append("💡 Не сдавайся! Анализируй предыдущие ходы.")

        # Формируем историю всех попыток для отображения
        history_text = ""
        if game.history:
            history_text = "\n\n<b>История попыток:</b>\n"
            for i, (attempt_guess, attempt_bulls, attempt_cows) in enumerate(game.history, 1):
                history_text += f"{i}. <code>{attempt_guess}</code> - 🐂{attempt_bulls} | 🐄{attempt_cows}\n"

        response = (
            f"🐂 <b>{bulls}</b> бык(а/ов) | 🐄 <b>{cows}</b> коров(а/ы)\n"
            f"{history_text}"
        )

        if hints:
            response += "\n" + "\n".join(hints)

        response += "\nПродолжай пытаться! 😊"

    await update.message.reply_text(response, parse_mode="HTML")


async def error_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик ошибок."""
    logger.error(f"Update {update} caused error {context.error}")
    if update and update.effective_message:
        await update.effective_message.reply_text(
            "😕 Произошла ошибка. Попробуй ещё раз или используй /help."
        )


def main() -> None:
    """Основная функция для запуска бота."""
    import sys

    # Получаем токен из аргументов командной строки
    if len(sys.argv) < 2:
        print("Использование: python telegram_bot.py <BOT_TOKEN>")
        print("Получите токен от @BotFather в Telegram")
        sys.exit(1)

    token = sys.argv[1]

    # Создаём приложение
    application = Application.builder().token(token).build()

    # Регистрируем обработчики команд
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("help", help_command))
    application.add_handler(CommandHandler("newgame", new_game))
    application.add_handler(CommandHandler("cancel", cancel_game))
    application.add_handler(CommandHandler("stats", show_stats))

    # Регистрируем обработчик текстовых сообщений (для попыток угадать)
    application.add_handler(
        MessageHandler(filters.TEXT & ~filters.COMMAND, handle_guess)
    )

    # Регистрируем обработчик ошибок
    application.add_error_handler(error_handler)

    # Запускаем бота
    logger.info("Бот запущен...")
    print("Бот запущен! Нажмите Ctrl+C для остановки.")
    application.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
