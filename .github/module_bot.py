from telethon import TelegramClient, sessions
import asyncio
import os
import sys

API_ID = os.environ.get("API_ID")
API_HASH = os.environ.get("API_HASH")
BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = os.environ.get("CHAT_ID")
MESSAGE_THREAD_ID = os.environ.get("MESSAGE_THREAD_ID")

async def send_telegram_files(files):
    """
    Connects to Telegram and sends the specified files as a group message.
    """
    try:
        target_chat = int(CHAT_ID)
    except (ValueError, TypeError):
        print(f"[-] Invalid CHAT_ID: {CHAT_ID}")
        return

    topic_id = None
    if MESSAGE_THREAD_ID and MESSAGE_THREAD_ID.strip():
        try:
            topic_id = int(MESSAGE_THREAD_ID)
            print(f"[+] Targeting Topic ID: {topic_id}")
        except ValueError:
            print(f"[-] Invalid MESSAGE_THREAD_ID: {MESSAGE_THREAD_ID}, sending to main channel instead.")

    session = sessions.StringSession() 

    async with TelegramClient(session, api_id=API_ID, api_hash=API_HASH) as client:
        await client.start(bot_token=BOT_TOKEN)

        print(f"[+] Sending {len(files)} files as a group...")
        await client.send_file(
            entity=target_chat,
            file=files,
            reply_to=topic_id
        )
        print("[+] Files sent successfully.")

if __name__ == '__main__':
    if len(sys.argv) > 1:
        apk_files = sys.argv[1:]
        print(f"[+] Found files to upload: {apk_files}")
        try:
            asyncio.run(send_telegram_files(apk_files))
        except Exception as e:
            print(f"[-] An error occurred: {e}")
    else:
        print("[-] No file paths provided as arguments.")
