#!/usr/bin/env python3
"""check_coverage.py — 컴플라이언스 매트릭스의 REQ ID가 산출물에 매핑됐는지 검증한다.

사용법:
    python check_coverage.py [프로젝트루트] [--stage outline|plan|body]

    프로젝트루트 기본값: bid-project (현재 디렉토리 기준)
    --stage 생략 시 세 단계 모두 검사

검사 대상:
    outline : 4-outline/            (목차 전략표에 REQ가 배정됐는가)
    plan    : 5-section-plans/      (섹션 기획서에 REQ가 반영됐는가)
    body    : 6-proposal/sections/  (본문에 REQ 주석 <!-- REQ-xxx -->가 있는가)

출력: 단계별 커버율 + 누락 REQ 리스트 (누락 존재 시 종료 코드 1)
"""
import sys, os, re, glob

REQ_RE = re.compile(r"REQ-\d{3,}")


def extract_matrix_ids(matrix_path: str) -> set[str]:
    with open(matrix_path, encoding="utf-8") as f:
        text = f.read()
    ids = set()
    for line in text.splitlines():
        # 매트릭스 표의 첫 열에 있는 REQ만 (질의 후보 섹션 등 중복 무해)
        m = REQ_RE.search(line)
        if m and line.strip().startswith("|"):
            ids.add(m.group())
    return ids


def extract_ids_from_dir(path: str) -> set[str]:
    ids = set()
    for f in glob.glob(os.path.join(path, "**", "*.md"), recursive=True):
        with open(f, encoding="utf-8") as fh:
            ids.update(REQ_RE.findall(fh.read()))
    return ids


def report(stage: str, matrix: set, found: set) -> bool:
    covered = matrix & found
    missing = sorted(matrix - found)
    rate = len(covered) / len(matrix) * 100 if matrix else 0
    print(f"\n[{stage}] 커버리지: {len(covered)}/{len(matrix)} ({rate:.1f}%)")
    if missing:
        print(f"  누락 REQ ({len(missing)}개):")
        for r in missing:
            print(f"    - {r}")
    else:
        print("  ✅ 누락 없음")
    # 매트릭스에 없는 유령 ID 경고
    ghost = sorted(found - matrix)
    if ghost:
        print(f"  ⚠️ 매트릭스에 없는 ID 발견 (오타 가능): {', '.join(ghost)}")
    return not missing


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    root = args[0] if args else "bid-project"
    stage = None
    for a in sys.argv[1:]:
        if a.startswith("--stage"):
            stage = a.split("=")[-1] if "=" in a else None
    if "--stage" in sys.argv:
        i = sys.argv.index("--stage")
        if i + 1 < len(sys.argv):
            stage = sys.argv[i + 1]

    matrix_path = os.path.join(root, "1-analysis", "compliance-matrix.md")
    if not os.path.exists(matrix_path):
        print(f"[!] 매트릭스 없음: {matrix_path} — 1단계를 먼저 실행하세요.")
        sys.exit(1)
    matrix = extract_matrix_ids(matrix_path)
    print(f"매트릭스 REQ 총 {len(matrix)}개")

    targets = {
        "outline": os.path.join(root, "4-outline"),
        "plan": os.path.join(root, "5-section-plans"),
        "body": os.path.join(root, "6-proposal", "sections"),
    }
    if stage:
        targets = {stage: targets[stage]}

    all_ok = True
    for name, path in targets.items():
        if not os.path.isdir(path):
            print(f"\n[{name}] 디렉토리 없음 ({path}) — 건너뜀")
            continue
        ok = report(name, matrix, extract_ids_from_dir(path))
        all_ok = all_ok and ok

    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
