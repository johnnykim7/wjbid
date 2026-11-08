#!/bin/bash
# 상용 MariaDB 읽기 전용 조회(TESTBOOK_DB_CMD). 표준입력의 SQL 을 받아 머리행 있는 탭 구분 결과를 낸다.
# 계정과 비밀번호는 상용 서버 안 컨테이너 환경에서 읽어 이 기기로 가져오지 않는다. 알림 서버 기록(notification_db.)을 읽는 조회는 알림 서버 계정으로 돈다
S=$(cd "$(dirname "$0")" && pwd)
sql=$(cat)
if grep -q 'notification_db\.' <<<"$sql"; then who=N; else who=B; fi
{ echo "WHO=$who"; cat "$S/prod_db_remote.sh"
  echo 'docker exec -i -e MYSQL_PWD="$P" wjbid-infra-mariadb-1 mariadb -u"$U" -B --init-command="SET SESSION TRANSACTION READ ONLY" "$DB" <<'"'"'__SQL_186__'"'"''
  printf '%s\n' "$sql"; echo '__SQL_186__'
} | ssh -i ~/.ssh/wjbid-prod.pem -o ConnectTimeout=10 -o BatchMode=yes ubuntu@54.116.150.247 'bash -s'
