#!/usr/bin/env bash
# CR-145 운영 서버(59.8.160.12) ufw 에 도커 대역에서 8183 으로 오는 연결 예외를 더한다.
# 컨테이너가 서버 주소의 공개 포트로 들어오는 연결은 ufw 의 들어오는 규칙을 지난다.
# 2026-07-29 ufw 를 켤 때 이 길은 포트마다 예외를 두었고(5432, 3306, 8185, 6379) 8183 이 빠졌다.
# 같은 대역 세 개를 같은 방식으로 더한다. 바깥에서 오는 연결은 이 규칙에 걸리지 않는다.
#
# 실행(맥에서 한 줄):
#   scp -i ~/.ssh/id_ed25519 /Users/sykim/Documents/GitHub/dev-labs/bidding-agency-platform/docs/기록/운영설정_ufw_CR-145_8183.sh therecommerce@59.8.160.12:/tmp/cr145-fw.sh && ssh -t -i ~/.ssh/id_ed25519 therecommerce@59.8.160.12 'bash /tmp/cr145-fw.sh'
#
# 끝 문구(반드시 이 중 하나가 찍혀야 끝난 것이다):
#   CR145_FW_OK            규칙을 더했고 Aimbase 컨테이너에서 8183 이 열린다
#   CR145_FW_ROLLED_BACK   더한 뒤에도 열리지 않아 더한 규칙을 지웠다
#   CR145_FW_ABORTED       비밀번호 또는 백업 실패, 바꾼 것 없음
#   CR145_FW_ALREADY_OPEN  이미 열려 있다, 바꾼 것 없음

set -u
TS=$(date +%Y%m%d-%H%M%S)
DIR="$HOME/cr145-fw-$TS"
PORT=8183
NETS="172.16.0.0/12 192.168.0.0/16 10.0.0.0/8"
COMMENT="docker-internal-bidding-mcp CR-145"
mkdir -p "$DIR"

finish() {
  echo "$1 기록 $DIR"
  exit 0
}

# 이름 해석이나 앱 응답과 상관없이 연결이 서는지만 본다
probe() {
  docker exec aimbase-api-1 python3 -c "import socket;socket.create_connection(('59.8.160.12',$PORT),4);print('OPEN')" 2>/dev/null | grep -q OPEN
}

sudo -v || finish "CR145_FW_ABORTED 비밀번호 실패, 바꾼 것 없음."

sudo cp -p /etc/ufw/user.rules "/etc/ufw/user.rules.bak.cr145-$TS" \
  && sudo cp -p /etc/ufw/user6.rules "/etc/ufw/user6.rules.bak.cr145-$TS" \
  && sudo ufw status numbered > "$DIR/ufw-before.txt" \
  && sudo iptables-save > "$DIR/iptables-before.txt" \
  || finish "CR145_FW_ABORTED 백업 실패, 바꾼 것 없음."

if probe; then
  finish "CR145_FW_ALREADY_OPEN 이미 열려 있어 바꾼 것 없음."
fi

FAIL=0
for n in $NETS; do
  sudo ufw insert 1 allow from "$n" to any port "$PORT" proto tcp comment "$COMMENT" >> "$DIR/change.log" 2>&1 || FAIL=1
done
sudo ufw status numbered > "$DIR/ufw-after.txt"

sleep 2
if [ "$FAIL" = 0 ] && probe; then
  finish "CR145_FW_OK 8183 열림. 되돌리려면 같은 규칙을 ufw delete 로 지운다(백업 /etc/ufw/user.rules.bak.cr145-$TS)."
fi

for n in $NETS; do
  sudo ufw delete allow from "$n" to any port "$PORT" proto tcp >> "$DIR/rollback.log" 2>&1
done
sudo ufw status numbered > "$DIR/ufw-rolledback.txt"
finish "CR145_FW_ROLLED_BACK 더한 뒤에도 8183 이 닫혀 있어 더한 규칙을 지웠다."
