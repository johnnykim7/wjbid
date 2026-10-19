# 영역 16 기본 스모크. 배포 직후 상용이 살아 있는지만 본다(사용자 10/13 07:27).
# 기능이 아직 바뀔 수 있어 화면 문구는 보지 않고 로그인, 공고 목록과 검색, 공고문 상세, AI 요약과 한글 번역만 본다.
# AI 는 상용에 숨겨 둔 시험 공고문 하나를 다시 분석시킨다. 고객에게 노출하지 않고, 같은 공고문이라 관리자 메일은 첫 회만 간다(사용자 10/13 07:44).
# SAM 수집은 수집 이력을 읽기만 한다. 수동 수집을 부르면 SAM.gov 하루 조회 한도를 쓰고 운영에 회차가 쌓인다(사용자 10/15 06:41).
# 근거는 T1-1 의 BID-AUTH-002, BID-BROWSE-001, BID-BROWSE-002, BID-REQ-003, BID-MKT-035, BID-ADMIN-001, BID-OPP-006, BID-OPP-001 이다.

USER = 'user:qa-cr143@example.com'
ADMIN = 'admin:smoke-admin@example.com'
OPEN = '/api/opportunities/collected?openOnly=true&latestOnly=true'
NOTICES_ALL = '/api/opportunities?includeExpired=true'
NOTICE = '/api/admin/notices/{smoke_notice_id}'
HANGUL = "any('가' <= c <= '힣' for c in {})"
RUNS = '/api/admin/collection/runs?size=20'
# 진행 중인 회차는 끝난 시각이 없어 성공 여부를 아직 모른다. 끝난 회차만 본다
DONE = "[x for x in r[0]['body']['content'] if x.get('completedAt')]"

