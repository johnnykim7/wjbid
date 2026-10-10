// 시안 HTML 을 상태(해시)별로 첫 화면(1440x900)과 전체 길이 PNG 로 뽑는다
// 사용: node shot7.js <playwright-core> <브라우저> <html> <출력 접두어> <해시,해시,...>
const [, , pwPath, exe, htmlPath, outPrefix, hashes] = process.argv;
const { chromium } = require(pwPath);
(async () => {
  const browser = await chromium.launch({ executablePath: exe, headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  for (const h of hashes.split(',')) {
    await page.goto('file://' + htmlPath + '#' + h + '-shot', { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(500);
    const tag = h.replace(/-/g, '_');
    await page.screenshot({ path: `${outPrefix}_${tag}_fold.png` });
    await page.screenshot({ path: `${outPrefix}_${tag}_full.png`, fullPage: true });
    const h2 = await page.evaluate(() => document.documentElement.scrollHeight);
    console.log(tag, h2);
  }
  await browser.close();
})();
