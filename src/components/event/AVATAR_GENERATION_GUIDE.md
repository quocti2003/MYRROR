# Avatar Generation Guide

## Overview

This document explains how to generate unique avatars that don't repeat for each user in the event system.

---

## Setup: Define Elements & Options

```javascript
// avatarConfig.js

export const AVATAR_ELEMENTS = {
  // Element 1: Background
  background: {
    options: [
      { id: 'bg1', name: 'Pink Gradient', asset: '/assets/bg/pink.png' },
      { id: 'bg2', name: 'Purple Night', asset: '/assets/bg/purple.png' },
      { id: 'bg3', name: 'Midnight', asset: '/assets/bg/midnight.png' },
    ]
  },

  // Element 2: Scenery/Pattern
  scenery: {
    options: [
      { id: 'sc1', name: 'Mountains', asset: '/assets/scenery/mountains.png' },
      { id: 'sc2', name: 'Stars', asset: '/assets/scenery/stars.png' },
      { id: 'sc3', name: 'Flowers', asset: '/assets/scenery/flowers.png' },
    ]
  },

  // Element 3: Diamond Shape
  diamond: {
    options: [
      { id: 'd1', name: 'Round', asset: '/assets/diamond/round.png' },
      { id: 'd2', name: 'Heart', asset: '/assets/diamond/heart.png' },
      { id: 'd3', name: 'Oval', asset: '/assets/diamond/oval.png' },
    ]
  },

  // Element 4: Frame/Decoration
  frame: {
    options: [
      { id: 'f1', name: 'Classic', asset: '/assets/frame/classic.png' },
      { id: 'f2', name: 'Modern', asset: '/assets/frame/modern.png' },
    ]
  }
};

// Total combinations = 3 × 3 × 3 × 2 = 54 unique avatars
export const getTotalCombinations = () => {
  return Object.values(AVATAR_ELEMENTS).reduce(
    (total, element) => total * element.options.length,
    1
  );
};
```

---

## Approach 1: Index-based (Recommended)

### How it works

Uses `lightNumber` as an index to deterministically generate a unique combination.
Think of it like an odometer - each position cycles through its options.

```
lightNumber 1  → A-X-1-M
lightNumber 2  → A-X-1-N  (last element changes)
lightNumber 3  → A-X-2-M  (3rd element changes, 4th resets)
...
```

### Basic Implementation (Sequential)

```javascript
/**
 * Generate unique combination from lightNumber
 * Same lightNumber always produces same combination
 */
export const generateUniqueCombination = (lightNumber) => {
  const elements = Object.entries(AVATAR_ELEMENTS);
  let index = lightNumber - 1; // 0-based
  const combination = {};

  // Decode index into combination (like converting decimal to mixed-base)
  for (let i = elements.length - 1; i >= 0; i--) {
    const [elementName, elementData] = elements[i];
    const optionCount = elementData.options.length;
    const optionIndex = index % optionCount;
    combination[elementName] = elementData.options[optionIndex];
    index = Math.floor(index / optionCount);
  }

  return combination;
};
```

### Problem: Consecutive users have similar avatars

```
lightNumber 1: A-X-1-M
lightNumber 2: A-X-1-N  ← Only 1 element different!
```

### Solution: Shuffle with Prime Number

Multiply by a prime number to "jump around" instead of counting sequentially.

```javascript
/**
 * Generate unique combination with shuffling
 * Consecutive lightNumbers produce very different combinations
 */
export const generateShuffledCombination = (lightNumber) => {
  const TOTAL = getTotalCombinations();

  // Choose a prime number that:
  // 1. Is less than TOTAL
  // 2. Does not divide TOTAL evenly
  // This ensures we visit ALL combinations before repeating
  const SHUFFLE_PRIME = findSuitablePrime(TOTAL);

  // Shuffle: consecutive numbers now map to distant indices
  const shuffledIndex = ((lightNumber - 1) * SHUFFLE_PRIME) % TOTAL;

  // Decode shuffled index into combination
  let index = shuffledIndex;
  const combination = {};
  const elements = Object.entries(AVATAR_ELEMENTS).reverse();

  for (const [name, data] of elements) {
    const optionCount = data.options.length;
    combination[name] = data.options[index % optionCount];
    index = Math.floor(index / optionCount);
  }

  return combination;
};

/**
 * Find a suitable prime for shuffling
 */
const findSuitablePrime = (total) => {
  const primes = [17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97];

  for (const prime of primes) {
    if (prime < total && total % prime !== 0) {
      return prime;
    }
  }

  return 17; // fallback
};
```

