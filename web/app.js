'use strict';
const app = document.getElementById('app');
const WEEKS = { 1: 'Âm cơ bản + từ nền', 2: 'Phonics + từ công việc', 3: 'Giao tiếp công việc', 4: 'Lớp training AU480/DxC 700 AU' };
const WEEK_COLORS = [['#58cc02', '#58a700'], ['#1cb0f6', '#1899d6']];
const ZIGZAG = [0, 45, 70, 45, 0, -45, -70, -45];
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const todayNumber = () => Math.floor((Date.now() - new Date().getTimezoneOffset() * 60000) / 86400000);

// Progress is kept in this browser only (Safari on Leo's iPhone).
const store = {
  load() {
    try { return JSON.parse(localStorage.getItem('progress')) || {}; } catch (e) { return {}; }
  },
  save(p) { try { localStorage.setItem('progress', JSON.stringify(p)); } catch (e) { /* private mode */ } },
};
let progress = Object.assign({ done: [], xp: 0, streak: 0, last: -99 }, store.load());
let lessons = [];

// Speech output: the browser's built-in voices (free).
let voice = null;
function pickVoice() {
  const voices = speechSynthesis.getVoices();
  voice = voices.find((v) => v.lang === 'en-US' && /Samantha|Google/.test(v.name)) || voices.find((v) => v.lang === 'en-US') ||
    voices.find((v) => v.lang.startsWith('en')) || null;
}
if ('speechSynthesis' in window) { pickVoice(); speechSynthesis.onvoiceschanged = pickVoice; }
function say(text, slow) {
  if (!('speechSynthesis' in window)) return;
  speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'en-US';
  if (voice) u.voice = voice;
  u.rate = slow ? 0.6 : 0.9;
  speechSynthesis.speak(u);
}

// Speech input: the browser's speech recognizer (Safari on iPhone, Chrome on Android/desktop).
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
let recognition = null;
function listen(onResult, onError) {
  if (!Recognition) { onError('Trình duyệt này chưa hỗ trợ nhận dạng giọng nói. Trên iPhone hãy mở bằng Safari.'); return; }
  if ('speechSynthesis' in window) speechSynthesis.cancel();
  recognition = new Recognition();
  recognition.lang = 'en-US';
  recognition.maxAlternatives = 5;
  recognition.interimResults = false;
  let got = false;
  recognition.onresult = (e) => {
    got = true;
    const alts = [];
    for (let i = 0; i < e.results[0].length; i++) alts.push(e.results[0][i].transcript);
    onResult(alts);
  };
  recognition.onerror = (e) => {
    got = true;
    if (e.error === 'not-allowed' || e.error === 'service-not-allowed') onError('Cần cho phép micro và nhận dạng giọng nói trong Cài đặt > Safari.');
    else if (e.error === 'network') onError('Cần có mạng để nhận dạng giọng nói.');
    else onError('Máy chưa nghe rõ, bấm micro và nói lại nhé.');
  };
  recognition.onend = () => { if (!got) onError('Máy chưa nghe rõ, bấm micro và nói lại nhé.'); recognition = null; };
  recognition.start();
}
function stopListening() { if (recognition) { recognition.onend = null; recognition.abort(); recognition = null; } }

// ---------- Home: the lesson path ----------
function renderHome() {
  stopListening();
  const today = Logic.currentDay(progress.done, lessons.length);
  const streak = Logic.visibleStreak(progress.streak, progress.last, todayNumber());
  let html = `<div class="stats"><span class="days">🇬🇧 ${progress.done.length}/${lessons.length}</span>
    <span class="${streak ? 'fire' : 'off'}">🔥 ${streak}</span><span class="xp">⚡ ${progress.xp} XP</span></div><div class="path">`;
  let week = 0;
  lessons.forEach((l) => {
    if (l.week !== week) {
      week = l.week;
      const [c, s] = WEEK_COLORS[(week - 1) % WEEK_COLORS.length];
      html += `<div class="week" style="--c:${c};--s:${s}"><small>TUẦN ${week}</small><b>${esc(WEEKS[week] || '')}</b></div>`;
    }
    const state = progress.done.includes(l.day) ? 'done' : l.day === today ? 'current' : 'locked';
    const icon = state === 'done' ? '✓' : state === 'current' ? '★' : '🔒';
    html += `<div class="node ${state}" style="transform:translateX(${ZIGZAG[(l.day - 1) % ZIGZAG.length]}px)" ${state === 'current' ? 'id="today"' : ''}>
      ${state === 'current' ? '<div class="start">BẮT ĐẦU</div>' : ''}
      <button class="circle" data-day="${l.day}" ${state === 'locked' ? 'disabled' : ''}>${icon}</button>
      Ngày ${l.day} · ${esc(l.title)}</div>`;
  });
  app.innerHTML = html + '</div>';
  app.querySelectorAll('.node button:not([disabled])').forEach((b) => { b.onclick = () => startLesson(Number(b.dataset.day)); });
  const t = document.getElementById('today');
  if (t) t.scrollIntoView({ block: 'center' });
}

