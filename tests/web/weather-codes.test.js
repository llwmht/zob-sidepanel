import { test } from 'node:test';
import assert from 'node:assert/strict';
import { describeCode } from '../../web/widgets/weather/weather-codes.js';

const text = (c) => describeCode(c).text;

test('WMO code groups', () => {
  assert.equal(text(0), 'Clear');
  assert.equal(text(1), 'Mostly clear');
  assert.equal(text(2), 'Partly cloudy');
  assert.equal(text(3), 'Cloudy');
  assert.equal(text(45), 'Fog');
  assert.equal(text(48), 'Fog');
  assert.equal(text(53), 'Drizzle');
  assert.equal(text(63), 'Rain');
  assert.equal(text(66), 'Freezing rain');
  assert.equal(text(73), 'Snow');
  assert.equal(text(81), 'Showers');
  assert.equal(text(86), 'Snow showers');
  assert.equal(text(95), 'Thunderstorm');
  assert.equal(text(99), 'Thunderstorm');
});

test('unknown codes', () => {
  assert.equal(text(4), 'Unknown');
  assert.equal(text(undefined), 'Unknown');
});

test('every description has an icon', () => {
  for (const c of [0, 1, 2, 3, 45, 51, 61, 66, 71, 80, 85, 95, 4]) assert.equal(typeof describeCode(c).icon, 'string');
});