### Result with shuffling

```
lightNumber 1  → index 0   → A-X-1-M
lightNumber 2  → index 17  → B-Y-2-N  ← Very different!
lightNumber 3  → index 34  → C-X-4-M  ← Very different!
```

### Pros & Cons

| Pros | Cons |
|------|------|
| No database needed | Limited to TOTAL combinations |
| Fast, O(1) | Deterministic (same input = same output) |
| Same lightNumber always = same avatar | Need to track lightNumber externally |

---

## Approach 2: Database Tracking (Random)

### How it works

Randomly generate combination, check if used, save to database.

```javascript
/**
 * Generate random unique combination
 * Requires database to track used combinations
 */
export const generateRandomUniqueCombination = async (supabase) => {
  const MAX_ATTEMPTS = 100;

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    // Random pick from each element
    const combination = {};
    Object.entries(AVATAR_ELEMENTS).forEach(([name, data]) => {
      const randomIndex = Math.floor(Math.random() * data.options.length);
      combination[name] = data.options[randomIndex];
    });

    // Create hash for uniqueness check
    const hash = createCombinationHash(combination);

    // Check database
    const { data: existing } = await supabase
      .from('used_avatar_combinations')
      .select('id')
      .eq('combination_hash', hash)
      .single();

    if (!existing) {
      // Not used yet - save and return
      await supabase
        .from('used_avatar_combinations')
        .insert({ combination_hash: hash });
      return combination;
    }
  }

  throw new Error('No unique combinations available!');
};

const createCombinationHash = (combination) => {
  return Object.values(combination).map(opt => opt.id).join('-');
  // Example: "bg1-sc2-d3-f1"
};
```

### Database Schema

```sql
CREATE TABLE used_avatar_combinations (
  id SERIAL PRIMARY KEY,
  combination_hash VARCHAR(100) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);
```

### Pros & Cons

| Pros | Cons |
|------|------|
| Truly random | Requires database |
| Can reset anytime | Slower (network calls) |
| Flexible | Gets slower as combinations fill up |

---

## Which Approach to Use?

| Situation | Recommended |
|-----------|-------------|
| Event has incrementing `lightNumber` | **Index-based with shuffle** |
| Want completely random avatars | **Database** |
| No backend/database available | **Index-based** |
| Need to reset easily | Both work |
| High volume (1000+ users) | **Index-based** (faster) |

---

## Quick Reference: Prime Number Selection

For shuffling to work correctly, choose a prime that:
1. Is smaller than total combinations
2. Does NOT divide total combinations evenly

```
Total = 48  → Use 17, 19, 23, 29, 31, 37, 41, 43, 47
Total = 54  → Use 17, 19, 23, 29, 31, 37, 41, 43, 47
Total = 100 → Use 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59...
```

---

## Example: Full Implementation

```javascript
// utils/avatarGenerator.js

import { AVATAR_ELEMENTS, getTotalCombinations } from '../config/avatarConfig';

const PRIMES = [17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73];

export const generateAvatar = (lightNumber) => {
  const total = getTotalCombinations();
  const prime = PRIMES.find(p => p < total && total % p !== 0) || 17;

  // Shuffle
  const shuffledIndex = ((lightNumber - 1) * prime) % total;

  // Decode
  let index = shuffledIndex;
  const result = {};
  const elements = Object.entries(AVATAR_ELEMENTS).reverse();

  for (const [name, data] of elements) {
    const count = data.options.length;
    result[name] = data.options[index % count];
    index = Math.floor(index / count);
  }

  return result;
};

// Usage
const avatar = generateAvatar(42);
// Returns: { background: {...}, scenery: {...}, diamond: {...}, frame: {...} }
```