// ---------- Lesson: one exercise at a time, missed ones come back once ----------
let L = null;
function startLesson(day) {
  const lesson = lessons.find((l) => l.day === day);
  const exercises = Logic.exercisesFor(lesson);
  L = { lesson, total: exercises.length, queue: exercises.map((e, i) => [i, e]), index: 0, done: new Set(), retried: new Set(),
    firstTry: 0, selected: null, feedback: null, speech: null, listening: false, micError: null };
  showExercise(true);
}

function showExercise(autoplay) {
  if (L.index >= L.queue.length) return renderFinish();
  const [id, e] = L.queue[L.index];
  if (autoplay) say(e.type === 'listen' ? e.answer : e.type === 'meaning' ? e.en : e.text, e.type === 'listen');
  const checked = !!L.feedback;
  let body = L.retried.has(id) ? '<div class="retry">LÀM LẠI CÂU SAI</div>' : '';
  const optionClass = (o) => checked && o === e.answer ? 'right' : checked && o === L.selected ? 'wrong' : o === L.selected ? 'selected' : '';
  if (e.type === 'listen') {
    body += `<h1>Nghe và chọn từ đúng</h1><button class="circle blue" style="width:64px;height:64px;font-size:26px" data-say="${esc(e.answer)}">🔊</button>
      <div class="soft">Luyện âm ${esc(e.sound)}</div>` +
      e.options.map((o) => `<button class="option ${optionClass(o)}" data-option="${esc(o)}">${esc(o)}</button>`).join('');
  } else if (e.type === 'meaning') {
    body += `<h1>Chọn nghĩa đúng</h1><div class="row"><button class="circle blue" data-say="${esc(e.en)}">🔊</button><span class="big">${esc(e.en)}</span></div>` +
      e.options.map((o) => `<button class="option ${optionClass(o)}" data-option="${esc(o)}">${esc(o)}</button>`).join('');
  } else {
    const text = L.speech
      ? L.speech.words.map(([w, ok]) => `<span class="${ok ? 'ok' : 'bad'}">${esc(w)}</span>`).join(' ')
      : esc(e.text);
    body += `<h1>Đọc to</h1><div class="row"><button class="circle blue" data-say="${esc(e.text)}" data-slow="1">🔊</button>
      <div><div class="big">${text}</div>${e.vi ? `<div class="soft">${esc(e.vi)}</div>` : ''}</div></div>
      ${e.sound ? `<div class="soft">Chú ý âm ${esc(e.sound)}: ${esc(e.tip)}</div>` : ''}
      <div class="mic"><button class="circle blue ${L.listening ? 'listening' : ''}" id="mic" ${checked ? 'disabled' : ''}>🎤</button>
      <b class="${L.listening ? 'bad' : 'soft'}">${L.listening ? 'Đang nghe… nói ngay' : 'Bấm micro rồi nói'}</b>
      ${L.micError ? `<div class="bad">${esc(L.micError)}</div>` : ''}</div>`;
  }
  let foot;
  if (L.feedback) {
    const f = L.feedback;
    foot = `<div class="foot ${f.correct ? 'right' : 'wrong'}"><h2>${esc(f.title)}</h2>${f.detail ? `<p>${esc(f.detail)}</p>` : ''}
      <button class="btn ${f.correct ? '' : 'red'}" id="continue">${f.correct ? 'Tiếp tục' : 'Đã hiểu'}</button></div>`;
  } else if (e.type === 'speak') {
    foot = '<div class="foot"><button class="skip" id="skip">KHÔNG NÓI ĐƯỢC LÚC NÀY</button></div>';
  } else {
    foot = `<div class="foot"><button class="btn" id="check" ${L.selected ? '' : 'disabled'}>Kiểm tra</button></div>`;
  }
  app.innerHTML = `<div class="top"><button class="close" id="close">✕</button><div class="bar"><i style="width:${L.done.size * 100 / L.total}%"></i></div></div>
    <div class="body">${body}</div>${foot}`;

  app.querySelector('#close').onclick = renderHome;
  app.querySelectorAll('[data-say]').forEach((b) => { b.onclick = () => say(b.dataset.say, !!b.dataset.slow || e.type === 'listen'); });
  app.querySelectorAll('[data-option]').forEach((b) => {
    b.onclick = () => {
      if (L.feedback) return;
      L.selected = b.dataset.option;
      if (e.type === 'listen') say(L.selected, true);
      showExercise(false);
    };
  });
  const on = (sel, fn) => { const el = app.querySelector(sel); if (el) el.onclick = fn; };
  on('#check', () => {
    const ok = L.selected === e.answer;
    const detail = ok ? '' : e.type === 'listen' ? `Đáp án: ${e.answer}. ${e.sound}: ${e.tip}` : `Đáp án: ${e.answer}`;
    answer(id, ok, ok ? 'Chính xác!' : 'Chưa đúng', detail);
  });
  on('#skip', () => next(id, e, true));
  on('#continue', () => next(id, e, false));
  on('#mic', () => {
    if (L.listening || L.feedback) return;
    L.listening = true; L.micError = null; showExercise(false);
    listen((alts) => {
      L.listening = false;
      const r = Logic.checkSpeech(e.text, alts);
      L.speech = r;
      const confused = e.confusable && Logic.tokens(r.heard).includes(e.confusable.toLowerCase());
      if (r.passed) answer(id, true, `Phát âm tốt! ${r.score}%`, '');
      else if (confused) answer(id, false, `Máy nghe thành “${e.confusable}”`, `${e.sound}: ${e.tip}`);
      else answer(id, false, `Chưa rõ, đạt ${r.score}%`, `Máy nghe: “${r.heard || '…'}”. Nói chậm lại và giữ rõ âm cuối.`);
    }, (msg) => { L.listening = false; L.micError = msg; showExercise(false); });
  });
}

