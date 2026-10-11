#!/bin/bash
# deploy_aws.sh - WJbid 상용(AWS) 배포. CI/CD 를 갖추기 전까지 쓴다.
#
# 커밋된 코드만 올린다. 고른 커밋(기본 HEAD)을 임시 worktree 로 꺼내 빌드하므로
# 작업 폴더에 섞인 다른 창의 미커밋 수정은 들어가지 않는다.
#
# 서버 /opt/wjbid/app
#   bidding-agency.jar    BE fat jar, compose 가 볼륨으로 건다
#   docker-compose.yml    레포 docker-compose.aws.yml 을 올린다
#   .env                  비밀값. 서버에서만 관리하고 이 스크립트는 건드리지 않는다
#   customer-portal/      고객 화면 (nginx /)
#   admin-console/        관리자 화면 (nginx /admin/)
#   storage/, logs/       건드리지 않는다
# 바꾸기 전 것은 이름 끝에 .prev 를 붙여 한 벌 남긴다.
#
# 사용법: ./deploy_aws.sh [all|fe|be] [커밋]
#   ./deploy_aws.sh             HEAD 를 BE, FE 순서로
#   ./deploy_aws.sh be          HEAD 의 BE 만
#   ./deploy_aws.sh fe a848aa3  지정 커밋의 FE 만
set -euo pipefail

TARGET="${1:-all}"
REF="${2:-HEAD}"
SERVER="ubuntu@54.116.150.247"
SSH_KEY="$HOME/.ssh/wjbid-prod.pem"
REMOTE="/opt/wjbid/app"
SITE="https://woojinusbid.com"
HEALTH_URL="http://127.0.0.1:8183/api/actuator/health"
HEALTH_WAIT_SEC=180
FE_APPS="customer-portal admin-console"

case "$TARGET" in
  all|fe|be) ;;
  *) echo "사용법: ./deploy_aws.sh [all|fe|be] [커밋]"; exit 1 ;;
esac

ROOT="$(cd "$(dirname "$0")" && pwd)"
SHA="$(git -C "$ROOT" rev-parse --verify "$REF^{commit}")"

ssh_run()  { ssh -i "$SSH_KEY" -o IdentitiesOnly=yes -o ConnectTimeout=10 "$SERVER" "$@"; }
scp_send() { scp -q -i "$SSH_KEY" -o IdentitiesOnly=yes "$@"; }

check_node() {
  # Vite 7 은 Node 20.19 또는 22.12 이상이 필요하다. 낮으면 빌드가 엉뚱한 오류로 멈춘다
  local v major minor
  v="$(node -p 'process.versions.node' 2>/dev/null || echo 0.0.0)"
  major="${v%%.*}"; minor="$(echo "$v" | cut -d. -f2)"
  if [ "$major" -gt 22 ] || { [ "$major" -eq 22 ] && [ "$minor" -ge 12 ]; } || { [ "$major" -eq 20 ] && [ "$minor" -ge 19 ]; }; then
    return 0
  fi
  echo "Node $v 로는 화면을 빌드할 수 없습니다. Node 22 를 PATH 앞에 두고 다시 실행하세요."
  exit 1
}

build_be() {
  echo "[BE 빌드] $SHORT"
  # gradle wrapper 는 .gitignore 대상이라 worktree 에 없어 작업 폴더 것을 쓴다
  cp "$ROOT/backend/gradlew" "$WT/backend/"
  cp -R "$ROOT/backend/gradle" "$WT/backend/"
  ( cd "$WT/backend" && ./gradlew bootJar -x test -q )
  JAR="$(find "$WT/backend/build/libs" -name '*.jar' ! -name '*plain*' | head -1)"
  if [ -z "$JAR" ]; then echo "BE jar 를 찾지 못했습니다"; exit 1; fi
  if [ ! -f "$WT/docker-compose.aws.yml" ]; then echo "커밋 $SHORT 에 docker-compose.aws.yml 이 없습니다"; exit 1; fi
}

