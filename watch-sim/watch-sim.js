#!/usr/bin/env node
/**
 * 模拟手表端（阶段2）
 *
 * 作用：在没有真机的情况下，按真实手表的方式调用设备侧接口，
 *      让网页上的心率血压、睡眠步数、位置、摔倒告警数据真正“动起来”。
 *
 * 只依赖 Node 内置模块，Node 10 及以上都能跑：
 *   node watch-sim.js
 *   node watch-sim.js --deviceNo WATCH0002 --secret demo-secret-2
 *   node watch-sim.js --scenario demo        # 循环演示：正常 -> 心率异常 -> 摔倒 -> SOS
 *   node watch-sim.js --scenario normal      # 只上报正常数据
 *   node watch-sim.js --server http://192.168.1.5:8795
 *   node watch-sim.js --bind ABC123          # 先在手表上绑定老人
 */

'use strict';

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');

const CONFIG_FILE = path.join(__dirname, 'watch-sim.config.json');

const HELP = [
  '模拟手表端 —— 参数说明',
  '',
  '  --server <url>       后端地址，默认读配置文件（如 http://192.168.1.5:8795）',
  '  --deviceNo <编号>    设备号（默认 WATCH0001）',
  '  --secret <密钥>      设备密钥（默认 demo-secret-1）',
  '  --interval <秒>      上报间隔，默认 3 秒',
  '  --scenario <模式>    normal=只报正常数据；demo=循环演示异常（默认 demo）',
  '  --bind <绑定码>      启动时先执行一次绑定（6 位码，可在系统里生成）',
  '  --once               只上报一次后退出（用于联调）',
  '  --help               显示本帮助',
  ''
].join('\n');

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i++) {
    const item = argv[i];
    if (item.indexOf('--') !== 0) {
      continue;
    }
    const key = item.slice(2);
    const next = argv[i + 1];
    if (next !== undefined && next.indexOf('--') !== 0) {
      args[key] = next;
      i++;
    } else {
      args[key] = true;
    }
  }
  return args;
}

function loadConfig() {
  const defaults = {
    server: 'http://127.0.0.1:8795',
    deviceNo: 'WATCH0001',
    secret: 'demo-secret-1',
    intervalSeconds: 3,
    scenario: 'demo',
    startPoint: { lng: 113.0625, lat: 28.1975 },
    appVersion: '1.0.0-demo'
  };
  let fileConfig = {};
  if (fs.existsSync(CONFIG_FILE)) {
    try {
      fileConfig = JSON.parse(fs.readFileSync(CONFIG_FILE, 'utf8'));
    } catch (e) {
      console.log('[warn] 配置文件解析失败，使用默认值：' + e.message);
    }
  }
  const merged = Object.assign({}, defaults, fileConfig);
  merged.startPoint = Object.assign({}, defaults.startPoint, fileConfig.startPoint || {});
  return merged;
}

