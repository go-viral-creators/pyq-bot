import logging
import re
import json
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


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    keyboard = []
    classes = get_classes()
    row = []
    for i, cls in enumerate(classes):
        row.append(InlineKeyboardButton(f"Class {cls}", callback_data=f"class:{cls}"))
        if (i + 1) % 2 == 0:
            keyboard.append(row)
            row = []
    if row:
        keyboard.append(row)

    reply_markup = InlineKeyboardMarkup(keyboard)
    text = (
        "📚 *Previous Year Question Papers*\n\n"
        "Select your class to get started, or type a query like:\n"
        "`class 10 math 2022`"
    )
    if update.message:
        await update.message.reply_text(text, parse_mode="Markdown", reply_markup=reply_markup)
    else:
        await update.callback_query.edit_message_text(text, parse_mode="Markdown", reply_markup=reply_markup)


async def handle_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    data = query.data

    if data.startswith("class:"):
        cls = data.split(":")[1]
        subjects = get_subjects(cls)
        if not subjects:
            await query.edit_message_text("❌ No subjects found for this class.")
            return
        keyboard = []
        row = []
        for i, sub in enumerate(subjects):
            row.append(InlineKeyboardButton(sub.capitalize(), callback_data=f"subject:{cls}:{sub}"))
            if (i + 1) % 2 == 0:
                keyboard.append(row)
                row = []
        if row:
            keyboard.append(row)
        keyboard.append([InlineKeyboardButton("⬅️ Back", callback_data="back:start")])
        await query.edit_message_text(
            f"📖 *Class {cls}* — Select Subject:",
            parse_mode="Markdown",
            reply_markup=InlineKeyboardMarkup(keyboard),
        )

    elif data.startswith("subject:"):
        _, cls, subject = data.split(":")
        years = get_years(cls, subject)
        if not years:
            await query.edit_message_text("❌ No years found for this subject.")
            return
        keyboard = []
        row = []
        for i, year in enumerate(sorted(years, reverse=True)):
            row.append(InlineKeyboardButton(year, callback_data=f"paper:{cls}:{subject}:{year}"))
            if (i + 1) % 3 == 0:
                keyboard.append(row)
                row = []
        if row:
            keyboard.append(row)
        keyboard.append([InlineKeyboardButton("⬅️ Back", callback_data=f"class:{cls}")])
        await query.edit_message_text(
            f"📅 *Class {cls} — {subject.capitalize()}*\nSelect Year:",
            parse_mode="Markdown",
            reply_markup=InlineKeyboardMarkup(keyboard),
        )

    elif data.startswith("paper:"):
        _, cls, subject, year = data.split(":")
        paper = find_paper(cls, subject, year)
        if not paper:
            await query.edit_message_text(
                f"❌ Paper not found for Class {cls} | {subject.capitalize()} | {year}.\n"
                "It may not be uploaded yet.",
                reply_markup=InlineKeyboardMarkup(
                    [[InlineKeyboardButton("⬅️ Back", callback_data=f"subject:{cls}:{subject}")]]
                ),
            )
            return
        caption = f"📄 *Class {cls} — {subject.capitalize()} ({year})*\n\nPrevious Year Question Paper"
        await query.message.reply_document(
            document=paper["file_id"],
            caption=caption,
            parse_mode="Markdown",
        )
        await query.edit_message_text(
            f"✅ Sent: Class {cls} {subject.capitalize()} {year}",
            reply_markup=InlineKeyboardMarkup(
                [[InlineKeyboardButton("🏠 Main Menu", callback_data="back:start")]]
            ),
        )

    elif data == "back:start":
        await start(update, context)


async def handle_text(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text.strip().lower()
    pattern = r"class\s*(\d+)\s+(\w+)\s+(\d{4})"
    match = re.search(pattern, text)

    if not match:
        await update.message.reply_text(
            "🤔 I didn't understand that.\n\n"
            "Try: `class 10 math 2022`\n"
            "Or use /start to browse.",
            parse_mode="Markdown",
        )
        return

    cls, subject, year = match.group(1), match.group(2), match.group(3)
    paper = find_paper(cls, subject, year)

    if not paper:
        await update.message.reply_text(
            f"❌ No paper found for:\n"
            f"• Class: {cls}\n• Subject: {subject.capitalize()}\n• Year: {year}\n\n"
            "Use /start to browse available papers.",
        )
        return

    caption = f"📄 *Class {cls} — {subject.capitalize()} ({year})*\n\nPrevious Year Question Paper"
    await update.message.reply_document(
        document=paper["file_id"],
        caption=caption,
        parse_mode="Markdown",
    )


async def error_handler(update: object, context: ContextTypes.DEFAULT_TYPE):
    logger.error("Exception while handling update:", exc_info=context.error)
    if isinstance(update, Update) and update.effective_message:
        await update.effective_message.reply_text(
            "⚠️ Something went wrong. Please try again or use /start."
        )


def main():
    app = Application.builder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text))
    app.add_error_handler(error_handler)
    logger.info("Bot is running...")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