function answer(id, correct, title, detail) {
  L.feedback = { correct, title, detail };
  if (correct && !L.retried.has(id)) L.firstTry++;
  showExercise(false);
}

function next(id, e, skipped) {
  stopListening();
  if (!(L.feedback && L.feedback.correct) && !skipped && !L.retried.has(id)) {
    L.retried.add(id);
    L.queue.push([id, e]);
  } else {
    L.done.add(id);
  }
  Object.assign(L, { selected: null, feedback: null, speech: null, listening: false, micError: null });
  L.index++;
  showExercise(true);
}

function renderFinish() {
  const xp = 10 + L.firstTry;
  const accuracy = L.total ? Math.floor(L.firstTry * 100 / L.total) : 100;
  app.innerHTML = `<div class="finish"><div style="font-size:72px">🎉</div><h1>Hoàn thành bài học!</h1>
    <div class="cards"><div class="card" style="--c:var(--gold)">TỔNG XP<div>⚡ ${xp}</div></div>
    <div class="card" style="--c:var(--green)">CHÍNH XÁC<div>🎯 ${accuracy}%</div></div></div>
    <button class="btn" id="done" style="margin-top:16px">Tiếp tục</button></div>`;
  app.querySelector('#done').onclick = () => {
    const today = todayNumber();
    if (!progress.done.includes(L.lesson.day)) progress.done.push(L.lesson.day);
    progress.xp += xp;
    progress.streak = Logic.nextStreak(progress.streak, progress.last, today);
    progress.last = today;
    store.save(progress);
    renderHome();
  };
}

fetch('lessons.json')
  .then((r) => r.json())
  .then((data) => { lessons = data.days.sort((a, b) => a.day - b.day); renderHome(); })
  .catch(() => { app.innerHTML = '<p style="padding:20px">Không tải được bài học. Kiểm tra mạng rồi mở lại.</p>'; });
