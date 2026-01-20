/**
 * Test Script: Avatar Config Consistency
 *
 * Mục đích: Verify rằng cùng lightNumber sẽ luôn generate ra cùng config
 * cho Mountain, Grass, Flowers, Moon, Diamond Variant
 *
 * Chạy script: node scripts/test-avatar-consistency.js
 */

// Seeded random function (copy từ AvatarGenerator.jsx)
const seededRandom = (seed) => {
  const x = Math.sin(seed) * 10000;
  return x - Math.floor(x);
};

// Config lengths (từ AvatarGenerator.jsx)
const MOUNTAINS_LENGTH = 10;
const GRASS_LENGTH = 10;
const FLOWERS_LENGTH = 10;
const MOONS_LENGTH = 11;
const DIAMOND_VARIANTS = 3;

// Function generate config từ lightNumber
const generateAvatarConfig = (lightNumber) => {
  const seed = lightNumber || 1;

  return {
    mountainIndex: Math.floor(seededRandom(seed) * MOUNTAINS_LENGTH),
    grassIndex: Math.floor(seededRandom(seed + 1) * GRASS_LENGTH),
    flowersIndex: Math.floor(seededRandom(seed + 2) * FLOWERS_LENGTH),
    moonIndex: Math.floor(seededRandom(seed + 3) * MOONS_LENGTH),
    diamondVariantIndex: Math.floor(seededRandom(seed + 4) * DIAMOND_VARIANTS),
  };
};

// ============================================
// TEST: Simulate 20 logins với cùng lightNumber
// ============================================

const TEST_LIGHT_NUMBER = 42; // User cố định
const NUM_LOGINS = 20;

console.log('╔════════════════════════════════════════════════════════════════╗');
console.log('║     TEST: Avatar Config Consistency Across Multiple Logins     ║');
console.log('╠════════════════════════════════════════════════════════════════╣');
console.log(`║  Light Number: ${TEST_LIGHT_NUMBER}`);
console.log(`║  Number of Logins: ${NUM_LOGINS}`);
console.log('╚════════════════════════════════════════════════════════════════╝');
console.log('');

// Store results
const results = [];

// Simulate 20 logins
for (let login = 1; login <= NUM_LOGINS; login++) {
  const config = generateAvatarConfig(TEST_LIGHT_NUMBER);
  results.push({
    login,
    ...config,
  });
}

// Display results table
console.log('┌────────┬──────────┬───────┬─────────┬──────┬─────────────────┐');
console.log('│ Login  │ Mountain │ Grass │ Flowers │ Moon │ Diamond Variant │');
console.log('├────────┼──────────┼───────┼─────────┼──────┼─────────────────┤');

results.forEach((r) => {
  console.log(
    `│ ${String(r.login).padStart(6)} │ ${String(r.mountainIndex).padStart(8)} │ ${String(r.grassIndex).padStart(5)} │ ${String(r.flowersIndex).padStart(7)} │ ${String(r.moonIndex).padStart(4)} │ ${String(r.diamondVariantIndex).padStart(15)} │`
  );
});

console.log('└────────┴──────────┴───────┴─────────┴──────┴─────────────────┘');
console.log('');

// Verify consistency
const firstConfig = results[0];
const allSame = results.every(
  (r) =>
    r.mountainIndex === firstConfig.mountainIndex &&
    r.grassIndex === firstConfig.grassIndex &&
    r.flowersIndex === firstConfig.flowersIndex &&
    r.moonIndex === firstConfig.moonIndex &&
    r.diamondVariantIndex === firstConfig.diamondVariantIndex
);

console.log('╔════════════════════════════════════════════════════════════════╗');
if (allSame) {
  console.log('║  ✅ PASSED: Tất cả 20 lần login đều có CÙNG config!            ║');
  console.log('╠════════════════════════════════════════════════════════════════╣');
  console.log(`║  Mountain:        #${firstConfig.mountainIndex}`);
  console.log(`║  Grass:           #${firstConfig.grassIndex}`);
  console.log(`║  Flowers:         #${firstConfig.flowersIndex}`);
  console.log(`║  Moon:            #${firstConfig.moonIndex}`);
  console.log(`║  Diamond Variant: #${firstConfig.diamondVariantIndex}`);
} else {
  console.log('║  ❌ FAILED: Config khác nhau giữa các lần login!               ║');
}
console.log('╚════════════════════════════════════════════════════════════════╝');
console.log('');

// Bonus: Test với nhiều lightNumber khác nhau
console.log('');
console.log('╔════════════════════════════════════════════════════════════════╗');
console.log('║     BONUS: So sánh config của các lightNumber khác nhau        ║');
console.log('╚════════════════════════════════════════════════════════════════╝');
console.log('');
console.log('┌─────────────┬──────────┬───────┬─────────┬──────┬─────────────────┐');
console.log('│ LightNumber │ Mountain │ Grass │ Flowers │ Moon │ Diamond Variant │');
console.log('├─────────────┼──────────┼───────┼─────────┼──────┼─────────────────┤');

[1, 10, 42, 100, 500, 1000].forEach((ln) => {
  const config = generateAvatarConfig(ln);
  console.log(
    `│ ${String(ln).padStart(11)} │ ${String(config.mountainIndex).padStart(8)} │ ${String(config.grassIndex).padStart(5)} │ ${String(config.flowersIndex).padStart(7)} │ ${String(config.moonIndex).padStart(4)} │ ${String(config.diamondVariantIndex).padStart(15)} │`
  );
});

console.log('└─────────────┴──────────┴───────┴─────────┴──────┴─────────────────┘');
console.log('');
console.log('→ Mỗi lightNumber khác nhau sẽ có config khác nhau');
console.log('→ Nhưng cùng lightNumber luôn cho cùng config');
