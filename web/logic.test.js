// Run with: node web/logic.test.js
const assert = require('assert');
const path = require('path');
const L = require('./logic.js');
const lessons = require(path.join(__dirname, '../app/src/main/assets/lessons.json')).days;

lessons.forEach((l) => {
  const ex = L.exercisesFor(l);
  assert(ex.length > 0, `day ${l.day} has no exercises`);
  ex.forEach((e) => {
    if (e.answer) assert(e.options.includes(e.answer), `day ${l.day}: answer missing from options`);
    if (e.type === 'meaning') assert.strictEqual(new Set(e.options).size, e.options.length);
  });
});
assert.deepStrictEqual(L.exercisesFor(lessons[0]), L.exercisesFor(lessons[0]));

assert(L.checkSpeech('think', ['sink', 'think']).passed);
const confused = L.checkSpeech('think', ['sink', 'zinc']);
assert(!confused.passed);
assert.strictEqual(confused.heard, 'sink');
assert(L.checkSpeech('Could you repeat that slowly?', ['could you repeat that slowly']).passed);
const partial = L.checkSpeech('I need to check it.', ['I need to chat']);
assert.deepStrictEqual(partial.words.map((w) => w[1]), [true, true, true, false, false]);
assert.strictEqual(partial.score, 60);
assert(!partial.passed);
assert(L.checkSpeech('eight', ['8']).passed);
assert(!L.checkSpeech('hello', []).passed);

assert.strictEqual(L.currentDay([1, 2, 4], 30), 3);
assert.strictEqual(L.nextStreak(3, 99, 100), 4);
assert.strictEqual(L.nextStreak(3, 97, 100), 1);
assert.strictEqual(L.visibleStreak(3, 98, 100), 0);
console.log('web logic ok');
