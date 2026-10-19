"""서버 판정(run_server.py)이 조회에 끼우는 서버별 값. 경로와 SQL 에는 {smoke_notice_id}, check·need 에는 SMOKE_NOTICE_ID 로 쓴다.

서버 이름은 TESTBOOK_SERVER 로 주고, 비우면 상용이다.
"""
import os
from datetime import datetime, timedelta, timezone

# 스모크 AI 확인(IT-16-008)이 다시 분석시키는 시험 공고문. 서버마다 따로 만들어 두므로 항목에 박지 않는다.
# 고객에게 노출하지 않는 공고문이다. 지우면 스모크가 그 줄을 실행 오류로 낸다
SMOKE_NOTICE = {
    '상용': os.environ.get('WJBID_SMOKE_NOTICE_ID_PROD', ''),
}

SERVER = os.environ.get('TESTBOOK_SERVER') or '상용'

# 수집 회차 시각은 서버가 서울 시각으로 존 없이 저장한다. 같은 꼴로 만들어야 글자 비교가 시각 비교가 된다
KST = timezone(timedelta(hours=9))

NOW = datetime.now(KST)

# 한 번만 읽히므로 run_started 는 이번 점검을 시작한 시각이다. 다시 분석이 그날 실제로 돌았는지 가르는 기준이 된다
VARS = {
    'smoke_notice_id': SMOKE_NOTICE.get(SERVER, ''),
    'since_24h': (NOW - timedelta(hours=24)).strftime('%Y-%m-%dT%H:%M:%S'),
    'run_started': NOW.strftime('%Y-%m-%dT%H:%M:%S'),
}
