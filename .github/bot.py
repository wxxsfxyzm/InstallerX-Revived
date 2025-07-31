from telethon import TelegramClient
import asyncio
from telethon.sessions import StringSession
import os
import sys

API_ID = 611335
API_HASH = "d524b414d21f4d37f08684c1df41ac9c"

BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = os.environ.get("CHAT_ID")
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
    if len(msg) > 1024:
        return COMMIT_URL
    return msg

async def send_telegram_message():
    files = sys.argv[1]
    async with TelegramClient(StringSession(BOT_CI_SESSION), api_id=API_ID, api_hash=API_HASH) as client:
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
