#!/usr/bin/env python3
"""convert_hwp.py — .hwp/.hwpx 파일을 마크다운/텍스트로 변환한다.

사용법:
    python convert_hwp.py <입력.hwp|입력.hwpx> [출력.md]

- .hwpx : ZIP+XML 구조이므로 표준 라이브러리만으로 직접 파싱 (표는 마크다운 표로 변환)
- .hwp  : pyhwp(hwp5txt/hwp5html) 사용. 미설치 시 자동으로 pip 설치 시도
- 실패 시 종료 코드 1 + 안내 메시지 → 사용자에게 PDF 변환 재업로드 요청할 것
- 변환 후 반드시 표(배점표/요구사항표) 무결성을 원문과 대조 확인할 것
"""
import sys, os, zipfile, subprocess, re
import xml.etree.ElementTree as ET


def convert_hwpx(path: str) -> str:
    """HWPX(OWPML) → 텍스트/마크다운. 표는 마크다운 표로 재구성."""
    out_parts = []
    with zipfile.ZipFile(path) as z:
        sections = sorted(n for n in z.namelist()
                          if re.match(r"Contents/section\d+\.xml", n))
        if not sections:
            raise RuntimeError("Contents/section*.xml 없음 — 올바른 HWPX가 아닐 수 있음")
        for sec in sections:
            root = ET.fromstring(z.read(sec))
            out_parts.append(_walk_hwpx(root))
    return "\n".join(out_parts)


def _local(tag: str) -> str:
    return tag.split("}")[-1]


def _text_of(el) -> str:
    """단락(p) 하나의 텍스트를 수집."""
    parts = []
    for node in el.iter():
        if _local(node.tag) == "t" and node.text:
            parts.append(node.text)
    return "".join(parts).strip()


def _walk_hwpx(root) -> str:
    lines = []
    def visit(el, in_table=False):
        tag = _local(el.tag)
        if tag == "tbl":
            lines.append(_table_md(el))
            return  # 표 내부 단락은 _table_md가 처리
        if tag == "p" and not in_table:
            t = _text_of(el)
            if t:
                lines.append(t)
            return
        for child in el:
            visit(child, in_table)
    visit(root)
    return "\n\n".join(lines)


def _table_md(tbl) -> str:
    rows = []
    for tr in tbl.iter():
        if _local(tr.tag) == "tr":
            cells = []
            for tc in tr:
                if _local(tc.tag) == "tc":
                    cell_text = " ".join(
                        _text_of(p) for p in tc.iter() if _local(p.tag) == "p"
                    ).strip()
                    cells.append(cell_text.replace("|", "／") or " ")
            if cells:
                rows.append(cells)
    if not rows:
        return ""
    width = max(len(r) for r in rows)
    rows = [r + [" "] * (width - len(r)) for r in rows]
    md = ["| " + " | ".join(rows[0]) + " |",
          "|" + "---|" * width]
    md += ["| " + " | ".join(r) + " |" for r in rows[1:]]
    return "\n".join(md)


def convert_hwp(path: str) -> str:
    """구형 바이너리 HWP → pyhwp의 hwp5txt 사용."""
    def run_hwp5txt():
        return subprocess.run(["hwp5txt", path], capture_output=True, text=True)
    r = None
    try:
        r = run_hwp5txt()
    except FileNotFoundError:
        print("[i] pyhwp 미설치 — 설치 시도 중...", file=sys.stderr)
        subprocess.run([sys.executable, "-m", "pip", "install", "--quiet",
                        "--break-system-packages", "pyhwp"], check=False)
        try:
            r = run_hwp5txt()
        except FileNotFoundError:
            raise RuntimeError("pyhwp 설치 실패")
    if r.returncode != 0:
        raise RuntimeError(f"hwp5txt 실패 (암호화/손상 가능): {r.stderr[:300]}")
    return r.stdout


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    src = sys.argv[1]
    dst = sys.argv[2] if len(sys.argv) > 2 else os.path.splitext(src)[0] + ".md"
    ext = os.path.splitext(src)[1].lower()
    try:
        if ext == ".hwpx":
            text = convert_hwpx(src)
        elif ext == ".hwp":
            text = convert_hwp(src)
        else:
            print(f"[!] 지원하지 않는 확장자: {ext} (hwp/hwpx만 지원)")
            sys.exit(1)
    except Exception as e:
        print(f"[!] 변환 실패: {e}")
        print("[!] 사용자에게 PDF로 변환 후 재업로드를 요청하세요.")
        sys.exit(1)

    if len(text.strip()) < 200:
        print("[!] 추출 텍스트가 비정상적으로 짧습니다. 이미지 기반 문서일 수 있습니다.")
        print("[!] 사용자에게 PDF 재업로드 또는 원문 확인을 요청하세요.")

    os.makedirs(os.path.dirname(dst) or ".", exist_ok=True)
    with open(dst, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"[OK] 변환 완료 → {dst} ({len(text):,}자)")
    print("[주의] 배점표·요구사항표 등 표 무결성을 원문과 반드시 대조 확인할 것")


if __name__ == "__main__":
    main()
