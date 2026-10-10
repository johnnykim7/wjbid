// AIDA 홈 첫 화면에서 왼쪽 칸, 오른쪽 칸, 다섯 칸 띠의 아래 끝과 넘친 글자를 잰다
// 사용: node measure8.js <playwright-core> <브라우저> <html> <해시,해시,...>
const [, , pwPath, exe, htmlPath, hashes] = process.argv;
const { chromium } = require(pwPath);
(async () => {
  const browser = await chromium.launch({ executablePath: exe, headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  for (const h of hashes.split(',')) {
    await page.goto('file://' + htmlPath + '#' + h + '-shot', { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    const r = await page.evaluate(() => {
      const b = (el) => (el ? Math.round(el.getBoundingClientRect().bottom) : null);
      const cols = [...document.querySelectorAll('.fs > div')].map((d) => {
        const vis = [...d.children].filter((c) => !c.hidden);
        return vis.map((c) => b(c));
      });
      const over = [...document.querySelectorAll('.fs *, .flow *')].filter((el) => {
        const r = el.getBoundingClientRect();
        return r.height > 0 && el.scrollWidth > el.clientWidth + 1 && getComputedStyle(el).overflow === 'visible' && el.children.length === 0;
      }).map((el) => el.textContent.trim().slice(0, 30));
      return { hd: b(document.querySelector('header.hd')), cols, flow: b(document.querySelector('.flow')), over: over.slice(0, 8) };
    });
    console.log(h, JSON.stringify(r));
  }
  await browser.close();
})();
