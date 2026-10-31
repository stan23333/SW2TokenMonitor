import http from 'http';
import { exec } from 'child_process';
import os from 'os';
import path from 'path';
import fs from 'fs';

const DB_PATH = path.join(os.homedir(), '.local/share/opencode/opencode.db');
const LOG_FILE = path.join(os.homedir(), '.local/share/opencode/token_push.log');
const PHONE_IP = '192.168.0.11';
const PHONE_PORT = 8888;

function log(msg) {
  const ts = new Date().toISOString();
  const line = `[${ts}] ${msg}\n`;
  try {
    fs.appendFileSync(LOG_FILE, line, 'utf-8');
  } catch (e) {}
}

function fetchStatsAndPush(triggerReason = 'scheduled') {
  const now = new Date();
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();

  const sqlToday = `SELECT SUM(json_extract(data, '$.tokens.input')), SUM(json_extract(data, '$.tokens.output')), SUM(json_extract(data, '$.tokens.cache.read')), SUM(json_extract(data, '$.cost')), json_extract(data, '$.modelID') FROM message WHERE time_created >= ${startOfDay} AND json_extract(data, '$.tokens') IS NOT NULL;`;

  exec(`sqlite3 "${DB_PATH}" "${sqlToday}"`, (err, stdout) => {
    let output = (stdout || '').trim();
    let parts = output ? output.split('|') : [];

    if (!parts[0] || parts[0] === '') {
      const sqlFallback = `SELECT SUM(json_extract(data, '$.tokens.input')), SUM(json_extract(data, '$.tokens.output')), SUM(json_extract(data, '$.tokens.cache.read')), SUM(json_extract(data, '$.cost')), (SELECT json_extract(data, '$.modelID') FROM message WHERE json_extract(data, '$.tokens') IS NOT NULL ORDER BY time_created DESC LIMIT 1) FROM message WHERE json_extract(data, '$.tokens') IS NOT NULL;`;
      exec(`sqlite3 "${DB_PATH}" "${sqlFallback}"`, (err2, stdout2) => {
        let fallbackOutput = (stdout2 || '').trim();
        let fparts = fallbackOutput ? fallbackOutput.split('|') : [];
        if (fparts.length >= 5) {
          sendToPhone(fparts[0], fparts[1], fparts[2], fparts[3], fparts[4], triggerReason);
        }
      });
      return;
    }

    sendToPhone(parts[0], parts[1], parts[2], parts[3], parts[4], triggerReason);
  });
}

function sendToPhone(inStr, outStr, cacheStr, costStr, modelStr, triggerReason) {
  const input = parseInt(inStr || '0', 10);
  const output = parseInt(outStr || '0', 10);
  const total = input + output;
  const payloadObj = {
    model: modelStr || 'gemini-3.8-flash',
    total_tokens: total,
    input_tokens: input,
    output_tokens: output,
    cache_tokens: parseInt(cacheStr || '0', 10),
    cost: parseFloat(costStr || '0.0')
  };
  const payload = JSON.stringify(payloadObj);

  log(`[TRIGGER: ${triggerReason}] Pushing payload: model=${payloadObj.model}, total=${total}`);

  // 1. 优先 Wi-Fi 直连
  const req = http.request({
    hostname: PHONE_IP,
    port: PHONE_PORT,
    path: '/sync',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(payload)
    },
    timeout: 1500
  }, (res) => {
    let body = '';
    res.on('data', d => body += d);
    res.on('end', () => {
      log(`[SUCCESS_WIFI] HTTP status=${res.statusCode}, response=${body}`);
    });
  });

  req.on('error', (err) => {
    log(`[WIFI_FAIL] Error=${err.message}. Trying ADB fallback...`);
    fallbackAdbPush(payload);
  });

  req.on('timeout', () => {
    log(`[WIFI_TIMEOUT] Timeout reached. Trying ADB fallback...`);
    req.destroy();
    fallbackAdbPush(payload);
  });

  req.write(payload);
  req.end();
}

function fallbackAdbPush(payload) {
  const escaped = payload.replace(/'/g, "'\\''");
  const cmd = `adb shell "curl -s -X POST http://127.0.0.1:8888/sync -H 'Content-Type: application/json' -d '${escaped}'"`;
  exec(cmd, (err, stdout, stderr) => {
    if (err) {
      log(`[ADB_FAIL] Error=${err.message}, stderr=${stderr}`);
    } else {
      log(`[SUCCESS_ADB] response=${stdout.trim()}`);
    }
  });
}

export default {
  id: 'token-monitor',
  setup(ctx) {
    log('[PLUGIN] OpenChamber token-monitor plugin setup initialized.');
    setTimeout(() => fetchStatsAndPush('startup'), 1000);

    if (ctx && ctx.session && typeof ctx.session.hook === 'function') {
      try {
        ctx.session.hook('context', async (event) => {
          setTimeout(() => fetchStatsAndPush('session_hook_context'), 1500);
        });
      } catch (e) {
        log(`[HOOK_ERR] session.hook error: ${e.message}`);
      }
    }

    const timer = setInterval(() => fetchStatsAndPush('interval_60s'), 60000);
    return () => clearInterval(timer);
  }
};
