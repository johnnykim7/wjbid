#!/bin/bash
# deploy.sh - bidding-agency 운영 배포 스크립트 (CI/CD 도입 전 임시)
# 배포 디렉토리: /home/therecommerce/bidding-agency/
#   ├── bidding-agency.jar       (BE fat jar — compose가 볼륨 마운트)
#   ├── docker-compose.yml       (prod compose — 환경변수 인라인)
#   ├── customer-portal/         (FE 정적 파일, 외부 nginx가 서빙)
#   ├── admin-console/           (FE 정적 파일, 외부 nginx가 서빙)
#   ├── storage/                 (rfp-samples 등, 서버에서 관리 — 건드리지 않음)
#   └── logs/
#
# BE 는 jre 이미지에 jar 를 볼륨 마운트하는 방식 → 이미지 빌드 불필요.
# jar(+compose) 교체 후 컨테이너만 재기동(--force-recreate).
#
# 사용법: ./deploy.sh         (FE + BE 모두)
#         ./deploy.sh fe      (FE 2종만 — customer-portal + admin-console)
#         ./deploy.sh be      (BE만 — jar + compose 교체 + 컨테이너 재기동)
set -e

TARGET="${1:-all}"
SERVER="therecommerce@59.8.160.12"
SSH_KEY="$HOME/.ssh/id_ed25519"
REMOTE="/home/therecommerce/bidding-agency"

# ── 헬퍼 함수 ───────────────────────────────────────────
ssh_run()  { ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o IdentitiesOnly=yes "$SERVER" "$@"; }
scp_send() { scp -i "$SSH_KEY" -o StrictHostKeyChecking=no -o IdentitiesOnly=yes "$@"; }

deploy_fe() {
  echo "━━━ FE 빌드 (customer-portal) ━━━"
  ( cd frontend/customer-portal && npm run build )
  echo "━━━ FE 빌드 (admin-console) ━━━"
  ( cd frontend/admin-console && npm run build )

  echo "━━━ FE 배포 ━━━"
  ssh_run "mkdir -p $REMOTE/customer-portal $REMOTE/admin-console"
  ssh_run "rm -rf $REMOTE/customer-portal/* $REMOTE/admin-console/*"
  scp_send -r frontend/customer-portal/dist/. "$SERVER:$REMOTE/customer-portal/"
  scp_send -r frontend/admin-console/dist/.   "$SERVER:$REMOTE/admin-console/"
  echo "✅ FE 완료 → $REMOTE/{customer-portal,admin-console}/"
}

deploy_be() {
  echo "━━━ BE 빌드 (bootJar) ━━━"
  ( cd backend && ./gradlew bootJar -x test )

  echo "━━━ BE 배포 & 컨테이너 재기동 ━━━"
  FAT_JAR=$(find backend/build/libs -name '*.jar' ! -name '*plain*' | head -1)
  if [ -z "$FAT_JAR" ]; then echo "❌ fat jar 를 찾을 수 없습니다"; exit 1; fi
  echo "  jar = $FAT_JAR"
  ssh_run "mkdir -p $REMOTE"
  scp_send "$FAT_JAR"             "$SERVER:$REMOTE/bidding-agency.jar"
  scp_send docker-compose.prod.yml "$SERVER:$REMOTE/docker-compose.yml"
  ssh_run "cd $REMOTE && docker compose up -d --force-recreate bidding-agency"
  echo "✅ BE 완료 → $REMOTE/bidding-agency.jar"
}

case "$TARGET" in
  fe)  deploy_fe ;;
  be)  deploy_be ;;
  all) deploy_fe && deploy_be ;;
  *)   echo "사용법: ./deploy.sh [fe|be|all]"; exit 1 ;;
esac

echo ""
echo "✅ 배포 완료 → https://woojinusbid.com (BE: http://59.8.160.12:8183/api)"
