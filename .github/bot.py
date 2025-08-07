from telethon import TelegramClient
import asyncio
from telethon.sessions import StringSession
import os
import sys

API_ID = os.environ.get("API_ID")
API_HASH = os.environ.get("API_HASH")

BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = int(os.environ.get("CHAT_ID"))
COMMIT_MESSAGE = os.environ.get("COMMIT_MESSAGE")
BOT_CI_SESSION = os.environ.get("BOT_CI_SESSION")
MSG_TEMPLATE = """
```
{commit_message}
```
""".strip()


def get_caption():
    msg = MSG_TEMPLATE.format(
        commit_message=COMMIT_MESSAGE,
    )
    return msg

async def send_telegram_message():
    files = sys.argv[1]
    client = TelegramClient("bot", API_ID, API_HASH)
        await client.start(bot_token=BOT_TOKEN)
        print("[+] Caption: ")
        print("---")
        print("---")
        print("[+] Sending")
        await client.send_file(
            entity=CHAT_ID,
            file=files,
            caption=get_caption(),
            parse_mode="markdown"
        )

if __name__ == '__main__':
    try:
        asyncio.run(send_telegram_message())
    except Exception as e:
        print(f"[-] An error occurred: {e}")
