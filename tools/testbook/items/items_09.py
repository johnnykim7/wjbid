# 영역 09 공고 수집 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19, T1-3 업무 규칙, T1-5 상태 정의, T3-1 데이터 모델, T3-2 API 설계, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-09-001', flow='스케줄 수집', kind='정상', title='스케줄 수집이 활성 수집 키워드마다 공고를 모아 기록한다',
     pre='관리자 계정, 활성 수집 키워드 1개 이상, SAM.gov 호출 한도가 남아 있음, 다음 스케줄 수집 시각 직전',
     steps='1. 활성 수집 키워드(keyword_groups 의 active true) 목록을 적어 둔다\n2. 스케줄 수집 시각이 지나 수집이 끝나기를 기다린다\n3. 수집 실행 이력(collector_runs)과 공고(opportunities)를 조회한다',
     expect='1. 수집 실행 이력에 triggered_by SCHEDULED 인 행이 이번 시각으로 더해지고 success 가 true 이다\n'
            '2. 이번 회차 실행 이력 행의 keyword 가 1단계에서 적은 활성 키워드와 같다\n'
            '3. 이번 회차 실행 이력의 new_count 가 이번 회차에 first_seen_at 이 찍힌 공고 행 수와 같다\n'
            '4. 이번 회차에 새로 생긴 공고 행의 visibility 가 HIDDEN 이다\n'
            '5. 이번 회차에 새로 생긴 공고 행의 raw_json 에 SAM.gov 원본 JSON 이 고치지 않은 채 들어 있다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='', pri='High'),

