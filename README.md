# 📚 PYQ Bot — Previous Year Question Papers Bot

A Telegram bot for students (Class 9–12) to download previous year question papers via inline navigation or smart text search.

---

## 🚀 Setup

### 1. Clone & Navigate
```bash
git clone <your-repo-url>
cd bot
```

### 2. Create `.env`
```bash
cp .env.example .env
```
Edit `.env` and set your bot token:
```
BOT_TOKEN=your_telegram_bot_token_here
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

### 4. Run
```bash
python bot.py
```

---

## 📁 Project Structure

```
bot/
 ├── bot.py           # Main bot logic
 ├── data_loader.py   # JSON data access layer
 ├── config.py        # Environment config
 ├── data.json        # Question paper file_id database
 ├── requirements.txt
 ├── .env.example
 └── README.md
```

---

## 📄 How to Add Papers

1. Send the PDF file to your bot (or any chat, then forward to @userinfobot to get file_id).
2. Copy the `file_id` from Telegram's response.
3. Add an entry in `data.json`:

```json
{
  "class": "10",
  "subject": "math",
  "year": "2023",
  "file_id": "BQACAgIAAxkBAAI..."
}
```

> **Note:** `file_id` is permanent only if the file was sent via **your bot**. Upload PDFs through your bot to get valid persistent file_ids.

---

## 🔍 Smart Search

Users can type queries like:
```
class 10 math 2022
class 12 physics 2023
```

---

## ☁️ Render Deployment

1. Create a **Background Worker** service on Render.
2. Set **Start Command**: `python bot.py`
3. Add environment variable: `BOT_TOKEN=your_token`

---

## 📌 Subjects Available (Configurable via data.json)

| Class | Subjects |
|-------|----------|
| 9–10  | Math, Science, English, Hindi, Social |
| 11–12 | Physics, Chemistry, Math, Biology, English, Accountancy, Economics |
