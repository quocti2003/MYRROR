import QRCode from 'qrcode';

const url = 'https://masothue.com/0319135410-cong-ty-tnhh-so-it';

async function generateQRCodes() {
  const fileName = 'qr-masothue-0319135410.png';

  await QRCode.toFile(fileName, url, {
    type: 'png',
    errorCorrectionLevel: 'H',
    width: 512,
    margin: 4,
    color: {
      dark: '#000000',
      light: '#FFFFFF'
    }
  });

  console.log(`✓ Generated ${fileName}`);
  console.log(`  URL: ${url}`);
}

generateQRCodes().catch(console.error);
