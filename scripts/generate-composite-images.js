import sharp from 'sharp';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BASE_DIR = path.join(__dirname, '../src/assets/images/dmm/generatedImages');
const OUTPUT_DIR = path.join(__dirname, '../src/assets/images/dmm/output_test');

// Layer folders in order (bottom to top)
const LAYERS = [
  '1_mountains',
  '2_grass',
  '3_flowers',
  '4_moons',
  '5_music'
];

// Diamond shapes (layer 6)
const DIAMOND_SHAPES = ['asscher', 'emerald', 'heart', 'marquise', 'oval', 'pear', 'round'];

// Layer 7
const SUBHEADING_LAYER = '7_subheading';

// Progress bar helper
function progressBar(current, total, startTime, barLength = 40) {
  const percent = current / total;
  const filled = Math.round(barLength * percent);
  const empty = barLength - filled;
  const bar = '█'.repeat(filled) + '░'.repeat(empty);

  // Calculate ETA
  const elapsed = (Date.now() - startTime) / 1000; // seconds
  const rate = current / elapsed; // images per second
  const remaining = (total - current) / rate; // seconds remaining

  const formatTime = (seconds) => {
    if (!isFinite(seconds) || seconds < 0) return '--:--:--';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const elapsedStr = formatTime(elapsed);
  const etaStr = formatTime(remaining);
  const percentStr = (percent * 100).toFixed(1).padStart(5);

  process.stdout.write(`\r[${bar}] ${percentStr}% | ${current}/${total} | Elapsed: ${elapsedStr} | ETA: ${etaStr} | ${rate.toFixed(1)} img/s`);
}

// Get all images from a folder
function getImagesFromFolder(folderPath) {
  if (!fs.existsSync(folderPath)) return [];
  const files = fs.readdirSync(folderPath)
    .filter(f => f.endsWith('.webp') || f.endsWith('.png'))
    .sort() // Sort to ensure consistent ordering
    .map(f => path.join(folderPath, f));
  return files;
}

// Get diamond images organized by shape and variant
function getDiamondImages() {
  const diamondBase = path.join(BASE_DIR, '6_diamonds');
  const result = {};

  for (const shape of DIAMOND_SHAPES) {
    const shapePath = path.join(diamondBase, shape);
    const images = getImagesFromFolder(shapePath);
    result[shape] = images;
  }

  return result;
}

// Generate random combination for other layers
function getRandomLayerCombo(layerImages) {
  return layerImages.map(images => {
    const randomIndex = Math.floor(Math.random() * images.length);
    return images[randomIndex];
  });
}

// Composite images together - OPTIMIZED
async function compositeImages(imagePaths, outputPath) {
  // Giảm kích thước 50% để tiết kiệm RAM và tăng tốc
  const targetWidth = 1250;
  const targetHeight = 2231;

  // Prepare overlay layers với memory optimization
  const overlays = [];

  for (let i = 1; i < imagePaths.length; i++) {
    const buffer = await sharp(imagePaths[i], { limitInputPixels: false })
      .resize(targetWidth, targetHeight, { fit: 'cover', position: 'top' })
      .toBuffer();

    overlays.push({
      input: buffer,
      top: 0,
      left: 0
    });
  }

  // Process base layer và composite
  await sharp(imagePaths[0], { limitInputPixels: false })
    .resize(targetWidth, targetHeight, { fit: 'cover', position: 'top' })
    .composite(overlays)
    .webp({
      quality: 80,
      effort: 4,  // Giảm effort để tăng tốc
      smartSubsample: true
    })
    .toFile(outputPath);

  // Clear overlays để giải phóng memory
  overlays.length = 0;
}

async function main() {
  const TOTAL_IMAGES = 10; // Test với 10 ảnh trước

  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║           DMM COMPOSITE IMAGE GENERATOR                    ║');
  console.log('╚════════════════════════════════════════════════════════════╝\n');
  console.log(`Target: ${TOTAL_IMAGES.toLocaleString()} images\n`);

  // Get all layer images first
  console.log('=== Loading Resources ===\n');

  const layerImages = LAYERS.map(layer => {
    const images = getImagesFromFolder(path.join(BASE_DIR, layer));
    console.log(`  ${layer}: ${images.length} images`);
    return images;
  });

  // Get diamond images
  const diamondImages = getDiamondImages();
  console.log('\n  6_diamonds:');
  for (const shape of DIAMOND_SHAPES) {
    console.log(`    └── ${shape}: ${diamondImages[shape].length} variants`);
  }

  // Get subheading images
  const subheadingImages = getImagesFromFolder(path.join(BASE_DIR, SUBHEADING_LAYER));
  console.log(`\n  ${SUBHEADING_LAYER}: ${subheadingImages.length} images`);

  // Create output directory structure
  console.log('\n=== Creating Output Structure ===\n');

  // Structure: output/{shape}/{variant_number}/
  for (const shape of DIAMOND_SHAPES) {
    const variants = diamondImages[shape];
    for (let v = 0; v < variants.length; v++) {
      const variantDir = path.join(OUTPUT_DIR, shape, String(v + 1));
      if (!fs.existsSync(variantDir)) {
        fs.mkdirSync(variantDir, { recursive: true });
      }
    }
    console.log(`  output/${shape}/1, 2, 3 ✓`);
  }

  // Calculate distribution
  const imagesPerShape = Math.floor(TOTAL_IMAGES / DIAMOND_SHAPES.length);
  const extraImages = TOTAL_IMAGES % DIAMOND_SHAPES.length;

  console.log('\n=== Distribution Plan ===\n');
  console.log(`  Total shapes: ${DIAMOND_SHAPES.length}`);
  console.log(`  Images per shape: ~${imagesPerShape}`);
  console.log(`  Extra images distributed: ${extraImages}\n`);

  // Calculate and display plan
  const plan = [];
  for (let shapeIndex = 0; shapeIndex < DIAMOND_SHAPES.length; shapeIndex++) {
    const shape = DIAMOND_SHAPES[shapeIndex];
    const targetForShape = imagesPerShape + (shapeIndex < extraImages ? 1 : 0);
    const variants = diamondImages[shape];
    const numVariants = variants.length;
    const imagesPerVariant = Math.floor(targetForShape / numVariants);
    const extraVariantImages = targetForShape % numVariants;

    const variantCounts = [];
    for (let v = 0; v < numVariants; v++) {
      variantCounts.push(imagesPerVariant + (v < extraVariantImages ? 1 : 0));
    }

    plan.push({ shape, targetForShape, variantCounts });
    console.log(`  ${shape.padEnd(10)}: ${targetForShape} total → variant 1: ${variantCounts[0]}, variant 2: ${variantCounts[1]}, variant 3: ${variantCounts[2]}`);
  }

  // Track statistics
  const stats = {};
  for (const shape of DIAMOND_SHAPES) {
    stats[shape] = { total: 0, variants: [0, 0, 0] };
  }

  // Track all combinations to avoid duplicates
  const allCombinations = new Set();
  let totalGenerated = 0;
  const startTime = Date.now();

  console.log('\n=== Generating Images ===\n');

  // Generate images for each shape
  for (let shapeIndex = 0; shapeIndex < DIAMOND_SHAPES.length; shapeIndex++) {
    const shape = DIAMOND_SHAPES[shapeIndex];
    const { targetForShape, variantCounts } = plan[shapeIndex];
    const variants = diamondImages[shape];

    // Generate for each variant
    for (let variantIndex = 0; variantIndex < variants.length; variantIndex++) {
      const variantImage = variants[variantIndex];
      const variantNum = variantIndex + 1;
      const variantDir = path.join(OUTPUT_DIR, shape, String(variantNum));
      const targetForVariant = variantCounts[variantIndex];

      let variantCount = 0;
      let attempts = 0;
      const maxAttempts = targetForVariant * 20; // Prevent infinite loop

      while (variantCount < targetForVariant && attempts < maxAttempts) {
        attempts++;

        // Generate random combination for layers 1-5
        const baseLayers = getRandomLayerCombo(layerImages);

        // Pick subheading (random if multiple)
        const subheading = subheadingImages[Math.floor(Math.random() * subheadingImages.length)];

        // Create combination key
        const comboKey = [
          ...baseLayers.map(p => path.basename(p)),
          path.basename(variantImage),
          path.basename(subheading)
        ].join('|');

        // Skip if duplicate
        if (allCombinations.has(comboKey)) {
          continue;
        }

        allCombinations.add(comboKey);

        // Build full image path array
        const imagePaths = [
          ...baseLayers,
          variantImage,
          subheading
        ];

        // Generate filename
        const outputFileName = `${shape}-v${variantNum}-${String(variantCount + 1).padStart(4, '0')}.webp`;
        const outputPath = path.join(variantDir, outputFileName);

        // Composite and save
        await compositeImages(imagePaths, outputPath);

        variantCount++;
        totalGenerated++;
        stats[shape].total++;
        stats[shape].variants[variantIndex]++;

        // Update progress bar
        progressBar(totalGenerated, TOTAL_IMAGES, startTime);
      }
    }
  }

  // Final newline after progress bar
  console.log('\n');

  // Summary
  const totalTime = ((Date.now() - startTime) / 1000).toFixed(1);

  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║                    GENERATION COMPLETE                     ║');
  console.log('╚════════════════════════════════════════════════════════════╝\n');

  console.log(`  Total generated: ${totalGenerated.toLocaleString()} images`);
  console.log(`  Total time: ${totalTime} seconds`);
  console.log(`  Average speed: ${(totalGenerated / totalTime).toFixed(2)} images/second`);
  console.log(`  Output directory: ${OUTPUT_DIR}\n`);

  console.log('=== Summary by Shape & Variant ===\n');
  console.log('  Shape      │ Total  │ Variant 1 │ Variant 2 │ Variant 3');
  console.log('  ───────────┼────────┼───────────┼───────────┼───────────');

  for (const shape of DIAMOND_SHAPES) {
    const s = stats[shape];
    console.log(`  ${shape.padEnd(10)} │ ${String(s.total).padStart(6)} │ ${String(s.variants[0]).padStart(9)} │ ${String(s.variants[1]).padStart(9)} │ ${String(s.variants[2]).padStart(9)}`);
  }

  console.log('  ───────────┼────────┼───────────┼───────────┼───────────');
  const totals = DIAMOND_SHAPES.reduce((acc, shape) => {
    acc.total += stats[shape].total;
    acc.v1 += stats[shape].variants[0];
    acc.v2 += stats[shape].variants[1];
    acc.v3 += stats[shape].variants[2];
    return acc;
  }, { total: 0, v1: 0, v2: 0, v3: 0 });

  console.log(`  ${'TOTAL'.padEnd(10)} │ ${String(totals.total).padStart(6)} │ ${String(totals.v1).padStart(9)} │ ${String(totals.v2).padStart(9)} │ ${String(totals.v3).padStart(9)}`);

  console.log('\n=== Output Directory Structure ===\n');
  console.log('  output/');
  for (const shape of DIAMOND_SHAPES) {
    console.log(`  └── ${shape}/`);
    for (let v = 1; v <= 3; v++) {
      const variantDir = path.join(OUTPUT_DIR, shape, String(v));
      const files = fs.existsSync(variantDir) ? fs.readdirSync(variantDir).filter(f => f.endsWith('.webp')).length : 0;
      console.log(`      └── ${v}/ (${files} files)`);
    }
  }

  console.log('\n✅ Done!');
}

main().catch(console.error);
