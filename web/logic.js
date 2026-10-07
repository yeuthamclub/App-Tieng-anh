// Lesson rules shared by the web app; mirrors Exercises.kt and Pronunciation.kt in the Android app.
(function (root) {
  'use strict';

  // Small seeded random generator so a day always gets the same exercises.
  function seeded(seed) {
    let a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function shuffled(list, rand) {
    const out = list.slice();
    for (let i = out.length - 1; i > 0; i--) {
      const j = Math.floor(rand() * (i + 1));
      [out[i], out[j]] = [out[j], out[i]];
    }
    return out;
  }

  function exercisesFor(lesson) {
    const rand = seeded(lesson.day);
    const out = [];
    (lesson.sounds || []).forEach((sound) => {
      sound.pairs.slice(0, 2).forEach(([a, b]) => {
        const answer = rand() < 0.5 ? a : b;
        out.push({ type: 'listen', answer, options: shuffled([a, b], rand), sound: sound.ipa, tip: sound.tip });
      });
      const first = sound.pairs[0];
      if (first) out.push({ type: 'speak', text: first[0], vi: '', sound: sound.ipa, tip: sound.tip, confusable: first[1] });
    });
    const words = (lesson.words || []).filter((w) => w.vi);
    if (words.length >= 3) {
      shuffled(words, rand).slice(0, 4).forEach((word) => {
        const meanings = [...new Set(words.map((w) => w.vi))].filter((vi) => vi !== word.vi);
        const wrong = shuffled(meanings, rand).slice(0, 2);
        out.push({ type: 'meaning', en: word.en, answer: word.vi, options: shuffled(wrong.concat(word.vi), rand) });
      });
    }
    shuffled(lesson.words || [], rand).slice(0, 3).forEach((w) => out.push({ type: 'speak', text: w.en, vi: w.vi }));
    (lesson.sentences || []).slice(0, 3).forEach((s) => out.push({ type: 'speak', text: s.en, vi: s.vi }));
    return out;
  }

  const NUMBERS = { 0: 'zero', 1: 'one', 2: 'two', 3: 'three', 4: 'four', 5: 'five', 6: 'six', 7: 'seven',
    8: 'eight', 9: 'nine', 10: 'ten', 13: 'thirteen', 20: 'twenty', 30: 'thirty' };

  function tokens(text) {
    return text.toLowerCase().replace(/’/g, "'").replace(/[^a-z0-9' ]/g, ' ')
      .split(' ').filter(Boolean).map((t) => NUMBERS[t] || t);
  }

  // Longest common subsequence: which target words appear, in order, in what was heard.
  function matchWords(want, heard) {
    const n = want.length, m = heard.length;
    const lcs = Array.from({ length: n + 1 }, () => new Array(m + 1).fill(0));
    for (let i = n - 1; i >= 0; i--) for (let j = m - 1; j >= 0; j--) {
      lcs[i][j] = want[i] === heard[j] ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
    }
    const marks = new Array(n).fill(false);
    let i = 0, j = 0;
    while (i < n && j < m) {
      if (want[i] === heard[j]) { marks[i] = true; i++; j++; }
      else if (lcs[i + 1][j] >= lcs[i][j + 1]) i++;
      else j++;
    }
    return marks;
  }

  // A single word must match exactly; a sentence passes at 80% of its words.
  function checkSpeech(target, alternatives) {
    const want = tokens(target);
    let best = null;
    (alternatives.length ? alternatives : ['']).forEach((alt) => {
      const marks = matchWords(want, tokens(alt));
      const count = marks.filter(Boolean).length;
      if (!best || count > best.count) best = { heard: alt, marks, count };
    });
    const matched = best.marks.filter(Boolean).length;
    const passed = want.length > 0 && (want.length === 1 ? matched === 1 : matched * 100 >= want.length * 80);
    const score = want.length ? Math.floor(matched * 100 / want.length) : 0;
    return { words: want.map((w, k) => [w, best.marks[k]]), heard: best.heard, passed, score };
  }

  function currentDay(completed, total) {
    for (let d = 1; d <= total; d++) if (!completed.includes(d)) return d;
    return total;
  }

  function nextStreak(streak, lastDay, today) {
    if (lastDay === today) return Math.max(streak, 1);
    if (lastDay === today - 1) return streak + 1;
    return 1;
  }

  function visibleStreak(streak, lastDay, today) {
    return lastDay >= today - 1 ? streak : 0;
  }

  const api = { exercisesFor, tokens, checkSpeech, currentDay, nextStreak, visibleStreak };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.Logic = api;
})(this);