dict(id='IT-09-002', flow='스케줄 수집', kind='정상', title='이미 모은 공고가 있으면 최신 게시일 하루 전부터 받는다',
     pre='관리자 계정, 모은 공고가 1건 이상 있는 서버',
     steps='1. 모은 공고 가운데 가장 늦은 게시일을 확인한다\n2. 스케줄 수집 한 회차를 돌린다',
     expect='1. SAM.gov 검색 요청의 게시일 시작이 1단계의 최신 게시일에서 하루를 뺀 날이다',
     judge=['단위'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='', pri='High'),

dict(id='IT-09-003', flow='스케줄 수집', kind='정상', title='모은 공고가 없으면 최근 7일을 받는다',
     pre='관리자 계정, 모은 공고가 0건인 서버',
     steps='1. 스케줄 수집 한 회차를 돌린다',
     expect='1. SAM.gov 검색 요청의 게시일 시작이 실행일에서 7일을 뺀 날이다',
     judge=['단위'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='', pri='High'),

dict(id='IT-09-004', flow='스케줄 수집', kind='정상', title='내용이 같은 공고는 새로 넣지 않고 갱신한다',
     pre='관리자 계정, SAM.gov 호출 한도가 2회 이상 남아 있음',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 수집 기간 1 로 [수집 시작] 을 눌러 한 회차를 끝낸다\n2. 1단계에서 새로 생긴 공고의 notice_id 와 first_seen_at 을 적어 둔다\n3. 같은 기간으로 [수집 시작] 을 한 번 더 눌러 회차를 끝낸다',
     expect='1. 2단계에서 적은 notice_id 마다 공고 행이 하나뿐이다\n'
            '2. 2단계에서 적은 공고 행의 first_seen_at 이 1단계 회차 시각 그대로이다',
     judge=['서버', '서버'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='관리자 /collection', pri='High'),

dict(id='IT-09-005', flow='공고 본문 한글화', kind='정상', title='새로 모은 공고는 원문 본문을 받아 한글로 옮겨 둔다',
     pre='관리자 계정과 무료회원 계정, SAM.gov 호출 한도가 남아 있음, 이번 회차에 새 공고가 들어올 기간',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 끝낸다\n2. 이번 회차에 새로 생긴 공고 하나의 id 를 적는다\n3. 고객 화면에 무료회원으로 로그인해 원본 공고 상세 /search/origin/{그 id} 를 연다\n4. [원문 보기] 를 누른다',
     expect='1. 그 공고 행의 title_ko 에 한글 제목이 들어 있다\n'
            '2. 그 공고 행의 description_body 에 SAM.gov 원문 본문이 들어 있다\n'
            '3. 그 공고 행의 description_summary_ko 에 원문 본문을 한글로 옮긴 글이 들어 있다\n'
            '4. 원본 공고 상세 본문 자리에 description_summary_ko 의 한글 본문이 줄바꿈 그대로 먼저 보이고 그 아래 [원문 보기] 가 있다\n'
            '5. 머리 아래 안내가 "아직 한글 공고문이 없는 공고라 SAM.gov 원문을 한글로 옮겨 보여 드립니다" 이다\n'
            '6. [원문 보기] 를 누르면 description_body 의 원문 본문이 펼쳐지고 다시 누르면 접힌다\n'
            '7. 한글 본문이 원문 본문의 뜻을 빠짐없이 옮긴 글이다\n'
            '8. 공고 하나에 SAM.gov 본문 조회를 한 번, 번역을 한 번 부른다',
     judge=['서버', '서버', '서버', '가상', '가상', '가상', '합동', '단위'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-09-006', flow='공고 본문 한글화', kind='예외', title='본문 조회가 실패해도 수집은 끝나고 이미 있는 본문을 덮지 않는다',
     pre='관리자 계정, description_body 와 description_summary_ko 가 채워진 공고 1건, SAM.gov 본문 조회가 실패하도록 막은 환경',
     steps='1. 그 공고가 다시 받아지는 수집 회차를 돌린다',
     expect='1. 그 회차의 수집 실행 이력 success 가 true 이다\n'
            '2. 그 공고 행의 description_body 와 description_summary_ko 가 수집 전 값 그대로이다',
     judge=['단위', '단위'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='', pri='High'),

dict(id='IT-09-007', flow='공고 본문 한글화', kind='예외', title='번역이 실패해도 수집은 끝나고 이미 있는 한글 본문을 덮지 않는다',
     pre='관리자 계정, description_summary_ko 가 채워진 공고 1건, 번역 호출이 실패하도록 막은 환경',
     steps='1. 그 공고가 다시 받아지는 수집 회차를 돌린다',
     expect='1. 그 회차의 수집 실행 이력 success 가 true 이다\n'
            '2. 그 공고 행의 description_summary_ko 가 수집 전 값 그대로이다',
     judge=['단위', '단위'],
     req=['BID-OPP-001'], fc=['BID-OPP-001'], screen='', pri='High'),

dict(id='IT-09-008', flow='수동 수집', kind='정상', title='관리자가 SAM.gov 수집 화면에서 조회 기간을 넣어 바로 수집한다',
     pre='관리자 계정, SAM.gov 호출 한도가 남아 있음',
     steps='1. 관리자 화면에 로그인해 메뉴 [SAM.gov 수집] 을 연다\n2. 수집 기간(daysBack)에 3 을 넣고 [수집 시작] 을 누른 뒤 확인 창의 [수집 시작] 을 누른다\n3. 수집이 끝날 때까지 기다린 뒤 화면을 새로고침한다',
     expect='1. 수집 상태 영역의 마지막 수집 시간이 2단계에서 실행한 시각으로 바뀐다\n'
            '2. 수집 상태 영역의 결과에 이번 회차의 조회 건수, 신규 건수, 갱신 건수가 숫자로 보인다\n'
            '3. 수집 실행 이력에 triggered_by MANUAL 인 행이 1건 더해지고 new_count, updated_count 가 화면의 신규, 갱신 건수와 같다\n'
            '4. 이번 회차에 새로 생긴 공고 행의 posted_date 가 모두 실행 시각 기준 최근 3일 안이다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-OPP-002'], fc=['BID-OPP-002'], screen='관리자 /collection', pri='Medium',
     issue='화면 소스의 SAM.gov 수집 화면에는 수동 수집 카드만 있고 T3-3 의 수집 상태, 수집 이력 영역이 보이지 않는다(소스 검색 기준). T1-1 의 선택 입력 키워드는 T3-2, T3-3 에 없다'),

dict(id='IT-09-009', flow='수동 수집', kind='예외', title='관리자가 아니면 수동 수집을 부를 수 없다',
     pre='회원 계정 로그인 토큰',
     steps='1. 회원 토큰으로 POST /admin/collection/trigger?daysBack=1 을 부른다\n2. 수집 실행 이력을 조회한다',
     expect='1. 요청이 403 으로 거부된다\n'
            '2. 수집 실행 이력에 행이 더해지지 않는다',
     judge=['서버', '서버'],
     req=['BID-OPP-002'], fc=['BID-OPP-002'], screen='', pri='Medium'),

dict(id='IT-09-010', flow='수집 통계', kind='정상', title='SAM.gov 수집 화면에서 마지막 수집 시각과 수집 이력을 본다',
     pre='관리자 계정, 수집 실행 이력이 성공 1건 이상과 실패 1건 이상 있는 서버',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 을 연다\n2. 수집 이력 목록을 본다\n3. 수집 상태 조회(GET /admin/collection/status)와 수집 이력 조회(GET /admin/collection/runs)를 부른다',
     expect='1. 수집 상태 영역의 마지막 수집 시간이 수집 실행 이력의 가장 최근 started_at 과 같다\n'
            '2. 수집 이력 목록 줄마다 그 회차의 시작 시각과 성공 여부가 보이고 실패 회차는 성공 회차와 구분된다\n'
            '3. 수집 이력 조회 응답의 회차 수와 성공, 실패 건수가 collector_runs 의 행 수와 success 값별 행 수와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-OPP-005'], fc=['BID-OPP-005'], screen='관리자 /collection', pri='Low',
     issue='T1-1 의 입력 기간 필터가 T3-2 수집 이력 조회와 T3-3 화면에 없다. 화면 소스에 수집 이력 영역이 보이지 않는다(소스 검색 기준)'),

dict(id='IT-09-011', flow='수집 통계', kind='예외', title='관리자가 아니면 수집 상태와 이력을 볼 수 없다',
     pre='회원 계정 로그인 토큰',
     steps='1. 회원 토큰으로 GET /admin/collection/status 를 부른다\n2. 회원 토큰으로 GET /admin/collection/runs 를 부른다',
     expect='1. 수집 상태 조회가 403 으로 거부되고 수집 결과가 응답에 없다\n'
            '2. 수집 이력 조회가 403 으로 거부되고 회차 목록이 응답에 없다',
     judge=['서버', '서버'],
     req=['BID-OPP-005'], fc=['BID-OPP-005'], screen='', pri='Low'),

dict(id='IT-09-012', flow='수집 완료 알림', kind='정상', title='신규 공고가 있는 회차가 끝나면 관리자에게 수집 완료 메일이 간다',
     pre='관리자 계정과 관리자 메일함, SAM.gov 호출 한도가 남아 있음, 이번 회차에 새 공고가 들어올 기간',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 끝낸다\n2. 그 회차 수집 실행 이력의 new_count 가 1 이상인지 본다\n3. 관리자 메일함을 연다',
     expect='1. 관리자 메일함에 수집 완료 메일이 1통 온다\n'
            '2. 메일 본문의 신규 공고 건수가 2단계 new_count 와 같은 숫자다\n'
            '3. 메일 본문에 이번 회차 신규 공고의 주요 제목 요약이 있다\n'
            '4. 알림 이력(notification_log)에 notification_type COLLECTION_COMPLETE, success true 인 행이 1건 더해진다',
     judge=['합동', '합동', '합동', '서버'],
     req=['BID-OPP-004'], fc=['BID-OPP-004'], screen='관리자 /collection', pri='High'),

dict(id='IT-09-013', flow='수집 완료 알림', kind='예외', title='신규 공고가 0건인 성공 회차에는 수집 완료 메일도 실패 메일도 가지 않는다',
     pre='관리자 계정과 관리자 메일함, SAM.gov 호출 한도가 남아 있음, 바로 앞 회차로 최근 공고를 다 모은 상태',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 수집 기간 1 로 [수집 시작] 을 눌러 한 회차를 끝낸다\n2. 그 회차 수집 실행 이력의 new_count 가 0 이고 success 가 true 인지 본다\n3. 관리자 메일함을 연다',
     expect='1. 이번 회차의 수집 완료 메일이 오지 않는다\n'
            '2. 알림 이력에 이번 회차 시각 뒤로 COLLECTION_COMPLETE 행이 더해지지 않는다\n'
            '3. 수집 실패 메일도 오지 않는다',
     judge=['합동', '서버', '합동'],
     req=['BID-OPP-004', 'BID-OPP-009'], fc=['BID-OPP-004', 'BID-OPP-009'], screen='관리자 /collection', pri='High'),

dict(id='IT-09-014', flow='수집 실패 알림', kind='정상', title='SAM.gov 인증이 실패하면 관리자에게 키 만료 추정 메일이 바로 간다',
     pre='관리자 계정과 관리자 메일함, SAM.gov API 키를 틀린 값으로 바꿔 둔 서버, 그날 UTC 에 실패 메일을 보낸 적 없음',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 돌린다\n2. 회차가 끝나면 그날 UTC 의 search 호출 기록(sam_api_call_log)과 수집 실행 이력을 조회한다\n3. 관리자 메일함을 연다',
     expect='1. 그날 UTC search 행의 last_status 가 401 또는 403 이다\n'
            '2. 그 회차 수집 실행 이력의 success 가 false 이고 error_message 가 남는다\n'
            '3. 관리자 메일함에 수집 실패 메일이 회차가 끝난 직후 1통 온다\n'
            '4. 메일 본문에 API 키가 만료된 것으로 추정한다는 문구와 키 재발급 절차가 있다',
     judge=['서버', '서버', '합동', '합동'],
     req=['BID-OPP-009', 'BID-OPP-001'], fc=['BID-OPP-009', 'BID-OPP-001'], screen='관리자 /collection', pri='High',
     issue='sam_api_call_log 가 T3-1 데이터 모델에 정의되어 있지 않다'),

dict(id='IT-09-015', flow='수집 실패 알림', kind='정상', title='수집 회차에 오류가 나면 관리자에게 실패 메일을 보낸다',
     pre='관리자 계정, SAM.gov 검색이 인증 외 오류로 실패하도록 막은 환경',
     steps='1. 수집 회차 하나를 돌린다',
     expect='1. 오류 수(errorCount)가 1 이상인 회차가 끝나면 관리자 실패 메일 발송이 1건 일어난다',
     judge=['단위'],
     req=['BID-OPP-009'], fc=['BID-OPP-009'], screen='', pri='High'),

dict(id='IT-09-016', flow='수집 실패 알림', kind='예외', title='같은 키워드 같은 날 두 번째 실패에는 실패 메일을 다시 보내지 않는다',
     pre='관리자 계정과 관리자 메일함, IT-09-014 로 그날 UTC 실패 메일 1통을 받은 상태, SAM.gov API 키가 여전히 틀린 값',
     steps='1. 같은 날 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 더 돌린다\n2. 관리자 메일함을 연다',
     expect='1. 두 번째 회차의 실패 메일이 오지 않아 그날 실패 메일이 1통뿐이다\n'
            '2. 같은 키워드 같은 날 두 번째 실패에서 메일 발송이 일어나지 않는다',
     judge=['합동', '단위'],
     req=['BID-OPP-009'], fc=['BID-OPP-009'], screen='관리자 /collection', pri='High',
     issue='T1-1 출력은 "하루 1통" 이고 제약조건은 "동일 키워드·동일 일자에 1통" 이라 키워드가 여럿일 때 하루 몇 통인지 어긋난다'),

dict(id='IT-09-017', flow='첨부 획득', kind='정상', title='수집한 공고의 첨부를 내려받아 저장한다',
     pre='관리자 계정, SAM.gov 호출 한도가 남아 있음, 첨부를 바로 내려받을 수 있는 새 공고가 들어올 기간',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 끝낸다\n2. 첨부가 있는 새 공고 하나를 메뉴 [원본 공고] 에서 열어 첨부파일 영역을 본다\n3. 공고 첨부문서 목록(GET /opportunities/{id}/attachments)을 부른다',
     expect='1. 그 공고 첨부(opportunity_attachments) 행의 download_status 가 SUCCESS 이고 storage_url 과 downloaded_at 이 채워져 있다\n'
            '2. 원본 공고 상세 첨부파일 영역에 SAM.gov 첨부와 같은 파일 이름의 줄이 보인다\n'
            '3. 공고 첨부문서 목록 응답에 그 파일이 파일 이름과 함께 들어 있다',
     judge=['서버', '가상', '서버'],
     req=['BID-OPP-003'], fc=['BID-OPP-003'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-09-018', flow='첨부 획득', kind='정상', title='링크 형태 첨부는 링크를 따라가 내려받는다',
     pre='관리자 계정, 첨부가 다른 주소로 이어지는 링크 형태인 공고 시험 자료',
     steps='1. 그 공고를 받는 수집 회차를 돌린다',
     expect='1. 링크를 따라간 끝의 파일이 저장되고 그 첨부 행의 download_status 가 SUCCESS 이다',
     judge=['단위'],
     req=['BID-OPP-003'], fc=['BID-OPP-003'], screen='', pri='High'),

dict(id='IT-09-019', flow='첨부 획득', kind='정상', title='내려받을 수 없는 첨부는 링크만 저장해 관리자와 고객에게 준다',
     pre='관리자 계정, SAM.gov 호출 한도가 남아 있음, 첨부가 외부 사이트로 이어져 바로 내려받을 수 없는 공고가 들어올 기간',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 끝낸다\n2. 그 공고를 메뉴 [원본 공고] 에서 열어 첨부파일 영역을 본다\n3. 관리자 원본 공고 목록(GET /admin/opportunities)과 공고 첨부문서 목록(GET /opportunities/{id}/attachments)을 부른다',
     expect='1. 그 첨부 행의 download_status 가 MANUAL_FETCH_REQUIRED 이고 source_url 에 SAM.gov 첨부 주소가 있고 storage_url 은 비어 있다\n'
            '2. 원본 공고 상세 첨부파일 영역의 그 첨부 줄에 "가져와야 함" 표식과 외부 원본 링크가 보인다\n'
            '3. 관리자 원본 공고 목록 응답에서 그 공고의 manualFetchRequiredCount 가 가져와야 할 첨부 수와 같다\n'
            '4. 공고 첨부문서 목록 응답에 그 첨부의 링크 주소가 들어 있다',
     judge=['서버', '가상', '서버', '서버'],
     req=['BID-OPP-003'], fc=['BID-OPP-003'], screen='관리자 /opportunities/{id}', pri='High',
     issue='T3-1 다운로드 상태에 LINK_ONLY 와 MANUAL_FETCH_REQUIRED 가 함께 있고 어느 때 어느 값을 쓰는지 T1-1 과 T3-2 가 말하지 않는다'),

dict(id='IT-09-020', flow='첨부 획득', kind='정상', title='PDF, DOCX, XLSX, ZIP 첨부를 내려받는다',
     pre='관리자 계정, PDF, DOCX, XLSX, ZIP 첨부를 하나씩 단 공고 시험 자료',
     steps='1. 그 공고를 받는 수집 회차를 돌린다',
     expect='1. 네 첨부가 모두 저장되고 첨부 행 넷의 download_status 가 SUCCESS 이다',
     judge=['단위'],
     req=['BID-OPP-003'], fc=['BID-OPP-003'], screen='', pri='High'),

dict(id='IT-09-021', flow='첨부 획득', kind='예외', title='50MB 를 넘는 첨부는 저장하지 않는다',
     pre='관리자 계정, 50MB 를 넘는 첨부를 단 공고 시험 자료',
     steps='1. 그 공고를 받는 수집 회차를 돌린다',
     expect='1. 그 첨부 파일이 저장소에 저장되지 않고 첨부 행의 storage_url 이 비어 있다',
     judge=['단위'],
     req=['BID-OPP-003'], fc=['BID-OPP-003'], screen='', pri='High'),

dict(id='IT-09-022', flow='첨부 수동 업로드', kind='정상', title='첨부가 없는 공고에 관리자가 파일을 올리면 첨부가 생기고 사전 분석이 시작된다',
     pre='관리자 계정, 첨부가 0건인 원본 공고 1건, 10MB 이하 PWS PDF 파일',
     steps='1. 관리자 화면 메뉴 [원본 공고] 에서 그 공고 상세를 연다\n2. 첨부파일 영역의 [수동 업로드] 를 눌러 PWS PDF 를 고른다\n3. 그 공고의 첨부 행과 사전 분석(opportunity_analysis) 행을 조회한다',
     expect='1. 첨부파일 영역에 올린 파일 이름의 줄이 1개 더해진다\n'
            '2. 그 공고에 첨부 행이 1건 생기고 download_status 가 SUCCESS, storage_url 과 downloaded_at 이 채워져 있다\n'
            '3. 그 공고의 사전 분석 행 상태가 PENDING 또는 ANALYZING 이다',
     judge=['가상', '서버', '서버'],
     req=['BID-OPP-007', 'BID-OPP-006'], fc=['BID-OPP-007', 'BID-OPP-006'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-09-023', flow='첨부 수동 업로드', kind='정상', title='가져와야 함 첨부가 있는 공고에 올리면 그 첨부 줄이 저장된 파일로 바뀐다',
     pre='관리자 계정, MANUAL_FETCH_REQUIRED 첨부가 1건 있는 원본 공고 1건, 그 첨부를 외부에서 받은 파일',
     steps='1. 관리자 화면 메뉴 [원본 공고] 에서 그 공고 상세를 연다\n2. 첨부 행 수를 적어 둔다\n3. [수동 업로드] 로 받은 파일을 올린다',
     expect='1. 그 공고의 첨부 행 수가 2단계와 같고 그 MANUAL_FETCH_REQUIRED 행이 SUCCESS 로 바뀌어 storage_url 이 채워진다\n'
            '2. 첨부파일 영역의 그 첨부 줄에서 "가져와야 함" 표식이 사라진다',
     judge=['서버', '가상'],
     req=['BID-OPP-007'], fc=['BID-OPP-007'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-09-024', flow='첨부 수동 업로드', kind='예외', title='50MB 를 넘는 파일은 첨부로 올라가지 않는다',
     pre='관리자 계정, 원본 공고 1건, 50MB 를 넘는 PDF 파일',
     steps='1. 관리자 화면 메뉴 [원본 공고] 에서 그 공고 상세를 연다\n2. [수동 업로드] 로 50MB 를 넘는 파일을 고른다',
     expect='1. 첨부파일 영역에 그 파일 이름의 줄이 더해지지 않는다\n'
            '2. 그 공고의 첨부 행이 늘지 않는다',
     judge=['가상', '서버'],
     req=['BID-OPP-007'], fc=['BID-OPP-007'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-09-025', flow='첨부 수동 업로드', kind='예외', title='관리자가 아니면 첨부를 올릴 수 없다',
     pre='회원 계정 로그인 토큰, 원본 공고 1건',
     steps='1. 회원 토큰으로 POST /admin/opportunities/{id}/attachments 에 PDF 파일을 실어 부른다',
     expect='1. 요청이 403 으로 거부된다\n'
            '2. 그 공고의 첨부 행이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-OPP-007'], fc=['BID-OPP-007'], screen='', pri='High'),

dict(id='IT-09-026', flow='사전 분석', kind='정상', title='첨부 내려받기가 끝나면 사전 분석이 자동으로 돌아 결과 넷을 남긴다',
     pre='관리자 계정, SAM.gov 호출 한도가 남아 있음, 첨부를 바로 내려받을 수 있는 새 공고가 들어올 기간, Aimbase 공고 분석 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [SAM.gov 수집] 에서 [수집 시작] 으로 한 회차를 끝낸다\n2. 첨부가 SUCCESS 로 받아진 새 공고 하나를 고른다\n3. 사전 분석 결과 상세(GET /admin/opportunities/{id}/analysis)를 분석이 끝날 때까지 몇 분 간격으로 다시 부른다',
     expect='1. 그 공고의 사전 분석 행이 1건 생기고 상태가 PENDING, ANALYZING 을 거쳐 COMPLETED 가 된다\n'
            '2. 사전 분석 행의 summary_json, document_formats_json, required_documents_json, llm_prompt_preset_json 이 모두 채워지고 analyzed_at 이 남는다\n'
            '3. 요약본이 공고 내용과 맞고 필요 서류 목록이 공고가 요구하는 서류를 담는다',
     judge=['서버', '서버', '합동'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-027', flow='사전 분석', kind='정상', title='관리자가 분석이 끝난 공고를 다시 분석한다',
     pre='관리자 계정, 사전 분석이 COMPLETED 인 공고 1건, Aimbase 공고 분석 워크플로우가 도는 상태',
     steps='1. 사전 분석 행의 analyzed_at 을 적어 둔다\n2. 관리자 토큰으로 POST /admin/opportunities/{id}/analyze 를 부른다\n3. 분석이 끝날 때까지 사전 분석 결과 상세를 다시 부른다',
     expect='1. 사전 분석 상태가 PENDING 으로 돌아갔다가 다시 COMPLETED 가 된다\n'
            '2. 그 공고의 사전 분석 행이 여전히 1건이고 analyzed_at 이 1단계보다 늦은 시각으로 바뀐다',
     judge=['서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-028', flow='사전 분석', kind='예외', title='이미 분석이 끝난 공고는 첨부 내려받기 완료가 다시 와도 자동 분석을 다시 하지 않는다',
     pre='관리자 계정, 사전 분석이 COMPLETED 인 공고 1건',
     steps='1. 그 공고에 첨부 내려받기 완료 이벤트를 한 번 더 보낸다',
     expect='1. 분석 워크플로우가 다시 시작되지 않고 사전 분석 상태가 COMPLETED 그대로이다',
     judge=['단위'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-029', flow='사전 분석', kind='예외', title='사전 분석이 실패하면 FAILED 와 오류가 남고 관리자에게 알린다',
     pre='관리자 계정, 분석 워크플로우가 실패하도록 막은 환경, 사전 분석이 PENDING 인 공고 1건',
     steps='1. 그 공고의 사전 분석을 시작한다',
     expect='1. 사전 분석 상태가 ANALYZING 에서 FAILED 로 바뀌고 error_message 가 남는다\n'
            '2. 관리자 실패 알림 발송이 1건 일어난다',
     judge=['단위', '단위'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-030', flow='사전 분석', kind='정상', title='실패한 사전 분석을 관리자가 다시 시도한다',
     pre='관리자 계정, 사전 분석이 FAILED 인 공고 1건, Aimbase 공고 분석 워크플로우가 도는 상태',
     steps='1. 관리자 토큰으로 POST /admin/opportunities/{id}/analyze 를 부른다\n2. 분석이 끝날 때까지 사전 분석 결과 상세를 다시 부른다',
     expect='1. 사전 분석 상태가 FAILED 에서 PENDING 으로 바뀐 뒤 COMPLETED 가 된다\n'
            '2. 그 공고의 사전 분석 행이 여전히 1건이다',
     judge=['서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-031', flow='사전 분석', kind='정상', title='관리자가 사전 분석 결과를 보정한다',
     pre='관리자 계정, 사전 분석이 COMPLETED 인 공고 1건',
     steps='1. 관리자 토큰으로 PATCH /admin/opportunities/{id}/analysis 에 요약 문구 하나를 바꿔 보낸다\n2. 사전 분석 결과 상세(GET /admin/opportunities/{id}/analysis)를 부른다',
     expect='1. 사전 분석 결과 상세 응답의 요약에 1단계에서 바꾼 문구가 들어 있다\n'
            '2. 사전 분석 상태가 COMPLETED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-OPP-006', 'BID-OPP-008'], fc=['BID-OPP-006', 'BID-OPP-008'], screen='', pri='High',
     issue='T3-3 에 사전 분석 결과(요약, 양식, 필요 서류)를 보고 보정하는 관리자 화면이 없어 API 로만 절차를 쓸 수 있다'),

dict(id='IT-09-032', flow='컴플라이언스 매트릭스', kind='정상', title='공고 분석 저장에 매트릭스가 오면 요구 항목에 매트릭스 줄이 REQ ID 와 함께 생긴다',
     pre='관리자 계정과 MCP 열쇠, 공고문이 있고 매트릭스 줄이 없는 공고 1건, shall, should, may 원문 줄 하나씩(그중 1줄은 질의 후보)과 평가 항목 구조(평가 항목마다 문서 종류, 맨 위에 커버레터 요구 여부)를 담은 매트릭스 시험 자료',
     steps='1. MCP save_opportunity_analysis 를 그 공고 id 와 시험 자료의 complianceMatrix, factorTree 로 부른다\n2. 그 공고의 요구 항목(opportunity_requirement_items), 공고문, 원본 공고를 조회한다',
     expect='1. 요구 항목에 req_id 가 REQ-001, REQ-002, REQ-003 인 매트릭스 줄 3개가 보낸 순서대로 생긴다\n'
            '2. 매트릭스 줄의 req_level 이 보낸 구분대로 SHALL, SHOULD, MAY 이고 is_blocker 가 모두 false 이다\n'
            '3. 매트릭스 줄의 category 가 TECHNICAL, FORMAT, SUBMISSION, DEADLINE, ELIGIBILITY, OTHER 가운데 하나이고 DOCUMENT 가 아니다\n'
            '4. 공고문의 compliance_matrix_json 에 보낸 매트릭스가, factor_tree_json 에 보낸 평가 항목 구조가 그대로 들어 있다\n'
            '5. 원본 공고의 last_req_no 가 3 이다\n'
            '6. 공고문 factor_tree_json 의 평가 항목마다 보낸 문서 종류(documentType)가, 맨 위에 보낸 커버레터 요구 여부(coverLetterRequired)가 남아 있다',
     judge=['서버', '서버', '서버', '서버', '서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-033', flow='컴플라이언스 매트릭스', kind='정상', title='공고문 관리 상세에서 매트릭스와 질의 후보를 훑어본다',
     pre='관리자 계정, IT-09-032 로 매트릭스 줄 3개(질의 후보 1줄)가 들어간 공고문 1건',
     steps='1. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문을 연다\n2. 매트릭스 첫 줄을 누른다\n3. 질의 후보 표의 REQ ID 를 누른다',
     expect='1. 한글화 결과 아래 "컴플라이언스 매트릭스" 머리에 줄 수 3, 구분별 수 Shall 1, Should 1, May 1, 질의 후보 1 이 보인다\n'
            '2. 매트릭스 표 칸이 REQ ID, 요구사항, 번역, 출처, 구분, 요구 동사, 평가항목, 충족 주체 순서이고 줄마다 보낸 값이 들어 있다\n'
            '3. 질의 후보인 줄에만 질의 표시가 달려 있다\n'
            '4. 표 아래 질의 후보 표에 REQ ID, 모호한 점, 질의(안) 이 보낸 값으로 보인다\n'
            '5. 첫 줄을 누르면 그 줄이 펼쳐져 출처 파일, 쪽, 조항, 원문 인용이 보인다\n'
            '6. 질의 후보 표의 REQ ID 를 누르면 매트릭스의 같은 REQ ID 줄로 이동해 그 줄이 펼쳐진다\n'
            '7. 매트릭스 영역에 줄을 고치거나 지우는 단추가 없다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High'),

dict(id='IT-09-034', flow='컴플라이언스 매트릭스', kind='정상', title='매트릭스 줄이 없는 공고문은 매트릭스 없음 안내를 보인다',
     pre='관리자 계정, 한글화가 COMPLETED 이고 매트릭스 줄이 없는 공고문 1건',
     steps='1. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문을 연다\n2. 관리자 토큰으로 GET /admin/notices/{id}/compliance-matrix 를 부른다',
     expect='1. 매트릭스 영역에 "컴플라이언스 매트릭스가 없습니다. 공고 분석이 매트릭스를 낸 뒤에 보입니다" 한 줄만 보인다\n'
            '2. 응답의 lines 와 questions 가 빈 배열이다',
     judge=['가상', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High'),

dict(id='IT-09-035', flow='컴플라이언스 매트릭스', kind='정상', title='공고문이 분석 중이면 매트릭스 영역도 분석 중으로 보인다',
     pre='관리자 계정, 한글화 상태가 ANALYZING 인 공고문 1건',
     steps='1. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문을 연다',
     expect='1. 매트릭스 영역에 표 대신 공고를 분석하고 있다는 안내가 한글화 결과의 분석 중 표시와 같이 보인다',
     judge=['가상'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High'),

dict(id='IT-09-036', flow='컴플라이언스 매트릭스', kind='예외', title='매트릭스를 불러오지 못하면 매트릭스 영역에만 다시 시도가 보인다',
     pre='관리자 계정, 한글화가 COMPLETED 인 공고문 1건, 매트릭스 조회 요청이 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문을 연다\n2. 막음을 풀고 [다시 시도] 를 누른다',
     expect='1. 매트릭스 영역에 "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 헤더와 한글화 결과는 그 공고문의 값으로 그대로 보인다\n'
            '3. [다시 시도] 를 누르면 매트릭스 영역이 그 공고문의 매트릭스 머리와 표로 바뀐다',
     judge=['가상', '가상', '가상'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High'),

dict(id='IT-09-037', flow='컴플라이언스 매트릭스', kind='예외', title='없는 공고문의 매트릭스 조회는 404 다',
     pre='관리자 계정 로그인 토큰',
     steps='1. 관리자 토큰으로 GET /admin/notices/{없는 id}/compliance-matrix 를 부른다',
     expect='1. 응답이 404 이고 매트릭스 줄이 없다',
     judge=['서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-038', flow='컴플라이언스 매트릭스', kind='정상', title='다시 분석해도 같은 요구의 REQ ID 는 그대로이고 새 요구는 다음 번호를 받는다',
     pre='관리자 계정과 MCP 열쇠, IT-09-032 로 REQ-001 부터 REQ-003 이 생긴 공고, REQ-001 에 충족 매핑이 1줄 있는 입찰 건',
     steps='1. REQ-001 의 충족 매핑 줄을 적어 둔다\n2. MCP save_opportunity_analysis 를 같은 세 줄(출처 조항과 원문이 같음)에 새 원문 줄 하나를 더해 다시 부른다\n3. 그 공고의 요구 항목과 충족 매핑을 조회한다',
     expect='1. 처음 세 줄의 req_id 가 REQ-001, REQ-002, REQ-003 그대로이다\n'
            '2. 새로 더한 줄의 req_id 가 REQ-004 이고 원본 공고의 last_req_no 가 4 이다\n'
            '3. REQ-001 의 충족 매핑 줄이 1단계와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-039', flow='컴플라이언스 매트릭스', kind='예외', title='사라진 요구의 줄과 충족 매핑은 지워지고 그 번호는 다시 쓰지 않는다',
     pre='관리자 계정과 MCP 열쇠, IT-09-038 로 REQ-001 부터 REQ-004 가 있고 REQ-002 에 충족 매핑이 있는 공고',
     steps='1. MCP save_opportunity_analysis 를 REQ-002 의 원문 줄을 뺀 매트릭스로 부른다\n2. 다시 MCP save_opportunity_analysis 를 1단계 매트릭스에 새 원문 줄 하나를 더해 부른다\n3. 그 공고의 요구 항목과 충족 매핑을 조회한다',
     expect='1. 1단계 뒤 REQ-002 매트릭스 줄과 그 충족 매핑 줄이 지워져 있다\n'
            '2. 2단계에서 더한 줄의 req_id 가 REQ-005 이고 REQ-002 가 다시 쓰이지 않는다\n'
            '3. REQ-001, REQ-003, REQ-004 의 req_id 가 그대로이다',
     judge=['서버', '서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

dict(id='IT-09-040', flow='컴플라이언스 매트릭스', kind='정상', title='공고 분석이 요구 하나를 한 줄로, 원문 그대로, 출처와 함께 뽑는다',
     pre='관리자 계정, Aimbase 공고 분석 워크플로우에 매트릭스 뽑기가 붙은 뒤, Section L, M, C 와 수정 공고가 있는 공고 1건',
     steps='1. 관리자 토큰으로 POST /admin/opportunities/{id}/analyze 를 불러 분석을 끝낸다\n2. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문의 컴플라이언스 매트릭스를 공고 원문과 견준다',
     expect='1. 한 문장에 요구가 둘인 원문은 매트릭스에서 두 줄로 나뉘어 있다\n'
            '2. 요구사항 칸의 원문이 공고 원문과 글자 그대로 같고 번역은 따로 있다\n'
            '3. 구분이 shall, must, will 이면 Shall, should 면 Should, may 와 가점이면 May 로 매겨져 있다\n'
            '4. 줄마다 출처(파일, 쪽, 조항)와 원문 인용이 있고 그 인용이 원문에서 찾아진다\n'
            '5. 요구 동사 칸에 그 원문의 요구 동사가 있다\n'
            '6. Section L, M, C 와 수정 공고에 흩어진 요구가 빠짐없이 들어 있다',
     judge=['합동', '합동', '합동', '합동', '합동', '합동'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='관리자 /notices/{id}', pri='High',
     issue='BIZ-073 의 "인용을 원문에서 찾지 못한 줄은 버린다" 를 워크플로우가 하는지 서버가 하는지 설계서가 적지 않는다'),

dict(id='IT-09-041', flow='컴플라이언스 매트릭스', kind='예외', title='매트릭스 줄은 고객 서류 점검과 MCP 요구사항 조회에 섞이지 않고 서류 접수 전환을 막지 않는다',
     pre='회원 계정과 관리자 MCP 열쇠, 제출 서류 줄과 매트릭스 줄이 함께 있는 공고, 그 공고의 서류 대기(DOCS_PENDING) 입찰 건(필수 고객 서류를 모두 올림)',
     steps='1. MCP get_opportunity_requirements 를 그 공고 id 로 부른다\n2. 고객 화면에 회원으로 로그인해 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건을 연다\n3. GET /bid-requests/{id}/pre-check 를 부른다\n4. [서류 제출 완료] 를 눌러 확인한다',
     expect='1. MCP 응답에 req_id 가 있는 줄이 없다\n'
            '2. 입찰 건별 제출 전 검사의 올릴 서류와 플랫폼이 만드는 서류 목록에 매트릭스 줄의 요구사항이 없다\n'
            '3. 제출 전 검사 응답의 요구 항목이 req_id 가 없는 줄과 같은 수이다\n'
            '4. 입찰 건 상태가 DOCS_RECEIVED 로 바뀐다',
     judge=['서버', '가상', '서버', '서버'],
     req=['BID-OPP-006', 'BID-COMP-003'], fc=['BID-OPP-006', 'BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='High'),

dict(id='IT-09-042', flow='노출 승인', kind='정상', title='사전 분석이 끝난 공고를 관리자가 승인하면 노출된다',
     pre='관리자 계정과 회원 계정, 사전 분석 COMPLETED 이고 visibility HIDDEN 인 공고 1건',
     steps='1. 관리자 토큰으로 POST /admin/opportunities/{id}/approve 를 부른다\n2. 회원 토큰으로 GET /opportunities 를 그 공고 제목으로 거른다',
     expect='1. 그 공고의 visibility 가 VISIBLE 이다\n'
            '2. 회원의 공고 목록 응답에 그 공고가 들어 있다',
     judge=['서버', '서버'],
     req=['BID-OPP-008'], fc=['BID-OPP-008'], screen='', pri='High',
     issue='T3-3 에 공고 노출 승인 화면이 없고 공고문 관리 상세의 노출 승인은 공고문(CR-016) 노출이다. CR-155 뒤 고객 마감 전 공고 화면은 모은 공고 전부를 읽어 visibility 가 고객 화면 노출을 가르지 않는다. T1-1 입력의 반려는 T3-2 에 없다'),

dict(id='IT-09-043', flow='노출 승인', kind='정상', title='노출 중인 공고를 관리자가 비노출로 되돌린다',
     pre='관리자 계정과 회원 계정, visibility VISIBLE 인 공고 1건',
     steps='1. 관리자 토큰으로 POST /admin/opportunities/{id}/hide 를 부른다\n2. 회원 토큰으로 GET /opportunities 를 그 공고 제목으로 거른다',
     expect='1. 그 공고의 visibility 가 HIDDEN 이다\n'
            '2. 회원의 공고 목록 응답에 그 공고가 없다',
     judge=['서버', '서버'],
     req=['BID-OPP-008'], fc=['BID-OPP-008'], screen='', pri='High'),

dict(id='IT-09-044', flow='노출 승인', kind='예외', title='사전 분석이 COMPLETED 가 아니면 승인되지 않는다',
     pre='관리자 계정, 사전 분석이 ANALYZING 이고 visibility HIDDEN 인 공고 1건',
     steps='1. 관리자 토큰으로 POST /admin/opportunities/{id}/approve 를 부른다',
     expect='1. 승인 요청이 거부된다(2xx 가 아니다)\n'
            '2. 그 공고의 visibility 가 HIDDEN 그대로이다',
     judge=['서버', '서버'],
     req=['BID-OPP-008'], fc=['BID-OPP-008'], screen='', pri='High'),

dict(id='IT-09-045', flow='노출 승인', kind='예외', title='관리자가 아니면 공고를 승인할 수 없다',
     pre='회원 계정 로그인 토큰, 사전 분석 COMPLETED 이고 visibility HIDDEN 인 공고 1건',
     steps='1. 회원 토큰으로 POST /admin/opportunities/{id}/approve 를 부른다',
     expect='1. 요청이 403 으로 거부된다\n'
            '2. 그 공고의 visibility 가 HIDDEN 그대로이다',
     judge=['서버', '서버'],
     req=['BID-OPP-008'], fc=['BID-OPP-008'], screen='', pri='High'),

dict(id='IT-09-046', flow='컴플라이언스 매트릭스', kind='정상', title='공고 분석이 평가 항목마다 담을 문서 종류와 커버레터 요구 여부를 남긴다',
     pre='관리자 계정, Section L 이 기술, 과거 실적, 가격을 문서(볼륨)로 나눠 내라고 하고 커버레터를 요구하지 않는 공고 1건(예: W90VN926QA049), Aimbase 공고 분석 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [공고문 관리] 에서 그 공고문을 열고 다시 분석한다\n2. 분석이 끝나면 관리자 토큰으로 GET /admin/notices/{id}/compliance-matrix 를 부른다',
     expect='1. factorTree 의 평가 항목마다 documentType 이 TECHNICAL_PROPOSAL, PAST_PERFORMANCE, PRICING_SUMMARY 가운데 하나다\n'
            '2. 기술 평가 항목의 documentType 이 TECHNICAL_PROPOSAL, 과거 실적 평가 항목은 PAST_PERFORMANCE, 가격 평가 항목은 PRICING_SUMMARY 다\n'
            '3. factorTree 의 coverLetterRequired 가 false 다',
     judge=['서버', '서버', '서버'],
     req=['BID-OPP-006'], fc=['BID-OPP-006'], screen='', pri='High'),

]