ITEMS = [

dict(id='IT-16-001', flow='화면 열림', kind='정상', title='고객 화면과 관리자 화면이 열린다',
     pre='비로그인',
     steps='1. 고객 화면 첫 주소를 연다\n2. 관리자 화면 첫 주소를 연다',
     expect='1. 고객 화면 첫 페이지가 정상 응답(200)과 함께 HTML 문서로 내려온다\n2. 관리자 화면 첫 페이지가 정상 응답(200)과 함께 HTML 문서로 내려온다',
     judge=['서버', '서버'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/ , /admin/', pri='High', smoke=True,
     probe={1: dict(steps=[('api', 'guest', 'GET', '/')],
                    check="r[0]['status'] == 200 and '<html' in str(r[0]['body']).lower()"),
            2: dict(steps=[('api', 'guest', 'GET', '/admin/')],
                    check="r[0]['status'] == 200 and '<html' in str(r[0]['body']).lower()")}),

dict(id='IT-16-002', flow='로그인', kind='정상', title='시험 고객이 이메일과 비밀번호로 로그인한다',
     pre='상용 시험 고객 계정',
     steps='1. 시험 고객 이메일과 비밀번호로 로그인한다\n2. 로그인한 채로 내 정보를 연다',
     expect='1. 로그인되어 접속 토큰이 발급된다\n2. 내 정보의 이메일이 로그인한 시험 고객 이메일과 같다',
     judge=['서버', '서버'],
     req=['BID-AUTH-002'], fc=['BID-AUTH-002'], screen='/login', pri='High', smoke=True,
     probe={1: dict(steps=[('api', 'guest', 'POST', '/api/auth/login',
                            {'email': 'qa-cr143@example.com', 'password': '$QA_PASSWORD'})],
                    check="r[0]['status'] == 200 and len(r[0]['body'].get('accessToken') or '') > 20"),
            2: dict(steps=[('api', USER, 'GET', '/api/members/me')],
                    check="r[0]['status'] == 200 and r[0]['body']['email'] == 'qa-cr143@example.com'")}),

dict(id='IT-16-003', flow='공고 목록', kind='정상', title='마감 전 공고 목록이 한글 제목으로 나온다',
     pre='비로그인, 마감 전 공고가 1건 이상',
     steps='1. 마감 전 공고 목록을 연다',
     expect='1. 마감 전 공고가 1건 이상 나온다\n2. 줄마다 한글 제목과 마감 시각이 있다',
     judge=['서버', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High', smoke=True,
     probe={1: dict(steps=[('api', 'guest', 'GET', OPEN + '&size=50')],
                    check="r[0]['status'] == 200 and r[0]['body']['totalElements'] > 0 and len(r[0]['body']['content']) > 0"),
            2: dict(steps=[('api', 'guest', 'GET', OPEN + '&size=50')],
                    check="len(r[0]['body']['content']) > 0 and all(x.get('titleKo') and " + HANGUL.format("x['titleKo']") +
                          " and x.get('responseDeadline') for x in r[0]['body']['content'])")}),

dict(id='IT-16-004', flow='공고 검색', kind='정상', title='공고번호와 한글 제목으로 검색한다',
     pre='비로그인, 마감 전 공고가 1건 이상',
     steps='1. 마감 전 공고 목록 첫 줄의 공고번호로 검색한다\n2. 같은 줄의 한글 제목으로 검색한다',
     expect='1. 공고번호로 찾은 결과에 그 공고가 있고, 모든 줄이 그 공고번호를 담는다\n'
            '2. 한글 제목으로 찾은 결과에 그 공고가 있다',
     judge=['서버', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High', smoke=True,
     probe={1: dict(steps=[('api', 'guest', 'GET', OPEN + '&size=1'),
                           ('api', 'guest', 'GET', OPEN + '&q={val:0:solicitationNumber}')],
                    check="r[1]['status'] == 200 and r[1]['body']['totalElements'] >= 1 and "
                          "all(r[0]['body']['content'][0]['solicitationNumber'].lower() in (x['solicitationNumber'] or '').lower() "
                          "for x in r[1]['body']['content'])"),
            2: dict(steps=[('api', 'guest', 'GET', OPEN + '&size=1'),
                           ('api', 'guest', 'GET', OPEN + '&q={val:0:titleKo}')],
                    check="r[1]['status'] == 200 and "
                          "r[0]['body']['content'][0]['id'] in [x['id'] for x in r[1]['body']['content']]")}),

dict(id='IT-16-005', flow='공고문 상세', kind='정상', title='회원이 공고문 상세에서 AI 요약을 본다',
     pre='회원 기간 안인 시험 고객, 노출 중인 공고문 1건 이상(마감 지난 것 포함)',
     steps='1. 회원으로 마감 지난 것을 포함한 공고문 목록을 연다\n2. 첫 공고문 상세를 연다',
     expect='1. 공고문 목록이 1건 이상 나온다\n2. 상세에 한글 제목과 AI 요약(개요, 작업 범위)이 한국어로 채워져 있다',
     judge=['서버', '서버'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High', smoke=True,
     # 마감 전 공고문이 있는지는 관리자가 공개할 일이라 점검 실패로 치지 않는다(사용자 10/16 04:19). 회원이 상세와 AI 요약을 여는지만 본다
     probe={1: dict(steps=[('api', USER, 'GET', NOTICES_ALL)],
                    check="r[0]['status'] == 200 and r[0]['body']['totalElements'] > 0"),
            2: dict(steps=[('api', USER, 'GET', NOTICES_ALL),
                           ('api', USER, 'GET', '/api/opportunities/{val:0:id}')],
                    check="r[1]['status'] == 200 and " + HANGUL.format("r[1]['body']['title']") + " and " +
                          HANGUL.format("r[1]['body']['analysis']['summary']['overview']") + " and " +
                          HANGUL.format("r[1]['body']['analysis']['summary']['scope']"))}),

dict(id='IT-16-006', flow='내 입찰과 서류함', kind='정상', title='고객이 내 입찰 목록과 공통 서류함을 연다',
     pre='시험 고객으로 로그인',
     steps='1. 내 입찰 목록을 연다\n2. 공통 서류함을 연다',
     expect='1. 내 입찰 목록 조회가 정상 응답(200)과 함께 목록(0건 이상)을 돌려준다\n2. 공통 서류함에 고정 서류 칸 다섯이 나온다',
     judge=['서버', '서버'],
     req=['BID-REQ-003', 'BID-MKT-035'], fc=['BID-REQ-003', 'BID-MKT-035'], screen='/proposals , /vault',
     pri='High', smoke=True,
     probe={1: dict(steps=[('api', USER, 'GET', '/api/bid-requests/my')],
                    check="r[0]['status'] == 200 and 'content' in r[0]['body']"),
            2: dict(steps=[('api', USER, 'GET', '/api/member-documents')],
                    check="r[0]['status'] == 200 and len(r[0]['body']['fixedSlots']) == 5")}),

dict(id='IT-16-007', flow='관리자 목록', kind='정상', title='관리자가 대시보드와 주요 목록을 연다',
     pre='시험 관리자 계정',
     steps='1. 관리자로 로그인해 대시보드를 연다\n2. 입찰 요청 목록을 연다\n3. 원본 공고 목록을 연다\n4. 공고문 목록을 연다',
     expect='1. 대시보드 상태별 입찰 수가 나온다\n2. 입찰 요청 목록 조회가 정상 응답(200)과 함께 목록(0건 이상)을 돌려준다\n'
            '3. 원본 공고 목록이 1건 이상 나온다\n4. 공고문 목록이 1건 이상 나온다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /dashboard', pri='High', smoke=True,
     probe={1: dict(steps=[('api', ADMIN, 'GET', '/api/admin/bid-requests/stats')],
                    check="r[0]['status'] == 200 and len(r[0]['body']) > 0 and all(int(v) == v for v in r[0]['body'].values())"),
            2: dict(steps=[('api', ADMIN, 'GET', '/api/admin/bid-requests')],
                    check="r[0]['status'] == 200 and 'content' in r[0]['body']"),
            3: dict(steps=[('api', ADMIN, 'GET', '/api/admin/opportunities')],
                    check="r[0]['status'] == 200 and r[0]['body']['totalElements'] > 0"),
            4: dict(steps=[('api', ADMIN, 'GET', '/api/admin/notices')],
                    check="r[0]['status'] == 200 and r[0]['body']['totalElements'] > 0")}),

dict(id='IT-16-008', flow='AI 요약과 한글 번역', kind='정상', title='AI 가 시험 공고문을 다시 요약하고 한글로 옮긴다',
     pre='시험 관리자 계정, 상용에 숨겨 둔 시험 공고문(서버별 값 smoke_notice_id), Aimbase 연결',
     steps='1. 관리자 공고문 상세에서 시험 공고문의 한글화 재생성을 누른다\n2. 분석이 끝날 때까지 기다린다\n'
           '3. 시험 공고문 상세를 연다',
     expect='1. 재생성 요청이 받아들여진다\n2. 20분 안에 분석이 완료로 끝나고, 끝난 시각이 이번 점검을 시작한 뒤다\n'
            '3. 한글 제목이 한국어로 채워진다\n4. 공고 요약의 개요, 작업 범위, 참여 자격이 한국어로 채워진다\n'
            '5. 시험 공고문은 고객에게 노출되지 않은 채로 남는다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High', smoke=True,
     probe={1: dict(steps=[('api', ADMIN, 'POST', NOTICE + '/regenerate')],
                    check="r[0]['status'] == 202"),
            2: dict(steps=[('poll', ADMIN, NOTICE, "x['status'] != 200 or x['body']['generationStatus'] != 'ANALYZING'",
                            1200, 15)],
                    # 다시 분석이 시작되지 않아도 전날 결과가 완료로 남아 있다. 끝난 시각으로 이번에 돈 것인지 가른다
                    check="r[0]['status'] == 200 and r[0]['body']['generationStatus'] == 'COMPLETED' "
                          "and (r[0]['body']['analyzedAt'] or '')[:19] >= RUN_STARTED"),
            3: dict(steps=[('api', ADMIN, 'GET', NOTICE)],
                    check="r[0]['status'] == 200 and " + HANGUL.format("r[0]['body']['koreanTitle'] or ''")),
            4: dict(steps=[('api', ADMIN, 'GET', NOTICE)],
                    check="r[0]['status'] == 200 and " + " and ".join(HANGUL.format(f"r[0]['body']['analysis']['summary']['{k}'] or ''")
                                       for k in ('overview', 'scope', 'eligibility'))),
            5: dict(steps=[('api', ADMIN, 'GET', NOTICE)],
                    check="r[0]['status'] == 200 and r[0]['body']['visibility'] != 'VISIBLE'")}),

dict(id='IT-16-009', flow='SAM 수집', kind='정상', title='마지막 SAM.gov 수집이 하루 안에 성공으로 끝났다',
     pre='시험 관리자 계정, 스케줄 수집이 켜진 서버(하루 네 번 06시, 12시, 18시, 23시)',
     steps='1. 관리자로 수집 이력 조회(GET /api/admin/collection/runs)를 부른다\n2. 끝난 회차 중 가장 최근 회차를 본다',
     expect='1. 가장 최근에 끝난 수집 회차의 시작 시각이 지금으로부터 24시간 안이다\n'
            '2. 그 회차의 성공 여부가 성공(success=true)이다',
     judge=['서버', '서버'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='관리자 /collection', pri='High', smoke=True,
     probe={1: dict(steps=[('api', ADMIN, 'GET', RUNS)],
                    check=f"r[0]['status'] == 200 and len({DONE}) > 0 and {DONE}[0]['startedAt'][:19] >= SINCE_24H"),
            2: dict(steps=[('api', ADMIN, 'GET', RUNS)],
                    check=f"r[0]['status'] == 200 and len({DONE}) > 0 and {DONE}[0]['success'] is True")}),

]