function postJson(baseUrl, apiPath, body) {
  return new Promise(function (resolve, reject) {
    const target = url.resolve(baseUrl.replace(/\/+$/, '') + '/', apiPath.replace(/^\/+/, ''));
    const payload = Buffer.from(JSON.stringify(body), 'utf8');
    const parsed = url.parse(target);
    const lib = parsed.protocol === 'https:' ? https : http;
    const req = lib.request({
      hostname: parsed.hostname,
      port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
      path: parsed.path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'Content-Length': payload.length
      },
      timeout: 10000
    }, function (res) {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', function (chunk) { raw += chunk; });
      res.on('end', function () {
        let json = null;
        try {
          json = JSON.parse(raw);
        } catch (e) {
          json = { returnCode: 'HTTP_' + res.statusCode, raw: raw.slice(0, 200) };
        }
        resolve(json);
      });
    });
    req.on('timeout', function () { req.destroy(new Error('请求超时')); });
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

// ---------------- 数据模拟 ----------------

function pad(n) { return n < 10 ? '0' + n : '' + n; }

function nowText() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' '
    + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
}

function dayText() {
  const d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
}

function randomInt(min, max) {
  return min + Math.floor(Math.random() * (max - min + 1));
}

function createState(config) {
  return {
    step: randomInt(800, 2000),
    sleepHours: randomInt(6, 9),
    // 围绕起点缓慢走动（每步约 20~60 米）
    lng: config.startPoint.lng,
    lat: config.startPoint.lat,
    reportNo: 0,
    phase: 'normal',
    phaseLeft: 8
  };
}

function walk(state) {
  state.lng = +(state.lng + (Math.random() - 0.5) * 0.0006).toFixed(6);
  state.lat = +(state.lat + (Math.random() - 0.5) * 0.0006).toFixed(6);
}

/** 演示模式：正常 -> 心率偏高 -> 恢复正常 -> 摔倒 -> SAN -> 正常 ... */
function nextPhase(state) {
  if (state.phase === 'normal') {
    return 'highHr';
  }
  if (state.phase === 'highHr') {
    return 'normal';
  }
  if (state.phase === 'fall') {
    return 'sos';
  }
  if (state.phase === 'sos') {
    return 'normal';
  }
  return 'normal';
}

function buildPayload(config, state) {
  const scenario = String(config.scenario || 'demo').toLowerCase();
  state.reportNo++;
  state.phaseLeft--;
  if (state.phaseLeft <= 0) {
    state.phase = scenario === 'normal' ? 'normal' : nextPhase(state);
    state.phaseLeft = state.phase === 'normal' ? randomInt(6, 10) : randomInt(2, 4);
  }
  if (scenario === 'normal') {
    state.phase = 'normal';
  }

  walk(state);
  state.step += randomInt(3, 15);

  const payload = {
    deviceNo: config.deviceNo,
    secret: config.secret,
    beTime: nowText(),
    step: state.step,
    sleep: state.sleepHours,
    sleepState: state.sleepHours >= 7 ? '0' : '1',
    lng: state.lng,
    lat: state.lat,
    source: 'gps',
    battery: randomInt(60, 100),
    appVersion: config.appVersion
  };

  if (state.phase === 'highHr') {
    payload.hr = randomInt(118, 145);      // 超过阈值 110 -> 心率异常
    payload.bp = randomInt(142, 165);      // 超过阈值 140 -> 血压异常
  } else {
    payload.hr = randomInt(62, 92);
    payload.bp = randomInt(105, 132);
  }

  if (state.phase === 'fall') {
    payload.alarm = '1';                   // 摔倒告警
    payload.hr = randomInt(95, 120);
  }
  return payload;
}

// ---------------- 主流程 ----------------

function line(text) {
  console.log('[' + nowText() + '] ' + text);
}

async function main() {
  const args = parseArgs(process.argv);
  if (args.help) {
    console.log(HELP);
    return;
  }

  const config = loadConfig();
  if (args.server) { config.server = String(args.server); }
  if (args.deviceNo) { config.deviceNo = String(args.deviceNo).toUpperCase(); }
  if (args.secret) { config.secret = String(args.secret); }
  if (args.interval) { config.intervalSeconds = Math.max(1, parseInt(args.interval, 10) || 3); }
  if (args.scenario) { config.scenario = String(args.scenario); }

  const state = createState(config);
  const once = !!args.once;

  line('模拟手表启动：' + config.deviceNo + ' -> ' + config.server
    + '（间隔 ' + config.intervalSeconds + 's，模式 ' + config.scenario + '）');

  // 1. 设备登录
  let res = await postJson(config.server, 'restful/device/login', {
    deviceNo: config.deviceNo,
    secret: config.secret
  });
  if (res.returnCode !== '1111') {
    line('登录失败：' + (res.message || JSON.stringify(res)));
    line('请检查设备号/密钥是否正确（可在系统「设备管理」里查看），以及后端是否已启动。');
    if (!once) {
      line('10 秒后自动重试 ...');
      setTimeout(main, 10000);
    }
    return;
  }
  line('登录成功：deviceId=' + res.deviceId + '，绑定状态=' + (res.bound ? ('已绑定 ' + (res.oldPersonName || '')) : '未绑定'));

  // 2. 需要时先绑定
  if (args.bind) {
    const bindRes = await postJson(config.server, 'restful/device/bind', {
      deviceNo: config.deviceNo,
      secret: config.secret,
      code: String(args.bind).toUpperCase()
    });
    if (bindRes.returnCode === '1111') {
      line('绑定成功：' + bindRes.oldPersonName + '（oldId=' + bindRes.oldId + '）');
    } else {
      line('绑定失败：' + (bindRes.message || JSON.stringify(bindRes)));
    }
  } else if (!res.bound) {
    line('提示：该设备尚未绑定老人，上报会被拒绝。');
    line('请到系统「设备管理」生成绑定码，然后运行：node watch-sim.js --bind 绑定码');
    return;
  }

  // 3. 心跳
  const hb = await postJson(config.server, 'restful/device/heartbeat', {
    deviceNo: config.deviceNo,
    secret: config.secret,
    battery: 88,
    appVersion: config.appVersion
  });
  if (hb.returnCode === '1111') {
    line('心跳正常，服务器时间 ' + hb.serverTime);
  } else {
    line('心跳失败：' + (hb.message || JSON.stringify(hb)));
  }

  // 4. 循环上报
  let running = true;
  process.on('SIGINT', function () {
    running = false;
    line('已停止模拟手表（共上报 ' + state.reportNo + ' 次）');
    process.exit(0);
  });

  async function tick() {
    if (!running) {
      return;
    }
    const payload = buildPayload(config, state);
    try {
      const r = await postJson(config.server, 'restful/device/report', payload);
      if (r.returnCode === '1111') {
        const flag = [];
        if (payload.alarm) { flag.push('摔倒告警'); }
        if (r.rateState === '1') { flag.push('心率异常'); }
        if (r.preState === '1') { flag.push('血压异常'); }
        line('第 ' + state.reportNo + ' 次上报  心率=' + payload.hr + '  血压=' + payload.bp
          + '  步数=' + payload.step + '  位置=' + payload.lng + ',' + payload.lat
          + (flag.length ? '  ★ ' + flag.join(' / ') : ''));
      } else {
        line('上报被拒绝：' + (r.message || JSON.stringify(r)));
      }
    } catch (e) {
      line('上报失败：' + e.message + '（检查后端是否启动、地址是否为局域网 IP）');
    }
    if (!once && running) {
      setTimeout(tick, config.intervalSeconds * 1000);
    } else {
      line('本次上报完成，退出。');
      process.exit(0);
    }
  }

  tick();
}

main().catch(function (e) {
  console.log('运行出错：' + (e && e.message ? e.message : e));
  process.exit(1);
});
