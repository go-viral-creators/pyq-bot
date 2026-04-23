import logging
import re
import json
import threading
import os
from flask import Flask

from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application,
    CommandHandler,
    CallbackQueryHandler,
    MessageHandler,
    filters,
    ContextTypes,
)

from config import BOT_TOKEN
from data_loader import load_papers, get_classes, get_subjects, get_years, find_paper

logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    level=logging.INFO,
)
logger = logging.getLogger(__name__)

# ---------------- BOT LOGIC ---------------- #

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    keyboard = []
    classes = get_classes()

    for cls in classes:
        keyboard.append([InlineKeyboardButton(f"Class {cls}", callback_data=f"class:{cls}")])

    await update.message.reply_text(
        "📚 Select Class:",
        reply_markup=InlineKeyboardMarkup(keyboard)
    )


async def handle_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()

    data = query.data

    if data.startswith("class:"):
        cls = data.split(":")[1]
        subjects = get_subjects(cls)

        keyboard = [
            [InlineKeyboardButton(sub.capitalize(), callback_data=f"subject:{cls}:{sub}")]
            for sub in subjects
        ]

        await query.edit_message_text(
            "Select Subject:",
            reply_markup=InlineKeyboardMarkup(keyboard)
        )

    elif data.startswith("subject:"):
        _, cls, subject = data.split(":")
        years = get_years(cls, subject)

        keyboard = [
            [InlineKeyboardButton(year, callback_data=f"paper:{cls}:{subject}:{year}")]
            for year in years
        ]

        await query.edit_message_text(
            "Select Year:",
            reply_markup=InlineKeyboardMarkup(keyboard)
        )

    elif data.startswith("paper:"):
        _, cls, subject, year = data.split(":")
        paper = find_paper(cls, subject, year)

        if not paper:
            await query.message.reply_text("❌ Paper not found")
            return

        await query.message.reply_document(paper["file_id"])


async def handle_text(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text.lower()

    pattern = r"class\s*(\d+)\s+(\w+)\s+(\d{4})"
    match = re.search(pattern, text)

    if not match:
        await update.message.reply_text("❌ Format: class 10 math 2022")
        return

    cls, subject, year = match.groups()

    paper = find_paper(cls, subject, year)

    if not paper:
        await update.message.reply_text("❌ Not found")
        return

    await update.message.reply_document(paper["file_id"])


def run_bot():
    app = Application.builder().token(BOT_TOKEN).build()

    app.add_handler(CommandHandler("start", start))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text))

    logger.info("Bot started...")
    app.run_polling()


# ---------------- WEB SERVER ---------------- #

flask_app = Flask(__name__)

@flask_app.route("/")
def home():
    return "Bot is running"


if __name__ == "__main__":
    threading.Thread(target=run_bot).start()
    flask_app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 10000)))
