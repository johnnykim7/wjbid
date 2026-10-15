#!/bin/bash
# 상용 스모크를 테스트 서버에서 돌리고, 한 항목이라도 통과하지 못하면 관리자에게 메일을 보낸다.
# 테스트 서버 cron 이 매일 부른다. 서버에 python 3.12 가 없어 도커 이미지로 돈다.
set -u
HOME_DIR="$(cd "$(dirname "$0")" && pwd)"
STARTED="$(TZ=Asia/Seoul date +%Y%m%d_%H%M%S)"
mkdir -p "$HOME_DIR/logs"
# 결과 파일을 서버 계정 것으로 남겨야 사람이 지우고 옮길 수 있다
docker run --rm \
  --user "$(id -u):$(id -g)" \
  --network host \
  --env-file "$HOME_DIR/smoke.env" \
  -e TZ=Asia/Seoul \
  -e TESTBOOK_DIR=/smoke/testbook \
  -e CLAUDE_PLUGIN_ROOT=/smoke/plugin \
  -e SMOKE_STARTED="$STARTED" \
  -v "$HOME_DIR:/smoke" \
  -w /smoke \
  python:3.12-slim \
  sh -c 'python3 plugin/skills/joint-test/bin/smoke.py --only 서버; python3 alert.py' \
  >"$HOME_DIR/logs/$STARTED.log" 2>&1
