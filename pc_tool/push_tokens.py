#!/usr/bin/env python3
import os
import sys
import json
import sqlite3
import datetime
import urllib.request

# 手机局域网 IP 与端口
PHONE_IP = os.environ.get("PHONE_IP", "192.168.0.11")
PHONE_PORT = int(os.environ.get("PHONE_PORT", "8888"))
DB_PATH = os.path.expanduser("~/.local/share/opencode/opencode.db")

def get_today_stats():
    if not os.path.exists(DB_PATH):
        print(f"Database not found: {DB_PATH}")
        return None

    now = datetime.datetime.now()
    start_of_day = int(datetime.datetime(now.year, now.month, now.day).timestamp() * 1000)

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # 1. 优先统计今天零点以来的使用情况
    cursor.execute("""
        SELECT 
            SUM(json_extract(data, '$.tokens.input')),
            SUM(json_extract(data, '$.tokens.output')),
            SUM(json_extract(data, '$.tokens.cache.read')),
            SUM(json_extract(data, '$.cost')),
            json_extract(data, '$.modelID')
        FROM message
        WHERE time_created >= ? AND json_extract(data, '$.tokens') IS NOT NULL
    """, (start_of_day,))
    row = cursor.fetchone()

    # 2. 如果今天还没有新对话，则统计全部累计数据作为基线
    if row[0] is None or row[0] == 0:
        cursor.execute("""
            SELECT 
                SUM(json_extract(data, '$.tokens.input')),
                SUM(json_extract(data, '$.tokens.output')),
                SUM(json_extract(data, '$.tokens.cache.read')),
                SUM(json_extract(data, '$.cost')),
                (SELECT json_extract(data, '$.modelID') FROM message WHERE json_extract(data, '$.tokens') IS NOT NULL ORDER BY time_created DESC LIMIT 1)
            FROM message
            WHERE json_extract(data, '$.tokens') IS NOT NULL
        """)
        row = cursor.fetchone()

    conn.close()

    input_tokens = int(row[0] or 0)
    output_tokens = int(row[1] or 0)
    cache_tokens = int(row[2] or 0)
    cost = float(row[3] or 0.0)
    model = row[4] or "gemini-3.8-flash"
    total_tokens = input_tokens + output_tokens

    return {
        "model": model,
        "total_tokens": total_tokens,
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "cache_tokens": cache_tokens,
        "cost": cost
    }

def push_to_phone(data):
    url = f"http://{PHONE_IP}:{PHONE_PORT}/sync"
    payload = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})

    try:
        with urllib.request.urlopen(req, timeout=3) as resp:
            res = resp.read().decode("utf-8")
            print(f"Successfully pushed to {url} -> {res}")
            return True
    except Exception as e:
        print(f"Failed to push to {url}: {e}")
        return False

if __name__ == "__main__":
    stats = get_today_stats()
    if stats:
        print(f"Extracted OpenChamber Stats: {stats}")
        push_to_phone(stats)
    else:
        print("No stats extracted.")