build_fe() {
  check_node
  for app in $FE_APPS; do
    echo "[FE 빌드] $app $SHORT"
    # 패키지는 작업 폴더에 받아 둔 것을 링크해 다시 받지 않는다
    ln -s "$ROOT/frontend/$app/node_modules" "$WT/frontend/$app/node_modules"
    ( cd "$WT/frontend/$app" && npm run build )
  done
}

wait_health() {
  echo "[BE 기동 대기] 최대 ${HEALTH_WAIT_SEC}초"
  if ssh_run "for i in \$(seq 1 $HEALTH_WAIT_SEC); do curl -sf -o /dev/null $HEALTH_URL && exit 0; sleep 1; done; exit 1"; then
    echo "[BE 기동 확인]"
    return 0
  fi
  echo "BE 가 ${HEALTH_WAIT_SEC}초 안에 뜨지 않았습니다. 최근 로그:"
  ssh_run "cd $REMOTE && docker compose logs --tail 80 bidding-agency" || true
  echo "되돌리려면 서버 $REMOTE 에서 bidding-agency.jar.prev, docker-compose.yml.prev 를 제자리로 옮기고 docker compose up -d --force-recreate bidding-agency"
  exit 1
}

deploy_be() {
  echo "[BE 배포]"
  scp_send "$JAR" "$SERVER:$REMOTE/bidding-agency.jar.new"
  scp_send "$WT/docker-compose.aws.yml" "$SERVER:$REMOTE/docker-compose.yml.new"
  echo "[서버 compose 와 달라지는 곳, 없으면 빈칸]"
  ssh_run "cd $REMOTE && diff -u docker-compose.yml docker-compose.yml.new || true"
  ssh_run "cd $REMOTE \
    && mv bidding-agency.jar bidding-agency.jar.prev && mv bidding-agency.jar.new bidding-agency.jar \
    && mv docker-compose.yml docker-compose.yml.prev && mv docker-compose.yml.new docker-compose.yml \
    && docker compose up -d --force-recreate bidding-agency"
  wait_health
}

deploy_fe() {
  echo "[FE 배포]"
  for app in $FE_APPS; do
    ssh_run "rm -rf $REMOTE/$app.new && mkdir -p $REMOTE/$app.new"
    scp_send -r "$WT/frontend/$app/dist/." "$SERVER:$REMOTE/$app.new/"
  done
  # 두 화면을 다 올린 뒤에 한꺼번에 바꿔 끼워 올리는 동안 화면이 비지 않게 한다
  ssh_run "cd $REMOTE && for a in $FE_APPS; do rm -rf \$a.prev; if [ -d \$a ]; then mv \$a \$a.prev; fi; mv \$a.new \$a; done"
}

if ! ssh_run true; then
  echo "상용 서버에 접속하지 못했습니다. SSH 는 보안 그룹에 허용한 IP 에서만 열립니다."
  exit 1
fi

SHORT="$(git -C "$ROOT" rev-parse --short "$SHA")"
WORK="$(mktemp -d)"
WT="$WORK/src"
cleanup() {
  git -C "$ROOT" worktree remove --force "$WT" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT
git -C "$ROOT" worktree add --detach --quiet "$WT" "$SHA"

# 하나라도 빌드가 깨지면 아무것도 올리지 않도록 빌드를 먼저 다 끝낸다
case "$TARGET" in
  be)  build_be ;;
  fe)  build_fe ;;
  all) build_be; build_fe ;;
esac

# 새 화면이 새 API 를 부를 수 있어 BE 를 먼저 올린다
case "$TARGET" in
  be)  deploy_be ;;
  fe)  deploy_fe ;;
  all) deploy_be; deploy_fe ;;
esac

ssh_run "echo \"\$(date '+%Y-%m-%d %H:%M:%S %Z') $SHA $TARGET\" >> $REMOTE/deploy-history.log"
echo "[배포 완료] $SITE  커밋 $SHORT ($TARGET)"
