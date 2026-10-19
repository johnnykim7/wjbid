#!/usr/bin/env python3
"""capture_diagram.py — HTML 도식을 PNG로 캡처한다 (DOCX 삽입용).

사용법:
    python capture_diagram.py <도식.html> [출력.png]
    python capture_diagram.py <디렉토리>          # 디렉토리 내 모든 html 일괄 캡처

- #stage 요소를 2x 스케일로 캡처 (인쇄 품질)
- playwright(chromium)를 우선 사용, 없으면 시스템 chromium/google-chrome headless로 폴백
- 모두 실패하면 종료 코드 2 → HTML 원본을 산출물로 전달하고 사용자에게 브라우저 캡처 안내.
  ★캡처 실패로 전체 작업을 중단하지 말 것.
"""
import sys, os, glob, subprocess, shutil


def capture_playwright(html_path: str, png_path: str) -> bool:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        return False
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page(viewport={"width": 1700, "height": 1200},
                                    device_scale_factor=2)
            page.goto("file://" + os.path.abspath(html_path))
            page.wait_for_timeout(1200)  # 웹폰트 로딩 대기
            el = page.query_selector("#stage") or page.query_selector("body")
            el.screenshot(path=png_path)
            browser.close()
        return True
    except Exception as e:
        print(f"[i] playwright 캡처 실패: {e}", file=sys.stderr)
        return False


def capture_chrome_cli(html_path: str, png_path: str) -> bool:
    exe = shutil.which("chromium") or shutil.which("chromium-browser") \
        or shutil.which("google-chrome") or shutil.which("chrome")
    if not exe:
        return False
    r = subprocess.run([
        exe, "--headless=new", "--disable-gpu", "--no-sandbox",
        "--hide-scrollbars", "--force-device-scale-factor=2",
        "--window-size=1700,1400",
        f"--screenshot={png_path}",
        "file://" + os.path.abspath(html_path),
    ], capture_output=True, text=True)
    return r.returncode == 0 and os.path.exists(png_path)


def capture_one(html_path: str, png_path: str | None = None) -> bool:
    png_path = png_path or os.path.splitext(html_path)[0] + ".png"
    if capture_playwright(html_path, png_path) or capture_chrome_cli(html_path, png_path):
        print(f"[OK] {html_path} → {png_path}")
        return True
    print(f"[!] 캡처 실패: {html_path}")
    return False


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    target = sys.argv[1]
    if os.path.isdir(target):
        files = sorted(glob.glob(os.path.join(target, "*.html")))
        results = [capture_one(f) for f in files]
        ok, total = sum(results), len(results)
        print(f"\n{ok}/{total} 캡처 완료")
        sys.exit(0 if ok == total else 2)
    else:
        out = sys.argv[2] if len(sys.argv) > 2 else None
        ok = capture_one(target, out)
        if not ok:
            print("[안내] HTML 원본을 산출물로 전달하고, 사용자에게 브라우저에서")
            print("       열어 캡처하도록 안내하세요. 작업을 중단하지 마세요.")
        sys.exit(0 if ok else 2)


if __name__ == "__main__":
    main()
