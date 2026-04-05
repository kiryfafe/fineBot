"""
Telegram Bot для игры "Быки и Коровы"

Этот бот позволяет играть в классическую игру "Быки и Коровы".
Компьютер загадывает число, а игрок должен его угадать.

Для запуска:
1. Установите зависимости: pip install python-telegram-bot python-dotenv
2. Получите токен бота от @BotFather в Telegram
3. Создайте файл .env с переменными BOT_TOKEN и ADMIN_IDS
4. Запустите: python telegram_bot.py
"""

import random
import logging
import os
from typing import Dict, Tuple
from dotenv import load_dotenv

# Загружаем переменные окружения из файла .env
load_dotenv()

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


class MultiplayerGame:
    """Класс для хранения состояния мультиплеерной игры между двумя пользователями."""
    
    def __init__(self, player1_id: int, player2_id: int, number_length: int = 4):
        self.player1_id = player1_id
        self.player2_id = player2_id
        self.number_length = number_length
        # Числа игроков (изначально None, пока игроки не загадают)
        self.secret_number_p1 = None
        self.secret_number_p2 = None
        # Состояния для каждого игрока
        self.p1_attempts = 0
        self.p2_attempts = 0
        self.p1_history: list[Tuple[str, int, int]] = []  # Попытки игрока 1 угадать число игрока 2
        self.p2_history: list[Tuple[str, int, int]] = []  # Попытки игрока 2 угадать число игрока 1
        self.is_game_over = False
        self.winner = None  # 'player1', 'player2', 'draw'
        # Чей сейчас ход
        self.current_turn = None  # Будет установлен после того как оба загадают числа
        # Флаги готовности (кто уже загадал число)
        self.p1_ready = False  # Игрок 1 ещё не загадал число
        self.p2_ready = False  # Игрок 2 ещё не загадал число
        # Флаг начала игры (оба загадали числа)
        self.game_started = False


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
# Хранилище мультиплеерных игр (ключ - ID игры, значение - объект MultiplayerGame)
multiplayer_games: Dict[str, MultiplayerGame] = {}
# Маппинг user_id -> game_id для быстрого поиска активной мультиплеерной игры пользователя
user_to_mp_game: Dict[int, str] = {}


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /start."""
    user = update.effective_user
    user_id = user.id
    
    # Проверяем, есть ли активная игра (одиночная или мультиплеерная)
    if user_id in user_games and not user_games[user_id].is_game_over:
        # Если активна одиночная игра
        keyboard = [
            [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
            [InlineKeyboardButton("❌ Завершить игру", callback_data="cancel")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await update.message.reply_html(
            f"Привет, {user.mention_html()}! 🎮\n\n"
            f"У тебя уже есть активная игра!\n"
            f"Загадано число из <b>{user_games[user_id].number_length}</b> цифр.\n"
            f"Попыток сделано: {user_games[user_id].attempts}\n\n"
            "Отправь число, чтобы сделать ход, или выбери действие:",
            reply_markup=reply_markup
        )
    elif user_id in user_to_mp_game:
        # Если активна мультиплеерная игра
        game_id = user_to_mp_game[user_id]
        mp_game = multiplayer_games.get(game_id)
        if mp_game and not mp_game.is_game_over:
            is_player1 = (user_id == mp_game.player1_id)
            current_turn_text = "Твой ход!" if mp_game.current_turn == user_id else "Ход соперника."
            
            keyboard = [
                [InlineKeyboardButton("📊 Статистика", callback_data="mp_stats")],
                [InlineKeyboardButton("❌ Завершить игру", callback_data="mp_cancel")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            await update.message.reply_html(
                f"Привет, {user.mention_html()}! 🎲\n\n"
                f"У тебя активна мультиплеерная игра!\n"
                f"<b>Твоё число:</b> <code>{mp_game.secret_number_p1 if is_player1 else mp_game.secret_number_p2}</code>\n"
                f"Попыток сделано: {mp_game.p1_attempts if is_player1 else mp_game.p2_attempts}\n"
                f"<b>{current_turn_text}</b>\n\n"
                "Отправь число, чтобы сделать ход, или выбери действие:",
                reply_markup=reply_markup
            )
        else:
            # Игра завершена или не найдена, показываем главное меню
            await show_main_menu(update, context)
    else:
        # Если нет активной игры, показываем главное меню
        await show_main_menu(update, context)


async def show_main_menu(update_or_query, context, is_callback=False):
    """Показывает главное меню с выбором режима игры."""
    keyboard = [
        [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
        [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
        [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    text = (
        "🎮 <b>Главное меню</b>\n\n"
        "Выберите режим игры:\n"
        "• <b>Одиночная игра</b> - играй против компьютера\n"
        "• <b>Мультиплеер</b> - играй с другом\n"
        "• <b>Статистика</b> - посмотри свои результаты"
    )
    
    if is_callback:
        await update_or_query.edit_message_text(text, parse_mode="HTML", reply_markup=reply_markup)
    else:
        await update_or_query.message.reply_html(text, reply_markup=reply_markup)


async def button_callback(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик нажатий на кнопки."""
    query = update.callback_query
    user_id = query.from_user.id
    
    await query.answer()  # Подтверждаем получение callback
    
    data = query.data
    
    if data == "new_game":
        # Показываем кнопки для выбора количества цифр
        keyboard = [
            [InlineKeyboardButton("3 цифры", callback_data="length_3")],
            [InlineKeyboardButton("4 цифры", callback_data="length_4")],
            [InlineKeyboardButton("5 цифр", callback_data="length_5")],
            [InlineKeyboardButton("6 цифр", callback_data="length_6")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "🎮 <b>Новая игра</b>\n\n"
            "Выберите количество цифр в загадываемом числе:",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
    
    elif data.startswith("length_"):
        # Извлекаем длину числа
        number_length = int(data.split("_")[1])
        
        # Генерируем секретное число
        secret_number = generate_secret_number(number_length)
        
        # Создаём новое состояние игры
        user_games[user_id] = GameState(secret_number, number_length)
        
        await query.edit_message_text(
            f"🎮 <b>Новая игра началась!</b>\n\n"
            f"Я загадал число из <b>{number_length}</b> цифр.\n"
            f"Попробуй угадать его!\n\n"
            "Отправь число в чат, чтобы сделать ход.",
            parse_mode="HTML"
        )
        logger.info(f"User {user_id} started a new game with {number_length}-digit number via button")
    
    elif data == "stats":
        # Показываем статистику
        if user_id not in user_games:
            await query.edit_message_text(
                "У тебя нет активной игры.\n"
                "Используй кнопку «Новая игра», чтобы начать."
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
        
        # Добавляем кнопки навигации
        keyboard = []
        if not game.is_game_over:
            keyboard.append([InlineKeyboardButton("◀️ Назад к игре", callback_data="back_to_game")])
        
        reply_markup = InlineKeyboardMarkup(keyboard) if keyboard else None
        
        await query.edit_message_text(
            stats_text,
            parse_mode="HTML",
            reply_markup=reply_markup
        )
    
    elif data == "cancel":
        # Завершаем одиночную игру
        if user_id in user_games:
            del user_games[user_id]
            # Показываем главное меню с выбором режима
            keyboard = [
                [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
                [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
                [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            await query.edit_message_text(
                "❌ Игра завершена.\n\nВыберите режим игры:",
                reply_markup=reply_markup
            )
            logger.info(f"User {user_id} cancelled the game via button")
        else:
            # Если нет активной игры, показываем главное меню
            keyboard = [
                [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
                [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
                [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            await query.edit_message_text(
                "У тебя нет активной игры.\n\nВыберите режим игры:",
                reply_markup=reply_markup
            )
    
    elif data == "back_to_game":
        # Возвращаемся к сообщению с информацией об игре
        if user_id in user_games and not user_games[user_id].is_game_over:
            game = user_games[user_id]
            keyboard = [
                [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
                [InlineKeyboardButton("❌ Завершить игру", callback_data="cancel")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            await query.edit_message_text(
                f"🎮 <b>Активная игра</b>\n\n"
                f"Загадано число из <b>{game.number_length}</b> цифр.\n"
                f"Попыток сделано: {game.attempts}\n\n"
                "Отправь число в чат, чтобы сделать ход, или выбери действие:",
                parse_mode="HTML",
                reply_markup=reply_markup
            )
        else:
            await show_main_menu(query, context, is_callback=True)
    
    # Обработчик мультиплеерных callback-кнопок
    elif data == "main_menu":
        # Возврат в главное меню
        keyboard = [
            [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
            [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
            [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "🎮 <b>Главное меню</b>\n\n"
            "Выберите режим игры:",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        return
    
    elif data == "mp_menu":
        # Меню мультиплеера
        keyboard = [
            [InlineKeyboardButton("🎮 Создать комнату", callback_data="mp_create")],
            [InlineKeyboardButton("🔍 Найти игру", callback_data="mp_join")],
            [InlineKeyboardButton("◀️ Назад", callback_data="main_menu")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "🎲 <b>Мультиплеер</b>\n\n"
            "Выберите действие:\n"
            "• <b>Создать комнату</b> - создайте игру и отправьте ссылку другу\n"
            "• <b>Найти игру</b> - присоединитесь к существующей комнате по ID\n"
            "• <b>Назад</b> - вернуться в главное меню",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        return
    
    elif data == "mp_create":
        # Создание комнаты - выбор длины числа
        keyboard = [
            [InlineKeyboardButton("3 цифры", callback_data="mp_create_3")],
            [InlineKeyboardButton("4 цифры", callback_data="mp_create_4")],
            [InlineKeyboardButton("5 цифр", callback_data="mp_create_5")],
            [InlineKeyboardButton("6 цифр", callback_data="mp_create_6")],
            [InlineKeyboardButton("◀️ Назад", callback_data="mp_menu")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "🎲 <b>Создание комнаты</b>\n\n"
            "Выберите количество цифр в числе:",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        return
    
    elif data.startswith("mp_create_"):
        # Создаём новую комнату - игрок должен будет загадать число
        number_length = int(data.split("_")[2])
        game_id = f"mp_{user_id}_{random.randint(1000, 9999)}"
        
        # Сохраняем информацию о комнате
        mp_waiting_rooms[game_id] = {
            'creator': user_id,
            'length': number_length,
            'waiting': True,
            'player2': None
        }
        
        # Создаём объект игры (пока без второго игрока и без загаданного числа)
        mp_game = MultiplayerGame(user_id, 0, number_length)
        mp_game.p2_ready = False  # Второй игрок ещё не присоединился
        
        multiplayer_games[game_id] = mp_game
        user_to_mp_game[user_id] = game_id
        
        # Отправляем сообщение с ID комнаты и просим загадать число
        keyboard = [
            [InlineKeyboardButton("◀️ Отмена", callback_data="mp_cancel_create")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            f"🎲 <b>Комната создана!</b>\n\n"
            f"<b>ID комнаты:</b> <code>{game_id}</code>\n"
            f"<b>Длина числа:</b> {number_length}\n\n"
            "Отправь этот ID другу, чтобы он мог присоединиться.\n"
            "<b>Теперь загадай число из {number_length} цифр (без повторяющихся цифр) и отправь его в чат.</b>\n"
            "Как только друг подключится и оба загадаете числа, игра начнётся автоматически.",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        # Устанавливаем флаг ожидания загаданного числа от создателя
        context.user_data['waiting_for_mp_number'] = {'game_id': game_id, 'is_creator': True}
        logger.info(f"User {user_id} created multiplayer room {game_id}, waiting for number")
        return
    
    elif data == "mp_cancel_create":
        # Отмена создания комнаты
        # Ищем комнату пользователя и удаляем её
        rooms_to_delete = []
        for gid, room in mp_waiting_rooms.items():
            if room['creator'] == user_id and room['waiting']:
                rooms_to_delete.append(gid)
        
        for gid in rooms_to_delete:
            if gid in multiplayer_games:
                del multiplayer_games[gid]
            if gid in mp_waiting_rooms:
                del mp_waiting_rooms[gid]
            if user_id in user_to_mp_game:
                del user_to_mp_game[user_id]
        
        # Возвращаемся в меню мультиплеера
        keyboard = [
            [InlineKeyboardButton("🎮 Создать комнату", callback_data="mp_create")],
            [InlineKeyboardButton("🔍 Найти игру", callback_data="mp_join")],
            [InlineKeyboardButton("◀️ Назад", callback_data="main_menu")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "🎲 <b>Мультиплеер</b>\n\n"
            "Выберите действие:",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        return
    
    elif data == "mp_join":
        # Запрос ID комнаты для присоединения
        await query.edit_message_text(
            "🎲 <b>Присоединение к игре</b>\n\n"
            "Отправь мне ID комнаты, который тебе дал друг.\n"
            "ID выглядит как: <code>mp_123456_7890</code>\n\n"
            "Или нажми «Назад», чтобы отменить.",
            parse_mode="HTML",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("◀️ Назад", callback_data="mp_menu")]])
        )
        # Устанавливаем флаг, что пользователь хочет присоединиться
        context.user_data['waiting_for_room_id'] = True
        return
    
    elif data.startswith("mp_join_room_"):
        # Присоединение к конкретной комнате - игрок должен будет загадать число
        game_id = data.replace("mp_join_room_", "")
        
        if game_id not in mp_waiting_rooms or not mp_waiting_rooms[game_id]['waiting']:
            await query.edit_message_text("❌ Эта комната больше не существует или игра уже началась.")
            return
        
        room = mp_waiting_rooms[game_id]
        creator_id = room['creator']
        number_length = room['length']
        
        # Проверяем, не является ли пользователь уже частью этой игры
        if user_id == creator_id:
            await query.answer("❌ Ты не можешь присоединиться к своей собственной комнате!", show_alert=True)
            return
        
        # Обновляем комнату - добавляем второго игрока
        mp_game = multiplayer_games.get(game_id)
        if not mp_game:
            # Создаём объект игры если ещё не создан
            mp_game = MultiplayerGame(creator_id, user_id, number_length)
            multiplayer_games[game_id] = mp_game
        
        mp_game.player2_id = user_id
        mp_waiting_rooms[game_id]['waiting'] = False
        mp_waiting_rooms[game_id]['player2'] = user_id
        user_to_mp_game[user_id] = game_id
        
        # Отправляем сообщение второму игроку с просьбой загадать число
        keyboard2 = [
            [InlineKeyboardButton("◀️ Отмена", callback_data="mp_cancel")],
        ]
        reply_markup2 = InlineKeyboardMarkup(keyboard2)
        
        await query.edit_message_text(
            f"🎲 <b>Ты присоединился к игре!</b>\n\n"
            f"<b>ID комнаты:</b> <code>{game_id}</code>\n"
            f"<b>Длина числа:</b> {number_length}\n\n"
            f"<b>Теперь загадай число из {number_length} цифр (без повторяющихся цифр) и отправь его в чат.</b>\n"
            f"Как только оба игрока загадают числа, игра начнётся автоматически.",
            parse_mode="HTML",
            reply_markup=reply_markup2
        )
        # Устанавливаем флаг ожидания загаданного числа от второго игрока
        context.user_data['waiting_for_mp_number'] = {'game_id': game_id, 'is_creator': False}
        
        # Уведомляем создателя что игрок присоединился
        try:
            await context.bot.send_message(
                chat_id=creator_id,
                text=(
                    f"🎲 <b>Соперник присоединился!</b>\n\n"
                    f"Игрок готов к игре. Как только вы оба загадаете числа, игра начнётся.\n"
                    f"Если ты ещё не загадал число - отправь его в чат."
                ),
                parse_mode="HTML"
            )
        except Exception as e:
            logger.error(f"Failed to notify creator {creator_id}: {e}")
        
        logger.info(f"User {user_id} joined room {game_id}, waiting for numbers")
        return
    
    elif data == "mp_stats":
        # Статистика мультиплеерной игры
        if user_id not in user_to_mp_game:
            await query.edit_message_text("❌ У тебя нет активной мультиплеерной игры.")
            return
        
        game_id = user_to_mp_game[user_id]
        mp_game = multiplayer_games.get(game_id)
        if not mp_game:
            await query.edit_message_text("❌ Игра не найдена.")
            return
        
        is_player1 = (user_id == mp_game.player1_id)
        if is_player1:
            my_attempts = mp_game.p1_attempts
            my_history = mp_game.p1_history
            opponent_attempts = mp_game.p2_attempts
        else:
            my_attempts = mp_game.p2_attempts
            my_history = mp_game.p2_history
            opponent_attempts = mp_game.p1_attempts
        
        stats_text = (
            f"📊 <b>Статистика мультиплеерной игры</b>\n\n"
            f"Длина числа: {mp_game.number_length}\n"
            f"Твоих попыток: {my_attempts}\n"
            f"Попыток соперника: {opponent_attempts}\n\n"
        )
        
        if my_history:
            stats_text += "<b>Твои попытки:</b>\n"
            for i, (guess, bulls, cows) in enumerate(my_history[-10:], 1):
                stats_text += f"{i}. {guess} → 🐂{bulls} 🐄{cows}\n"
        else:
            stats_text += "Пока нет ходов."
        
        keyboard = [
            [InlineKeyboardButton("◀️ Назад к игре", callback_data="mp_back_to_game")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(stats_text, parse_mode="HTML", reply_markup=reply_markup)
        return
    
    elif data == "mp_back_to_game":
        # Возврат к информации об игре
        if user_id not in user_to_mp_game:
            await query.edit_message_text("❌ У тебя нет активной мультиплеерной игры.")
            return
        
        game_id = user_to_mp_game[user_id]
        mp_game = multiplayer_games.get(game_id)
        if not mp_game:
            await query.edit_message_text("❌ Игра не найдена.")
            return
        
        is_player1 = (user_id == mp_game.player1_id)
        current_turn_text = "Твой ход!" if mp_game.current_turn == user_id else "Ход соперника."
        
        keyboard = [
            [InlineKeyboardButton("📊 Статистика", callback_data="mp_stats")],
            [InlineKeyboardButton("❌ Завершить игру", callback_data="mp_cancel")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            f"🎲 <b>Активная игра</b>\n\n"
            f"<b>Твоё число:</b> <code>{mp_game.secret_number_p1 if is_player1 else mp_game.secret_number_p2}</code>\n"
            f"<b>Попыток сделано:</b> {mp_game.p1_attempts if is_player1 else mp_game.p2_attempts}\n"
            f"<b>{current_turn_text}</b>\n\n"
            "Отправь число, чтобы сделать ход.",
            parse_mode="HTML",
            reply_markup=reply_markup
        )
        return
    
    elif data == "mp_cancel":
        # Завершение мультиплеерной игры
        if user_id not in user_to_mp_game:
            await query.edit_message_text("❌ У тебя нет активной мультиплеерной игры.")
            return
        
        game_id = user_to_mp_game[user_id]
        mp_game = multiplayer_games.get(game_id)
        if not mp_game:
            await query.edit_message_text("❌ Игра не найдена.")
            return
        
        # Удаляем игру
        player1_id = mp_game.player1_id
        player2_id = mp_game.player2_id
        
        if game_id in multiplayer_games:
            del multiplayer_games[game_id]
        if user_id in user_to_mp_game:
            del user_to_mp_game[user_id]
        if player1_id in user_to_mp_game:
            del user_to_mp_game[player1_id]
        if player2_id in user_to_mp_game:
            del user_to_mp_game[player2_id]
        
        # Показываем меню выбора режима игры
        keyboard = [
            [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
            [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
            [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        await query.edit_message_text(
            "❌ Игра завершена.\n\nВыберите режим игры:",
            reply_markup=reply_markup
        )
        
        # Уведомляем второго игрока (если это был один из игроков)
        other_player = player2_id if user_id == player1_id else player1_id
        if other_player:
            try:
                await context.bot.send_message(
                    chat_id=other_player,
                    text="❌ Соперник завершил игру.\n\nВыберите режим игры:",
                    reply_markup=reply_markup
                )
            except Exception as e:
                logger.error(f"Failed to notify other player {other_player}: {e}")
        
        logger.info(f"User {user_id} cancelled multiplayer game {game_id}")
        return


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /help."""
    await update.message.reply_text(
        "📖 <b>Справка по игре «Быки и Коровы»</b>\n\n"
        "<b>Как играть:</b>\n"
        "1. Выбери режим игры: одиночный или мультиплеер\n"
        "2. Загадай число или отгадывай число противника\n"
        "3. Отправь число как попытку угадать\n"
        "4. Получи ответ: 🐂 Быки (цифры на правильных местах), 🐄 Коровы (правильные цифры не на своих местах)\n"
        "5. Продолжай, пока не угадаешь всё число!\n\n"
        "<b>Пример:</b>\n"
        "Загадано: 5189\n"
        "Твоя попытка: 5281\n"
        "Ответ: 🐂 2 🐄 2 (5 и 8 на правильных местах, 1 и 2 есть, но не там)\n\n"
        "<b>Команды:</b>\n"
        "/start - Запустить бота и показать главное меню\n"
        "/newgame - Начать новую одиночную игру\n"
        "/multiplayer - Найти соперника для игры вдвоем\n"
        "/stats - Показать вашу статистику игр\n"
        "/cancel - Завершить текущую игру и вернуться в меню\n"
        "/restart - Перезагрузить бота (доступно только администратору)\n"
        "/help - Показать эту справку\n\n"
        "<b>Сделано Кириллом Г 🐱</b>",
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
        # Показываем сообщение о завершении игры с кнопкой "Новая игра"
        keyboard = [
            [InlineKeyboardButton("🎮 Новая игра", callback_data="new_game")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await update.message.reply_text(
            "❌ Игра завершена. Используй /newgame, чтобы начать заново.",
            reply_markup=reply_markup
        )
        logger.info(f"User {user_id} cancelled the game")
    else:
        await update.message.reply_text(
            "У тебя нет активной игры.\nИспользуй /newgame, чтобы начать."
        )


async def show_stats(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /stats."""
    user_id = update.effective_user.id

    # Проверяем одиночную игру
    if user_id in user_games:
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
        return

    # Проверяем мультиплеерную игру
    if user_id in user_to_mp_game:
        game_id = user_to_mp_game[user_id]
        mp_game = multiplayer_games.get(game_id)
        if mp_game:
            # Определяем, какой это игрок
            is_player1 = (user_id == mp_game.player1_id)
            if is_player1:
                opponent_attempts = mp_game.p2_attempts
                opponent_history = mp_game.p2_history
                secret_number = mp_game.secret_number_p1
            else:
                opponent_attempts = mp_game.p1_attempts
                opponent_history = mp_game.p1_history
                secret_number = mp_game.secret_number_p2
            
            stats_text = (
                f"📊 <b>Статистика мультиплеерной игры</b>\n\n"
                f"Длина числа: {mp_game.number_length}\n"
                f"Твоих попыток: {mp_game.p1_attempts if is_player1 else mp_game.p2_attempts}\n"
                f"Попыток соперника: {opponent_attempts}\n\n"
            )

            if is_player1 and mp_game.p1_history:
                stats_text += "<b>Твои попытки угадать число соперника:</b>\n"
                for i, (guess, bulls, cows) in enumerate(mp_game.p1_history[-10:], 1):
                    stats_text += f"{i}. {guess} → 🐂{bulls} 🐄{cows}\n"
            elif not is_player1 and mp_game.p2_history:
                stats_text += "<b>Твои попытки угадать число соперника:</b>\n"
                for i, (guess, bulls, cows) in enumerate(mp_game.p2_history[-10:], 1):
                    stats_text += f"{i}. {guess} → 🐂{bulls} 🐄{cows}\n"
            else:
                stats_text += "Пока нет ходов. Сделай первый ход!"

            await update.message.reply_text(stats_text, parse_mode="HTML")
            return

    await update.message.reply_text(
        "У тебя нет активной игры.\nИспользуй /newgame для одиночной игры или /multiplayer для игры с другом."
    )


async def restart_bot(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /restart - перезагрузка бота (только для администратора)."""
    # Получаем ID администратора из переменных окружения или хардкодим
    # В продакшене лучше использовать переменные окружения
    admin_ids = [int(x) for x in __import__('os').environ.get('ADMIN_IDS', '').split(',') if x.isdigit()]
    
    user_id = update.effective_user.id
    if user_id not in admin_ids:
        await update.message.reply_text("❌ Эта команда доступна только администратору.")
        return
    
    await update.message.reply_text("🔄 Бот перезагружается...")
    logger.info(f"Bot restarted by user {user_id}")
    import sys
    import os
    os.execv(sys.executable, ['python'] + sys.argv)


async def multiplayer_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик команды /multiplayer - поиск соперника для игры."""
    user_id = update.effective_user.id
    
    # Проверяем, нет ли уже активной игры (одиночной или мультиплеерной)
    if user_id in user_games and not user_games[user_id].is_game_over:
        await update.message.reply_text(
            "❌ У тебя уже есть активная одиночная игра!\n"
            "Используй /cancel, чтобы завершить её, прежде чем начинать мультиплеер."
        )
        return
    
    if user_id in user_to_mp_game:
        game_id = user_to_mp_game[user_id]
        if game_id in multiplayer_games:
            await update.message.reply_text(
                "❌ У тебя уже есть активная мультиплеерная игра!\n"
                "Используй /cancel, чтобы завершить её."
            )
            return
    
    # Показываем меню выбора режима для мультиплеера
    keyboard = [
        [InlineKeyboardButton("🎮 Создать комнату", callback_data="mp_create")],
        [InlineKeyboardButton("🔍 Найти игру", callback_data="mp_join")],
        [InlineKeyboardButton("◀️ Назад", callback_data="main_menu")],
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    await update.message.reply_text(
        "🎲 <b>Мультиплеер</b>\n\n"
        "Выберите действие:\n"
        "• <b>Создать комнату</b> - создайте игру и отправьте ссылку другу\n"
        "• <b>Найти игру</b> - присоединитесь к существующей комнате по ID\n"
        "• <b>Назад</b> - вернуться в главное меню",
        parse_mode="HTML",
        reply_markup=reply_markup
    )


# Глобальный словарь для ожидания игроков (для упрощённой реализации)
# Ключ: game_id, Значение: {'creator': user_id, 'length': int, 'waiting': bool}
mp_waiting_rooms: Dict[str, dict] = {}


async def handle_mp_guess(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик попыток в мультиплеерной игре."""
    user_id = update.effective_user.id
    guess = update.message.text.strip()
    
    # Проверяем, есть ли активная мультиплеерная игра
    if user_id not in user_to_mp_game:
        # Это может быть одиночная игра или вообще нет игры
        return  # Обработчик handle_guess обработает это
    
    game_id = user_to_mp_game[user_id]
    mp_game = multiplayer_games.get(game_id)
    
    if not mp_game or mp_game.is_game_over:
        return
    
    # Проверяем, чей сейчас ход
    if mp_game.current_turn != user_id:
        await update.message.reply_text(
            "⏳ Сейчас не твой ход! Дождись хода соперника."
        )
        return
    
    # Валидация ввода
    if not guess.isdigit():
        await update.message.reply_text(
            "❌ Пожалуйста, отправь только цифры (без пробелов и других символов)."
        )
        return
    
    if len(guess) != mp_game.number_length:
        await update.message.reply_text(
            f"❌ Число должно содержать ровно {mp_game.number_length} цифр."
        )
        return
    
    if len(set(guess)) != len(guess):
        await update.message.reply_text(
            "❌ Цифры в числе не должны повторяться."
        )
        return
    
    # Определяем, какое число угадываем
    is_player1 = (user_id == mp_game.player1_id)
    if is_player1:
        # Игрок 1 угадывает число игрока 2
        secret = mp_game.secret_number_p2
        mp_game.p1_attempts += 1
        bulls, cows = evaluate_guess(secret, guess)
        mp_game.p1_history.append((guess, bulls, cows))
        
        # Проверяем победу
        if bulls == mp_game.number_length:
            # Игрок 1 выиграл
            mp_game.is_game_over = True
            mp_game.winner = 'player1'
            
            history_text = ""
            if mp_game.p1_history:
                history_text = "\n\n<b>Твои попытки:</b>\n"
                for i, (g, b, c) in enumerate(mp_game.p1_history, 1):
                    history_text += f"{i}. <code>{g}</code> - 🐂{b} | 🐄{c}\n"
            
            response = (
                f"🎉 <b>Поздравляю! Ты выиграл!</b> 🎉\n\n"
                f"Число соперника: <code>{secret}</code>\n"
                f"Количество попыток: <b>{mp_game.p1_attempts}</b>"
                f"{history_text}"
            )
            
            # Отправляем сообщение обоим игрокам
            keyboard = [
                [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
                [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
                [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            await update.message.reply_text(response, parse_mode="HTML")
            
            # Сообщаем проигравшему
            try:
                await context.bot.send_message(
                    chat_id=mp_game.player2_id,
                    text=(
                        f"❌ Ты проиграл!\n\n"
                        f"Соперник угадал твоё число: <code>{secret}</code>\n"
                        f"Количество попыток соперника: <b>{mp_game.p1_attempts}</b>\n\n"
                        "Хочешь сыграть ещё?"
                    ),
                    parse_mode="HTML",
                    reply_markup=reply_markup
                )
            except Exception as e:
                logger.error(f"Failed to send lose message to player2: {e}")
            
            # Очищаем игру
            del multiplayer_games[game_id]
            del user_to_mp_game[mp_game.player1_id]
            del user_to_mp_game[mp_game.player2_id]
            return
        
        # Ход переходит ко второму игроку
        mp_game.current_turn = mp_game.player2_id
        
        # Формируем ответ для игрока 1
        history_text = ""
        if mp_game.p1_history:
            history_text = "\n\n<b>История твоих попыток:</b>\n"
            for i, (g, b, c) in enumerate(mp_game.p1_history[-10:], 1):
                history_text += f"{i}. <code>{g}</code> - 🐂{b} | 🐄{c}\n"
        
        response = (
            f"🐂 <b>{bulls}</b> бык(а/ов) | 🐄 <b>{cows}</b> коров(а/ы)\n"
            f"{history_text}\n"
            "Ход передан сопернику. Ожидай его хода..."
        )
        
        await update.message.reply_text(response, parse_mode="HTML")
        
        # Уведомляем игрока 2
        try:
            await context.bot.send_message(
                chat_id=mp_game.player2_id,
                text=(
                    f"🎲 <b>Твой ход!</b>\n\n"
                    f"Соперник сделал попытку: <code>{guess}</code>\n"
                    f"Результат: 🐂{bulls} 🐄{cows}\n\n"
                    f"Отправь число из {mp_game.number_length} цифр, чтобы угадать число соперника."
                ),
                parse_mode="HTML"
            )
        except Exception as e:
            logger.error(f"Failed to notify player2: {e}")
        
    else:
        # Игрок 2 угадывает число игрока 1
        secret = mp_game.secret_number_p1
        mp_game.p2_attempts += 1
        bulls, cows = evaluate_guess(secret, guess)
        mp_game.p2_history.append((guess, bulls, cows))
        
        # Проверяем победу
        if bulls == mp_game.number_length:
            # Игрок 2 выиграл
            mp_game.is_game_over = True
            mp_game.winner = 'player2'
            
            history_text = ""
            if mp_game.p2_history:
                history_text = "\n\n<b>Твои попытки:</b>\n"
                for i, (g, b, c) in enumerate(mp_game.p2_history, 1):
                    history_text += f"{i}. <code>{g}</code> - 🐂{b} | 🐄{c}\n"
            
            response = (
                f"🎉 <b>Поздравляю! Ты выиграл!</b> 🎉\n\n"
                f"Число соперника: <code>{secret}</code>\n"
                f"Количество попыток: <b>{mp_game.p2_attempts}</b>"
                f"{history_text}"
            )
            
            # Отправляем сообщение обоим игрокам
            keyboard = [
                [InlineKeyboardButton("🎮 Одиночная игра", callback_data="new_game")],
                [InlineKeyboardButton("👥 Мультиплеер", callback_data="mp_menu")],
                [InlineKeyboardButton("📊 Статистика", callback_data="stats")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            await update.message.reply_text(response, parse_mode="HTML")
            
            # Сообщаем проигравшему
            try:
                await context.bot.send_message(
                    chat_id=mp_game.player1_id,
                    text=(
                        f"❌ Ты проиграл!\n\n"
                        f"Соперник угадал твоё число: <code>{secret}</code>\n"
                        f"Количество попыток соперника: <b>{mp_game.p2_attempts}</b>\n\n"
                        "Хочешь сыграть ещё?"
                    ),
                    parse_mode="HTML",
                    reply_markup=reply_markup
                )
            except Exception as e:
                logger.error(f"Failed to send lose message to player1: {e}")
            
            # Очищаем игру
            del multiplayer_games[game_id]
            del user_to_mp_game[mp_game.player1_id]
            del user_to_mp_game[mp_game.player2_id]
            return
        
        # Ход переходит к первому игроку
        mp_game.current_turn = mp_game.player1_id
        
        # Формируем ответ для игрока 2
        history_text = ""
        if mp_game.p2_history:
            history_text = "\n\n<b>История твоих попыток:</b>\n"
            for i, (g, b, c) in enumerate(mp_game.p2_history[-10:], 1):
                history_text += f"{i}. <code>{g}</code> - 🐂{b} | 🐄{c}\n"
        
        response = (
            f"🐂 <b>{bulls}</b> бык(а/ов) | 🐄 <b>{cows}</b> коров(а/ы)\n"
            f"{history_text}\n"
            "Ход передан сопернику. Ожидай его хода..."
        )
        
        await update.message.reply_text(response, parse_mode="HTML")
        
        # Уведомляем игрока 1
        try:
            await context.bot.send_message(
                chat_id=mp_game.player1_id,
                text=(
                    f"🎲 <b>Твой ход!</b>\n\n"
                    f"Соперник сделал попытку: <code>{guess}</code>\n"
                    f"Результат: 🐂{bulls} 🐄{cows}\n\n"
                    f"Отправь число из {mp_game.number_length} цифр, чтобы угадать число соперника."
                ),
                parse_mode="HTML"
            )
        except Exception as e:
            logger.error(f"Failed to notify player1: {e}")


async def handle_room_id_input(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик ввода ID комнаты для присоединения."""
    user_id = update.effective_user.id
    
    # Проверяем, ждёт ли пользователь ввода ID комнаты
    if not context.user_data.get('waiting_for_room_id'):
        return
    
    room_id = update.message.text.strip()
    
    # Проверяем существование комнаты
    if room_id not in mp_waiting_rooms or not mp_waiting_rooms[room_id]['waiting']:
        await update.message.reply_text(
            "❌ Комната с таким ID не найдена или игра уже началась.\n"
            "Проверь ID и попробуй снова."
        )
        context.user_data['waiting_for_room_id'] = False
        return
    
    room = mp_waiting_rooms[room_id]
    creator_id = room['creator']
    
    # Проверяем, не является ли пользователь создателем
    if user_id == creator_id:
        await update.message.reply_text("❌ Ты не можешь присоединиться к своей собственной комнате!")
        context.user_data['waiting_for_room_id'] = False
        return
    
    # Присоединяемся к комнате
    number_length = room['length']
    
    # Создаём полноценную игру
    mp_game = MultiplayerGame(creator_id, user_id, number_length)
    mp_game.secret_number_p2 = generate_secret_number(number_length)
    mp_game.p2_ready = True
    
    multiplayer_games[room_id] = mp_game
    user_to_mp_game[creator_id] = room_id
    user_to_mp_game[user_id] = room_id
    mp_waiting_rooms[room_id]['waiting'] = False
    mp_waiting_rooms[room_id]['player2'] = user_id
    
    context.user_data['waiting_for_room_id'] = False
    
    # Сообщаем обоим игрокам о начале игры
    keyboard = [
        [InlineKeyboardButton("📊 Статистика", callback_data="mp_stats")],
        [InlineKeyboardButton("❌ Завершить игру", callback_data="mp_cancel")],
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    # Отправляем сообщение создателю
    try:
        await context.bot.send_message(
            chat_id=creator_id,
            text=(
                f"🎲 <b>Игра началась!</b>\n\n"
                f"Твой соперник присоединился!\n"
                f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p1}</code>\n"
                f"<b>Число соперника:</b> ???\n\n"
                f"Сейчас твой ход! Отправь число из {number_length} цифр, чтобы угадать число соперника.",
            ),
            parse_mode="HTML",
            reply_markup=reply_markup
        )
    except Exception as e:
        logger.error(f"Failed to send message to creator {creator_id}: {e}")
    
    # Отправляем сообщение второму игроку
    await update.message.reply_text(
        f"🎲 <b>Игра началась!</b>\n\n"
        f"Ты присоединился к игре!\n"
        f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p2}</code>\n"
        f"<b>Число соперника:</b> ???\n\n"
        f"Сейчас ход создателя комнаты. Как только он сделает ход, ты получишь уведомление.",
        parse_mode="HTML",
        reply_markup=reply_markup
    )
    
    logger.info(f"User {user_id} joined room {room_id} via text input, game started")


async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Универсальный обработчик текстовых сообщений."""
    user_id = update.effective_user.id
    text = update.message.text.strip()
    
    # Сначала проверяем, не вводит ли пользователь ID комнаты для мультиплеера
    if context.user_data.get('waiting_for_room_id'):
        await handle_room_id_input(update, context)
        return
    
    # Проверяем, не вводит ли игрок своё число для мультиплеерной игры
    if context.user_data.get('waiting_for_mp_number'):
        await handle_mp_number_input(update, context)
        return
    
    # Затем проверяем мультиплеерную игру
    if user_id in user_to_mp_game:
        await handle_mp_guess(update, context)
        return
    
    # Наконец, проверяем одиночную игру
    await handle_guess(update, context)


async def handle_mp_number_input(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Обработчик ввода загаданного числа для мультиплеерной игры."""
    user_id = update.effective_user.id
    guess = update.message.text.strip()
    
    mp_data = context.user_data.get('waiting_for_mp_number')
    if not mp_data:
        return
    
    game_id = mp_data['game_id']
    is_creator = mp_data['is_creator']
    
    mp_game = multiplayer_games.get(game_id)
    if not mp_game:
        await update.message.reply_text("❌ Игра не найдена. Попробуй создать комнату заново.")
        del context.user_data['waiting_for_mp_number']
        return
    
    number_length = mp_game.number_length
    
    # Валидация ввода
    if not guess.isdigit():
        await update.message.reply_text(
            "❌ Пожалуйста, отправь только цифры (без пробелов и других символов)."
        )
        return
    
    if len(guess) != number_length:
        await update.message.reply_text(
            f"❌ Число должно содержать ровно {number_length} цифр."
        )
        return
    
    if len(set(guess)) != len(guess):
        await update.message.reply_text(
            "❌ Цифры в числе не должны повторяться."
        )
        return
    
    # Сохраняем загаданное число
    if is_creator:
        mp_game.secret_number_p1 = guess
        mp_game.p1_ready = True
        other_ready = mp_game.p2_ready
        other_id = mp_game.player2_id
    else:
        mp_game.secret_number_p2 = guess
        mp_game.p2_ready = True
        other_ready = mp_game.p1_ready
        other_id = mp_game.player1_id
    
    # Удаляем флаг ожидания
    del context.user_data['waiting_for_mp_number']
    
    # Проверяем, готовы ли оба игрока
    if mp_game.p1_ready and mp_game.p2_ready and not mp_game.game_started:
        # Оба игрока загадали числа - начинаем игру
        mp_game.game_started = True
        mp_game.current_turn = mp_game.player1_id  # Начинает создатель
        
        # Отправляем сообщения обоим игрокам
        keyboard = [
            [InlineKeyboardButton("📊 Статистика", callback_data="mp_stats")],
            [InlineKeyboardButton("❌ Завершить игру", callback_data="mp_cancel")],
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        
        # Сообщение создателю (игрок 1)
        try:
            await context.bot.send_message(
                chat_id=mp_game.player1_id,
                text=(
                    f"🎲 <b>Игра началась!</b>\n\n"
                    f"Твой соперник присоединился!\n"
                    f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p1}</code>\n"
                    f"<b>Число соперника:</b> ???\n\n"
                    f"Сейчас твой ход! Отправь число из {number_length} цифр, чтобы угадать число соперника.",
                ),
                parse_mode="HTML",
                reply_markup=reply_markup
            )
        except Exception as e:
            logger.error(f"Failed to send start message to player1: {e}")
        
        # Сообщение второму игроку
        try:
            await context.bot.send_message(
                chat_id=mp_game.player2_id,
                text=(
                    f"🎲 <b>Игра началась!</b>\n\n"
                    f"Ты присоединился к игре!\n"
                    f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p2}</code>\n"
                    f"<b>Число соперника:</b> ???\n\n"
                    f"Сейчас ход создателя комнаты. Как только он сделает ход, ты получишь уведомление.",
                ),
                parse_mode="HTML",
                reply_markup=reply_markup
            )
        except Exception as e:
            logger.error(f"Failed to send start message to player2: {e}")
        
        logger.info(f"Multiplayer game {game_id} started, both players ready")
        
    elif other_ready:
        # Другой игрок уже готов, ждём этого
        await update.message.reply_text(
            f"✅ <b>Число принято!</b>\n\n"
            f"Твоё число: <code>{guess}</code>\n"
            f"Соперник тоже уже загадал число.\n"
            f"Как только игра начнётся, ты получишь уведомление.",
            parse_mode="HTML"
        )
        
        # Если это второй игрок присоединился и оба готовы, но игра ещё не стартовала
        if not mp_game.game_started and is_creator == False:
            mp_game.game_started = True
            mp_game.current_turn = mp_game.player1_id
            
            keyboard = [
                [InlineKeyboardButton("📊 Статистика", callback_data="mp_stats")],
                [InlineKeyboardButton("❌ Завершить игру", callback_data="mp_cancel")],
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)
            
            try:
                await context.bot.send_message(
                    chat_id=mp_game.player1_id,
                    text=(
                        f"🎲 <b>Игра началась!</b>\n\n"
                        f"Твой соперник присоединился!\n"
                        f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p1}</code>\n"
                        f"<b>Число соперника:</b> ???\n\n"
                        f"Сейчас твой ход! Отправь число из {number_length} цифр, чтобы угадать число соперника.",
                    ),
                    parse_mode="HTML",
                    reply_markup=reply_markup
                )
            except Exception as e:
                logger.error(f"Failed to send start message to player1: {e}")
            
            await context.bot.send_message(
                chat_id=mp_game.player2_id,
                text=(
                    f"🎲 <b>Игра началась!</b>\n\n"
                    f"Ты присоединился к игре!\n"
                    f"<b>Твоё загаданное число:</b> <code>{mp_game.secret_number_p2}</code>\n"
                    f"<b>Число соперника:</b> ???\n\n"
                    f"Сейчас ход создателя комнаты. Как только он сделает ход, ты получишь уведомление.",
                ),
                parse_mode="HTML",
                reply_markup=reply_markup
            )
            
            logger.info(f"Multiplayer game {game_id} started immediately")
    else:
        # Ждём второго игрока
        await update.message.reply_text(
            f"✅ <b>Число принято!</b>\n\n"
            f"Твоё число: <code>{guess}</code>\n"
            f"Ожидаем, пока соперник загадает своё число...\n"
            f"Как только оба будете готовы, игра начнётся автоматически.",
            parse_mode="HTML"
        )
        logger.info(f"Player {user_id} set number in game {game_id}, waiting for opponent")


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

    # Получаем токен из переменной окружения или аргументов командной строки
    token = os.getenv("BOT_TOKEN")
    
    if not token:
        if len(sys.argv) >= 2:
            token = sys.argv[1]
            print("Внимание: Токен получен из аргументов командной строки. Рекомендуется использовать файл .env")
        else:
            print("Использование: python telegram_bot.py [BOT_TOKEN]")
            print("Или создайте файл .env с переменной BOT_TOKEN")
            print("Получите токен от @BotFather в Telegram")
            sys.exit(1)

    # Создаём приложение
    application = Application.builder().token(token).build()

    # Регистрируем обработчики команд
    application.add_handler(CommandHandler("start", start))
    application.add_handler(CommandHandler("help", help_command))
    application.add_handler(CommandHandler("newgame", new_game))
    application.add_handler(CommandHandler("cancel", cancel_game))
    application.add_handler(CommandHandler("stats", show_stats))
    application.add_handler(CommandHandler("restart", restart_bot))
    application.add_handler(CommandHandler("multiplayer", multiplayer_command))

    # Регистрируем обработчик callback-запросов (для кнопок)
    application.add_handler(CallbackQueryHandler(button_callback))

    # Регистрируем обработчик текстовых сообщений (для попыток угадать и ввода ID комнаты)
    application.add_handler(
        MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message)
    )

    # Регистрируем обработчик ошибок
    application.add_error_handler(error_handler)

    # Запускаем бота
    logger.info("Бот запущен...")
    print("Бот запущен! Нажмите Ctrl+C для остановки.")
    application.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    main()
