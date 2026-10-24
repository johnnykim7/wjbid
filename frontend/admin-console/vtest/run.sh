#!/bin/sh
# 가상 화면 시험 실행. 화면 레포의 vtest/run.sh 로 복사해 쓴다.
#
#   vtest/run.sh vtest/items/                     전체
#   vtest/run.sh vtest/items/IT-02.spec.tsx       영역 하나
#   vtest/run.sh vtest/items/IT-02.spec.tsx -t "(IT-02-004) "   항목 하나
#
# 시험 계정이 단일 세션이면 여러 창이 동시에 돌 때 서로 로그인을 끊는다. 서버 판정(run_server.py)과
# 같은 줄을 선다. LOCK 은 testbook.config.json 의 server.lock 과 같아야 한다.
LOCK=${TESTBOOK_LOCK:-/tmp/testbook-wjbid.lock}
WAIT=0
while ! mkdir "$LOCK" 2>/dev/null; do
  WAIT=$((WAIT + 5))
  if [ "$WAIT" -gt 900 ]; then
    echo "앞 시험이 15분 넘게 끝나지 않는다. $LOCK 을 지우고 다시 돌린다" >&2
    exit 1
  fi
  sleep 5
done
trap 'rmdir "$LOCK" 2>/dev/null' EXIT INT TERM
# 항목을 골라 돌릴 때(-t)는 그 영역의 회차 결과를 덮지 않고 vtest-out/one/ 에 쓴다.
# 합동시험 도중 한 건만 확인하는 쓰임이라 나머지 항목의 판정이 지워지면 안 된다.
for a in "$@"; do
  [ "$a" = "-t" ] && export VTEST_ONE=1
done
npx vitest run "$@" --config vitest.vtest.config.ts
