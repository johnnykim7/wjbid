# 영역 02 공고 열람 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-BROWSE-001~006), T1-3 BIZ-069·071, T3-2 C·C-2·D, T3-3 공통 틀·홈·공고 검색·공고 상세·회원 전용 화면, T3-4 원본 공고 상세·마감 전 공고 CR-147·CR-155·CR-157 이다.

ITEMS = [

dict(id='IT-02-001', flow='마감 전 공고 목록', kind='정상', title='메뉴로 마감 전 공고에 들어가 틀과 숫자 칸을 본다',
     pre='회원, 마감 전 모은 공고가 여러 건 있는 환경',
     steps='1. 메뉴 띠에서 입찰공고를 누르고 전체메뉴의 입찰공고 > 마감 전 공고를 누른다\n'
           '2. GET /opportunities/collected?openOnly=true&latestOnly=true 를 불러 totalElements 를 적는다',
     expect='1. 마감 전 공고(/search)가 속페이지 틀 안에 열리고 왼쪽 입찰공고 묶음 메뉴에서 마감 전 공고가 켜져 있다\n'
            '2. 위치 줄이 홈, 입찰공고, 마감 전 공고 순으로 보인다\n'
            '3. 제목 "마감 전 공고" 와 설명 "마감되지 않은 미군 공고를 한글로 봅니다" 가 보인다\n'
            '4. 숫자 칸 이름이 "마감 전 공고" 이고 "SAM.gov 활성 공고" 라는 이름은 없다\n'
            '5. 숫자 칸의 건수가 2번에서 적은 totalElements 와 같다\n'
            '6. 목록 줄이 게시일 최신순으로 정렬돼 있다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-002', flow='마감 전 공고 목록', kind='정상', title='마감 전 공고는 모은 공고 중 마감 전, 낙찰 공지 아님, 지우지 않음, 마지막 판만 보인다',
     pre='회원, 모은 공고에 마감 지난 공고 P1, 마감 전 낙찰 공지 P2, 지운 공고 P3, 같은 공고번호의 두 판 P4(이른 판)·P5(늦은 판), 관리자가 공고문을 공개하지 않은 마감 전 공고 P6 를 넣은 환경',
     steps='1. 마감 전 공고(/search)를 연다\n'
           '2. 검색어로 P1~P6 의 공고번호를 하나씩 찾는다',
     expect='1. 마감 지난 P1 이 목록에 없다\n'
            '2. 낙찰 공지 P2 가 목록에 없다\n'
            '3. 지운 공고 P3 가 목록에 없다\n'
            '4. 같은 공고번호는 게시가 늦은 P5 한 줄만 있고 P4 줄은 없다\n'
            '5. 관리자가 공고문을 공개하지 않은 P6 도 목록에 있다\n'
            '6. 숫자 칸 건수가 P1, P2, P3, P4 를 뺀 수와 같다',
     judge=['서버', '서버', '서버', '서버', '서버', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-003', flow='마감 전 공고 목록', kind='정상', title='마감 전 공고 줄의 칸과 한글 제목 대신 원문 제목을 본다',
     pre='회원, 한글 제목이 있는 마감 전 공고 A 와 한글 제목이 없는 마감 전 공고 B',
     steps='1. 마감 전 공고(/search)를 연다\n'
           '2. A 와 B 의 줄을 본다',
     expect='1. 줄마다 공고번호와 업종 코드, 제목과 발주처, 한글 유형, 게시일, 마감과 남은 날, 우리 회사 판정, 관심 공고 칸이 이 순서로 있다\n'
            '2. A 줄의 제목이 A 의 한글 제목이다\n'
            '3. B 줄의 제목이 B 의 원문 제목이다',
     judge=['가상', '서버', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-004', flow='마감 전 공고 목록', kind='정상', title='검색어로 한글 제목, 원문 제목, 공고번호를 찾는다',
     pre='회원, 한글 제목에 "경비" 가 든 공고 C, 원문 제목에 "Paving" 이 든 공고 D, 공고번호가 알려진 공고 E 가 마감 전인 환경',
     steps='1. 마감 전 공고(/search)에서 숫자 칸 건수를 적는다\n'
           '2. 검색어 "경비" 로 찾는다\n'
           '3. 검색어 "paving" 으로 찾는다\n'
           '4. E 의 공고번호로 찾는다',
     expect='1. 2번 목록이 한글 제목, 원문 제목, 공고번호 중 하나에 "경비" 가 든 줄만이고 C 가 있다\n'
            '2. 3번 목록에 원문 제목에 "Paving" 이 든 D 가 있다(대소문자 무관)\n'
            '3. 4번 목록에 E 가 있다\n'
            '4. 2~4번 내내 숫자 칸 건수가 1번에서 적은 수 그대로다\n'
            '5. 검색 요청(GET /opportunities/collected)에 q 값이 넣은 검색어로 간다',
     judge=['서버', '서버', '서버', '가상', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-005', flow='마감 전 공고 목록', kind='정상', title='마감 전 공고 목록을 쪽으로 넘긴다',
     pre='회원, 마감 전 공고가 한 쪽 줄 수보다 많은 환경',
     steps='1. 마감 전 공고(/search)를 연다\n'
           '2. 쪽 이동에서 2쪽을 누른다',
     expect='1. 쪽 이동에 지금 쪽 번호 1 과 전체 쪽 수가 보인다\n'
            '2. 2쪽을 누르면 1쪽과 겹치지 않는 다음 줄들이 게시일 최신순으로 보인다\n'
            '3. 2쪽 요청(GET /opportunities/collected)의 page 값이 2쪽을 가리킨다',
     judge=['가상', '서버', '서버'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-006', flow='마감 전 공고 목록', kind='정상', title='공개 공고문이 있는 줄과 없는 줄을 눌러 각 상세로 간다',
     pre='회원, 공개 공고문이 있는 마감 전 공고 F(공고문 id N)와 공개 공고문이 없는 마감 전 공고 G',
     steps='1. 마감 전 공고(/search)에서 F 줄을 누른다\n'
           '2. 뒤로 가서 G 줄을 누른다',
     expect='1. 1번 뒤 공고 상세(/search/{N})가 열리고 F 의 공고문 제목이 보인다\n'
            '2. 2번 뒤 원본 공고 상세(/search/origin/{G 의 opportunityId})가 열리고 G 의 공고번호가 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-001', 'BID-BROWSE-002'], fc=['BID-BROWSE-001', 'BID-BROWSE-002'], screen='/search', pri='High'),

dict(id='IT-02-007', flow='마감 전 공고 목록', kind='정상', title='우리 회사 칸은 회원과 무료회원에게 다르게 보인다',
     pre='회원 계정과 무료회원 계정, 공개 공고문이 있는 마감 전 공고 F 와 없는 공고 G',
     steps='1. 회원으로 마감 전 공고(/search)를 연다\n'
           '2. 무료회원으로 마감 전 공고(/search)를 연다',
     expect='1. 회원 화면 F 줄의 우리 회사 칸에 그 공고문의 판정 꼬리표가 보인다\n'
            '2. 회원 화면 G 줄의 우리 회사 칸은 "-" 다\n'
            '3. 무료회원 화면 F 줄의 우리 회사 칸에 "회원" 꼬리표가 보인다\n'
            '4. 무료회원 화면 G 줄의 우리 회사 칸은 "-" 다\n'
            '5. 무료회원으로 열 때 GET /eligibility/notices 를 부르지 않는다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-001', 'BID-AUTH-008'], fc=['BID-BROWSE-001', 'BID-AUTH-008'], screen='/search', pri='High'),

dict(id='IT-02-008', flow='마감 전 공고 목록', kind='정상', title='마감 전 공고가 0건이면 빈 목록 문구와 0 을 보인다',
     pre='회원, 마감 전 모은 공고가 0건인 환경',
     steps='1. 마감 전 공고(/search)를 검색어 없이 연다',
     expect='1. 목록 자리에 "지금 마감 전 공고가 없습니다" 가 보인다\n'
            '2. 그 아래 "새 공고가 올라오면 여기에 보입니다" 가 보인다\n'
            '3. 숫자 칸 "마감 전 공고" 의 값이 "-" 가 아니라 0 이다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-009', flow='마감 전 공고 목록', kind='정상', title='검색 결과가 0건이면 검색 결과 없음 문구를 보인다',
     pre='회원, 마감 전 공고가 1건 이상인 환경',
     steps='1. 마감 전 공고(/search)에서 어느 공고에도 없는 낱말 "zzqqxx" 로 찾는다',
     expect='1. 목록 자리에 "검색 결과가 없습니다" 가 보인다\n'
            '2. 그 아래 "마감 전 공고에서 찾지 못했습니다. 다른 낱말로 찾아 보세요" 가 보인다\n'
            '3. 숫자 칸 건수는 마감 전 공고 전체 건수 그대로다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-010', flow='마감 전 공고 목록', kind='정상', title='홈과 머리말 검색에서 마감 전 공고로 들어온다',
     pre='회원, 마감 전 공고가 1건 이상이고 한글 제목에 "경비" 가 든 공고가 있는 환경',
     steps='1. 홈(/) 마감 전 공고 목록 아래 "마감 전 공고 {n}건 모두 보기" 를 누른다\n'
           '2. 머리말 검색 칸에 "경비" 를 넣고 검색 단추를 누른다\n'
           '3. 머리말 추천 검색어 하나를 누른다',
     expect='1. 1번 뒤 마감 전 공고(/search)가 열리고 숫자 칸 건수가 홈에서 본 n 과 같다\n'
            '2. 2번 뒤 마감 전 공고(/search)가 검색어 "경비" 로 걸러진 목록으로 열린다\n'
            '3. 2번 화면의 숫자 칸 건수가 검색어 없는 마감 전 공고 전체 건수와 같다\n'
            '4. 2번에서 숫자 칸 건수를 GET /opportunities/collected?openOnly=true&latestOnly=true&size=1 로 따로 부른다\n'
            '5. 3번 뒤 마감 전 공고(/search)가 누른 추천 검색어로 걸러진 목록으로 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-001', 'BID-BROWSE-006'], fc=['BID-BROWSE-001', 'BID-BROWSE-006'], screen='/search', pri='High'),

dict(id='IT-02-011', flow='마감 전 공고 목록', kind='정상', title='모은 공고 목록과 원본 공고 상세 API 는 로그인 없이 열린다',
     pre='비로그인(토큰 없음), 마감 전 모은 공고 G(opportunityId M)',
     steps='1. GET /opportunities/collected?openOnly=true&latestOnly=true 를 부른다\n'
           '2. GET /opportunities/origin/{M} 을 부른다\n'
           '3. GET /opportunities/{공고문 id}/attachments 를 부른다',
     expect='1. 1번 응답이 200 과 마감 전 공고 목록이다\n'
            '2. 2번 응답이 200 과 G 의 원문 제목, 공고번호, samUrl 이다\n'
            '3. 3번 응답이 200 과 첨부 목록이다',
     judge=['서버', '서버', '서버'],
     req=['BID-BROWSE-001', 'BID-BROWSE-002'], fc=['BID-BROWSE-001', 'BID-BROWSE-002'], screen='', pri='High'),

dict(id='IT-02-012', flow='마감 전 공고 목록', kind='예외', title='마감 전 공고를 못 불러오면 실패 문구를 보인다',
     pre='회원, GET /opportunities/collected 가 500 으로 실패하게 한 환경',
     steps='1. 마감 전 공고(/search)를 연다',
     expect='1. 목록 자리에 "마감 전 공고를 불러오지 못했습니다. 잠시 후 다시 시도해주세요" 가 보인다\n'
            '2. "공고문을 불러오지 못했습니다" 라는 옛 문구는 없다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-013', flow='마감 전 공고 목록', kind='예외', title='검색어를 달고 들어와 전체 건수를 못 받으면 숫자 칸을 - 로 둔다',
     pre='회원, size=1 전체 건수 조회만 실패하게 한 환경',
     steps='1. 머리말 검색 칸에 "경비" 를 넣고 검색 단추를 누른다',
     expect='1. 숫자 칸 "마감 전 공고" 의 값이 "-" 다\n'
            '2. 목록은 "경비" 로 걸러진 줄들로 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-014', flow='마감 전 공고 목록', kind='예외', title='판정 자료를 못 불러오면 우리 회사 칸만 비운다',
     pre='회원, GET /eligibility/notices 가 실패하게 한 환경, 공개 공고문이 있는 마감 전 공고 F',
     steps='1. 마감 전 공고(/search)를 연다',
     expect='1. F 줄을 포함한 모든 줄의 우리 회사 칸이 비어 있다\n'
            '2. 목록 줄 수와 숫자 칸 건수는 판정 자료가 있을 때와 같다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High'),

dict(id='IT-02-015', flow='마감 전 공고 목록', kind='예외', title='로그인하지 않으면 마감 전 공고를 열지 않는다',
     pre='비로그인',
     steps='1. 마감 전 공고(/search)를 주소로 연다',
     expect='1. 로그인 화면(/login)이 열린다\n'
            '2. 로그인하면 마감 전 공고(/search)로 돌아온다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-001'], fc=['BID-BROWSE-001'], screen='/search', pri='High',
     issue='T3-4 CR-155 진입 조건은 마감 전 공고에 로그인이 필요하다고 하나 T3-3 페이지 목록의 /search 에는 잠금이 없고 collected API 는 공개다'),

dict(id='IT-02-016', flow='공고문 상세', kind='정상', title='회원이 공고문 상세의 머리, AI 분석, 첨부, 원본을 본다',
     pre='회원, 분석이 COMPLETED 인 공개 공고문 N(첨부 2개)',
     steps='1. 공고 상세(/search/{N})를 연다\n'
           '2. 머리의 "SAM.gov 원문 보기" 를 누른다\n'
           '3. SAM.gov 원본 접이식 칸을 펼친다',
     expect='1. 공고 헤더에 제목, 기관명, 게시일, 마감일, D-day 가 보인다\n'
            '2. "AI 분석 완료" 배지와 분석 일시가 보인다\n'
            '3. 공고 요약(AI)에 핵심 요약, 작업 범위, 참여 자격, 평가 기준, 예산 정보, 특이사항 칸이 있다\n'
            '4. 주요 일정 표의 열이 항목, 일자, 비고 다\n'
            '5. 문서 양식 안내(AI)에 전반 안내, 섹션별 양식 카드, 제출 방법이 있다\n'
            '6. 첨부문서에 파일명 2개와 다운로드 링크가 있다\n'
            '7. 2번 뒤 새 창에 응답 samUrl 의 SAM.gov 원문 공개 주소가 열린다\n'
            '8. 3번 뒤 원본 description 과 원문 링크가 펼쳐진다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-017', flow='공고문 상세', kind='정상', title='분석이 끝나지 않은 공고문은 원본만 보인다',
     pre='회원, 분석이 COMPLETED 가 아닌 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 연다',
     expect='1. "AI 분석 완료" 배지가 없다\n'
            '2. 공고 요약, 주요 일정, 필요 서류 체크리스트, 문서 양식 안내 칸이 없다\n'
            '3. 원본 description 과 resourceLinks 가 그대로 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-018', flow='공고문 상세', kind='정상', title='관리자 계정도 공고문 상세를 본다',
     pre='회원 기간이 없는 관리자 계정, 공개 공고문 N',
     steps='1. 고객 화면에서 공고 상세(/search/{N})를 연다\n'
           '2. GET /opportunities/{N} 을 부른다',
     expect='1. 공고 요약과 첨부문서 칸이 보이고 "공고문은 회원 전용입니다" 회원 안내는 없다\n'
            '2. 응답이 200 이고 opportunityId 와 samUrl 이 들어 있다',
     judge=['가상', '서버'],
     req=['BID-BROWSE-002', 'BID-AUTH-008'], fc=['BID-BROWSE-002', 'BID-AUTH-008'], screen='/search/{id}', pri='High'),

dict(id='IT-02-019', flow='공고문 상세', kind='예외', title='무료회원이 공고문 상세 주소를 직접 열면 회원 안내를 본다',
     pre='무료회원, 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 주소로 연다\n'
           '2. [요금 안내] 를 누른다\n'
           '3. 다시 열고 [마감 전 공고로] 를 누른다',
     expect='1. 회원 안내 상자에 "공고문은 회원 전용입니다" 와 [요금 안내], [마감 전 공고로] 가 보인다\n'
            '2. 공고 요약, 요구 서류, 판정 상자, [제안서 작성] 이 없다\n'
            '3. GET /opportunities/{N} 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '4. 2번 뒤 요금 안내(/pricing)가 열린다\n'
            '5. 3번 뒤 마감 전 공고(/search)가 열린다',
     judge=['가상', '가상', '서버', '가상', '가상'],
     req=['BID-BROWSE-002', 'BID-AUTH-008'], fc=['BID-BROWSE-002', 'BID-AUTH-008'], screen='/search/{id}', pri='High'),

dict(id='IT-02-020', flow='공고문 상세', kind='예외', title='로그인 없이 공고문 상세를 열면 로그인으로 보낸다',
     pre='비로그인, 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 주소로 연다\n'
           '2. 토큰 없이 GET /opportunities/{N} 을 부른다',
     expect='1. 로그인 화면(/login)이 열린다\n'
            '2. 2번 응답이 401 과 error LOGIN_REQUIRED, message "로그인이 필요합니다" 다',
     judge=['가상', '서버'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-021', flow='공고문 상세', kind='예외', title='없는 공고문 id 는 404 로 막는다',
     pre='회원, 없는 공고문 id 999999',
     steps='1. 공고 상세(/search/999999)를 연다\n'
           '2. GET /opportunities/999999 를 부른다',
     expect='1. 공고 상세 칸 대신 404 페이지가 보인다\n'
            '2. 2번 응답이 404 다',
     judge=['가상', '서버'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-022', flow='공고문 상세', kind='예외', title='공고문 조회가 실패하면 오류 문구와 목록 가는 길을 둔다',
     pre='회원, GET /opportunities/{id} 가 500 으로 실패하게 한 환경',
     steps='1. 공고 상세(/search/{N})를 연다',
     expect='1. 공고 요약 칸 대신 불러오지 못했다는 오류 문구가 보인다\n'
            '2. 마감 전 공고 목록(/search)으로 가는 링크가 있다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High',
     issue='T3-4 는 "에러 메시지 표시, 목록으로 이동 유도" 로만 적어 문구가 정해지지 않았다'),

dict(id='IT-02-023', flow='공고문 상세', kind='예외', title='요구사항을 못 불러오면 필요 서류 칸만 빈 상태로 둔다',
     pre='회원, 공개 공고문 N, GET /opportunities/{N}/requirements 가 실패하게 한 환경',
     steps='1. 공고 상세(/search/{N})를 연다',
     expect='1. 필요 서류 체크리스트 칸이 빈 상태다\n'
            '2. 공고 헤더와 공고 요약은 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-003', 'BID-BROWSE-002'], fc=['BID-BROWSE-003', 'BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-024', flow='공고문 상세', kind='예외', title='첨부를 못 불러오면 첨부문서 칸만 빈 상태로 둔다',
     pre='회원, 공개 공고문 N, GET /opportunities/{N}/attachments 가 실패하게 한 환경',
     steps='1. 공고 상세(/search/{N})를 연다',
     expect='1. 첨부문서 칸이 빈 상태다\n'
            '2. 공고 헤더와 공고 요약은 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/{id}', pri='High'),

dict(id='IT-02-025', flow='원본 공고 상세', kind='정상', title='공개 공고문이 없는 모은 공고를 원본 공고 상세로 본다',
     pre='회원, 공개 공고문이 없고 원문 본문이 있으며 한글 본문(descriptionKo)이 없는 마감 전 공고 G',
     steps='1. 원본 공고 상세(/search/origin/{G 의 opportunityId})를 연다\n'
           '2. 머리 오른쪽 "SAM.gov 원문 보기" 를 누른다',
     expect='1. 머리에 유형 꼬리표, 한글 제목, 작은 글씨의 원문 제목, 공고번호, 발주처, 게시일, 마감이 보인다\n'
            '2. 본문 자리에 "아직 한글 공고문이 없는 공고라 원문을 보여 드립니다" 가 보인다\n'
            '3. 원문 본문이 줄바꿈을 그대로 지켜 펼쳐져 있다\n'
            '4. 원문 본문 위에 "한글 번역을 준비하고 있습니다" 가 보인다\n'
            '5. 2번 뒤 새 창에 응답 samUrl 의 SAM.gov 원문 공개 주소가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-026', flow='원본 공고 상세', kind='정상', title='한글 본문이 있으면 한글 본문을 먼저 보이고 원문은 접어 둔다',
     pre='회원, 공개 공고문이 없고 한글 본문과 원문 본문이 모두 있는 마감 전 공고 H',
     steps='1. 원본 공고 상세(/search/origin/{H 의 opportunityId})를 연다\n'
           '2. "원문 보기" 를 누른다\n'
           '3. "원문 보기" 를 다시 누른다',
     expect='1. 본문 자리 안내가 "아직 한글 공고문이 없는 공고라 SAM.gov 원문을 한글로 옮겨 보여 드립니다" 다\n'
            '2. 한글 본문이 줄바꿈 그대로 먼저 보이고 원문 본문은 접혀 있다\n'
            '3. 2번 뒤 원문 본문이 한글 본문 아래에 펼쳐진다\n'
            '4. 3번 뒤 원문 본문이 다시 접힌다\n'
            '5. GET /opportunities/origin/{id} 응답의 descriptionKo 가 화면의 한글 본문과 같다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-027', flow='원본 공고 상세', kind='정상', title='원문 본문이 없으면 없다고 밝히고 SAM.gov 원문으로 잇는다',
     pre='회원, 공개 공고문과 원문 본문이 모두 없는 모은 공고 J',
     steps='1. 원본 공고 상세(/search/origin/{J 의 opportunityId})를 연다\n'
           '2. 본문 자리의 SAM.gov 원문 보기 링크를 누른다',
     expect='1. 본문 자리에 "모아 둔 원문 본문이 없습니다" 가 보인다\n'
            '2. 2번 뒤 새 창에 J 의 SAM.gov 원문 공개 주소가 열린다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-028', flow='원본 공고 상세', kind='정상', title='원본 공고 상세의 첨부 주소와 첨부 없음 문구를 본다',
     pre='회원, 공개 공고문이 없고 SAM.gov 첨부 주소가 2개인 모은 공고 K 와 첨부가 없는 모은 공고 L',
     steps='1. K 의 원본 공고 상세를 연다\n'
           '2. L 의 원본 공고 상세를 연다',
     expect='1. K 화면에 SAM.gov 첨부 주소 2개가 목록으로 보인다\n'
            '2. L 화면의 첨부 자리에 "첨부 파일이 없습니다" 가 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-029', flow='원본 공고 상세', kind='정상', title='원본 공고 상세에는 공고문에 붙는 기능 대신 관심 공고 단추만 둔다',
     pre='무료회원, 공개 공고문이 없는 모은 공고 G',
     steps='1. G 의 원본 공고 상세를 연다',
     expect='1. 머리 단추가 "SAM.gov 원문 보기" 와 [관심 공고 저장] 둘이다\n'
            '2. 자격 판정 상자가 없다\n'
            '3. 입찰 신청과 [제안서 작성] 단추가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-002', 'BID-BROWSE-004'], fc=['BID-BROWSE-002', 'BID-BROWSE-004'], screen='/search/origin/{opportunityId}', pri='High',
     issue='T3-4 원본 공고 상세 6단계는 관심 공고를 그리지 않는다고 하나 T1-1 CR-157 과 T3-3 회원 전용 화면은 관심 공고 단추를 둔다'),

dict(id='IT-02-030', flow='원본 공고 상세', kind='정상', title='회원은 공개 공고문이 있는 원본 주소에서 공고문 상세로 넘어간다',
     pre='회원, 공개 공고문 N 이 있는 마감 전 공고 F',
     steps='1. 마감 전 공고(/search)에서 F 줄을 누른다\n'
           '2. 원본 공고 상세 주소(/search/origin/{F 의 opportunityId})를 주소창에 넣어 연다\n'
           '3. 브라우저 뒤로 가기를 누른다',
     expect='1. 1번 뒤 공고 상세(/search/{N})가 열린다\n'
            '2. 2번 뒤에도 주소가 /search/{N} 으로 바뀌고 N 의 공고문 제목이 보인다\n'
            '3. GET /opportunities/origin/{id} 응답의 publicNoticeId 가 N 이다\n'
            '4. 1번 화면에서 뒤로 가기를 누르면 원본 공고 상세를 거치지 않고 마감 전 공고(/search)로 간다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-BROWSE-002', 'BID-AUTH-008'], fc=['BID-BROWSE-002', 'BID-AUTH-008'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-031', flow='원본 공고 상세', kind='정상', title='무료회원은 공개 공고문이 있어도 원본 공고 상세에 머문다',
     pre='무료회원, 공개 공고문 N 이 있는 마감 전 공고 F',
     steps='1. 마감 전 공고(/search)에서 "회원" 꼬리표가 달린 F 줄을 누른다\n'
           '2. 본문 위 [요금 안내] 를 누른다',
     expect='1. 원본 공고 상세(/search/origin/{F 의 opportunityId})에 머물고 공고 상세로 넘어가지 않는다\n'
            '2. 본문 위에 "AI 공고문은 회원 전용입니다" 로 시작하는 안내와 [요금 안내] 가 보인다\n'
            '3. GET /opportunities/origin/{id} 응답에 publicNoticeId N 이 들어 있다\n'
            '4. 2번 뒤 요금 안내(/pricing)가 열린다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-BROWSE-002', 'BID-AUTH-008'], fc=['BID-BROWSE-002', 'BID-AUTH-008'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-032', flow='원본 공고 상세', kind='정상', title='모은 공고 화면과 관심 공고 목록에서 원본 공고 상세로 들어온다',
     pre='회원, 공개 공고문이 없는 공고가 사전 공고, 변경 공고 목록에 있고 그중 하나 Q 를 관심 공고로 저장한 상태',
     steps='1. 사전 공고(/bids/pre-solicitation)에서 공개 공고문이 없는 줄을 누른다\n'
           '2. 변경 공고(/bids/amendments)에서 공개 공고문이 없는 줄을 누른다\n'
           '3. 관심 공고(/bookmarks)에서 Q 줄을 누른다',
     expect='1. 1번 뒤 그 공고의 원본 공고 상세(/search/origin/{opportunityId})가 열린다\n'
            '2. 2번 뒤 그 공고의 원본 공고 상세(/search/origin/{opportunityId})가 열린다\n'
            '3. 3번 뒤 Q 의 원본 공고 상세(/search/origin/{opportunityId})가 열린다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-033', flow='원본 공고 상세', kind='예외', title='없는 id 나 지운 공고의 원본 공고 상세는 열지 않는다',
     pre='회원, 없는 opportunityId 999999 와 지운 모은 공고 R',
     steps='1. 원본 공고 상세(/search/origin/999999)를 연다\n'
           '2. 마감 전 공고로 가는 단추를 누른다\n'
           '3. R 의 원본 공고 상세를 연다',
     expect='1. "공고를 찾을 수 없습니다" 와 마감 전 공고로 가는 단추가 보인다\n'
            '2. GET /opportunities/origin/999999 응답이 404 다\n'
            '3. 2번 뒤 마감 전 공고(/search)가 열린다\n'
            '4. 3번 뒤에도 "공고를 찾을 수 없습니다" 가 보인다',
     judge=['가상', '서버', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-034', flow='원본 공고 상세', kind='예외', title='원본 공고 조회가 404 가 아닌 오류면 다시 시도 단추를 둔다',
     pre='회원, GET /opportunities/origin/{id} 가 500 으로 실패하게 한 환경',
     steps='1. 원본 공고 상세를 연다\n'
           '2. 실패를 풀고 다시 시도 단추를 누른다',
     expect='1. 다시 시도 단추가 보이고 "공고를 찾을 수 없습니다" 는 없다\n'
            '2. 2번 뒤 화면 머리에 그 공고의 공고번호와 원문 제목이 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-035', flow='원본 공고 상세', kind='예외', title='로그인하지 않으면 원본 공고 상세를 열지 않는다',
     pre='비로그인, 공개 공고문이 없는 모은 공고 G',
     steps='1. 원본 공고 상세(/search/origin/{G 의 opportunityId})를 주소로 연다',
     expect='1. 로그인 화면(/login)이 열린다',
     judge=['가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High',
     issue='T3-3 과 T3-4 CR-152 는 원본 공고 상세에 로그인이 필요하다고 하나 BIZ-071 과 T3-2 는 원본 공고 상세 API 를 모두에게 연다'),

dict(id='IT-02-036', flow='필요 문서', kind='정상', title='회원이 공고문 상세에서 필요 서류 체크리스트를 본다',
     pre='회원, 분석이 COMPLETED 이고 공고 분석 제출 서류에 필수와 선택 항목이 있는 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})의 필요 서류 체크리스트 칸을 본다\n'
           '2. GET /opportunities/{N}/requirements 를 부른다',
     expect='1. 줄마다 서류명, 필수 또는 선택 배지, 제출 형식, 페이지 제한이 있다\n'
            '2. 필수 항목에 "필수" 배지가 붙는다\n'
            '3. 2번 응답이 DOCUMENT, CERTIFICATION, EXPERIENCE 같은 카테고리별로 묶여 있다\n'
            '4. 2번 응답의 항목마다 BLOCKER 여부가 있다\n'
            '5. 2번 응답의 항목이 그 공고 분석의 제출 서류 항목과 같다',
     judge=['가상', '가상', '서버', '서버', '서버'],
     req=['BID-BROWSE-003'], fc=['BID-BROWSE-003'], screen='/search/{id}', pri='High'),

dict(id='IT-02-037', flow='필요 문서', kind='예외', title='공고 분석이 없으면 필요 서류 체크리스트를 그리지 않는다',
     pre='회원, 분석이 끝나지 않아 제출 서류가 없는 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 연다',
     expect='1. 필요 서류 체크리스트 칸이 없다\n'
            '2. 원본 description 과 resourceLinks 는 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-003'], fc=['BID-BROWSE-003'], screen='/search/{id}', pri='High'),

dict(id='IT-02-038', flow='필요 문서', kind='예외', title='무료회원이 요구 서류 API 를 부르면 403 으로 막는다',
     pre='무료회원 토큰, 공개 공고문 N',
     steps='1. GET /opportunities/{N}/requirements 를 부른다\n'
           '2. GET /opportunities/recent 를 부른다',
     expect='1. 1번 응답이 403 과 error MEMBERSHIP_REQUIRED 이고 요구 서류 항목이 담기지 않는다\n'
            '2. 2번 응답이 403 과 error MEMBERSHIP_REQUIRED 다',
     judge=['서버', '서버'],
     req=['BID-BROWSE-003', 'BID-BROWSE-001'], fc=['BID-BROWSE-003', 'BID-BROWSE-001'], screen='', pri='High'),

dict(id='IT-02-039', flow='관심 공고', kind='정상', title='마감 전 공고 줄에서 관심 공고를 저장한다',
     pre='무료회원, 관심 공고 0건, 마감 전 공고 G(opportunityId M)',
     steps='1. 마감 전 공고(/search)에서 G 줄의 관심 공고 단추(마우스를 올리면 "관심 공고 저장")를 누른다\n'
           '2. 머리말 관심 공고 아이콘을 본다\n'
           '3. 관심 공고(/bookmarks)를 연다',
     expect='1. G 줄의 단추가 저장된 모양으로 바뀌고 마우스를 올리면 "북마크 제거" 가 보인다\n'
            '2. POST /bookmarks/{M} 가 모은 공고 번호 M 으로 불리고 성공한다\n'
            '3. 그 회원의 관심 공고 기록이 opportunity M 으로 1건 생긴다\n'
            '4. 머리말 관심 공고 아이콘 위 동그라미 숫자가 1 이다\n'
            '5. 관심 공고 목록에 G 의 공고번호와 제목이 있는 줄 1개가 있다',
     judge=['가상', '서버', '서버', '가상', '가상'],
     req=['BID-BROWSE-004'], fc=['BID-BROWSE-004'], screen='/search', pri='Low'),

dict(id='IT-02-040', flow='관심 공고', kind='정상', title='공고문 상세의 관심 공고 단추는 모은 공고 번호로 저장되어 목록과 같은 것을 가리킨다',
     pre='회원, 공개 공고문 N 이 있는 마감 전 공고 F(opportunityId M), 관심 공고 0건',
     steps='1. 마감 전 공고(/search)에서 F 줄의 관심 공고 단추를 누른다\n'
           '2. 공고 상세(/search/{N})를 연다\n'
           '3. [저장됨] 을 누른다\n'
           '4. 마감 전 공고(/search)로 돌아간다',
     expect='1. 2번 화면 단추 글자가 "저장됨" 이다\n'
            '2. 2번에서 GET /bookmarks/{M}/status 가 응답의 opportunityId M 으로 불리고 true 다\n'
            '3. 3번 뒤 단추 글자가 "관심 공고 저장" 으로 바뀐다\n'
            '4. 3번에서 DELETE /bookmarks/{M} 가 불리고 그 회원의 관심 공고 기록이 0건이 된다\n'
            '5. 4번 뒤 F 줄 단추에 마우스를 올리면 "관심 공고 저장" 이 보인다',
     judge=['가상', '서버', '가상', '서버', '가상'],
     req=['BID-BROWSE-004'], fc=['BID-BROWSE-004'], screen='/search/{id}', pri='Low'),

dict(id='IT-02-041', flow='관심 공고', kind='정상', title='무료회원이 원본 공고 상세에서 관심 공고를 저장한다',
     pre='무료회원, 관심 공고 0건, 공개 공고문이 있는 마감 전 공고 F(opportunityId M)',
     steps='1. F 의 원본 공고 상세를 연다\n'
           '2. [관심 공고 저장] 을 누른다\n'
           '3. 관심 공고(/bookmarks)를 연다',
     expect='1. 단추 글자가 "저장됨" 으로 바뀐다\n'
            '2. 그 회원의 관심 공고 기록이 opportunity M 으로 1건 생긴다\n'
            '3. 관심 공고 목록에 F 의 공고번호가 있는 줄이 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-BROWSE-004', 'BID-BROWSE-002'], fc=['BID-BROWSE-004', 'BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='Low'),

dict(id='IT-02-042', flow='관심 공고', kind='정상', title='관심 공고 목록 줄을 누르면 회원 구분에 따라 상세로 간다',
     pre='회원 계정과 무료회원 계정이 각각 공개 공고문 N 이 있는 공고 F 와 공개 공고문이 없는 공고 G 를 관심 공고로 저장한 상태',
     steps='1. 회원으로 관심 공고(/bookmarks)에서 F 줄을 누른다\n'
           '2. 회원으로 G 줄을 누른다\n'
           '3. 무료회원으로 관심 공고(/bookmarks)에서 F 줄을 누른다',
     expect='1. 1번 뒤 원본 공고 상세 주소를 거쳐 공고 상세(/search/{N})가 열린다\n'
            '2. 2번 뒤 G 의 원본 공고 상세(/search/origin/{opportunityId})가 열린다\n'
            '3. 3번 뒤 F 의 원본 공고 상세에 머물고 "AI 공고문은 회원 전용입니다" 안내가 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-004', 'BID-AUTH-008'], fc=['BID-BROWSE-004', 'BID-AUTH-008'], screen='/bookmarks', pri='Low'),

dict(id='IT-02-043', flow='관심 공고', kind='정상', title='관심 공고 목록에서 해제하고 다른 계정의 관심 공고는 섞이지 않는다',
     pre='무료회원 A 가 공고 G1, G2 를, 무료회원 B 가 공고 G3 을 관심 공고로 저장한 상태',
     steps='1. A 로 관심 공고(/bookmarks)를 연다\n'
           '2. G1 줄의 해제 단추(마우스를 올리면 "북마크 제거")를 누른다\n'
           '3. B 로 관심 공고(/bookmarks)를 연다',
     expect='1. 1번 목록이 G1, G2 두 줄이고 G3 줄은 없다\n'
            '2. 2번 뒤 목록이 G2 한 줄이고 머리말 관심 공고 동그라미 숫자가 1 이다\n'
            '3. A 의 관심 공고 기록이 G2 1건이다\n'
            '4. 3번 목록이 G3 한 줄이다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-BROWSE-004'], fc=['BID-BROWSE-004'], screen='/bookmarks', pri='Low'),

dict(id='IT-02-044', flow='관심 공고', kind='예외', title='로그인하지 않으면 관심 공고를 쓰지 못한다',
     pre='비로그인, 마감 전 모은 공고 G(opportunityId M)',
     steps='1. 관심 공고(/bookmarks)를 주소로 연다\n'
           '2. 토큰 없이 POST /bookmarks/{M} 을 부른다\n'
           '3. 토큰 없이 GET /bookmarks 를 부른다',
     expect='1. 1번 뒤 로그인 화면(/login)이 열린다\n'
            '2. 2번 응답이 401 이고 관심 공고 기록이 생기지 않는다\n'
            '3. 3번 응답이 401 이다',
     judge=['가상', '서버', '서버'],
     req=['BID-BROWSE-004'], fc=['BID-BROWSE-004'], screen='/bookmarks', pri='Low',
     issue='비로그인이 목록 줄의 관심 공고 단추를 누를 때 화면 반응은 T3-4 CR-155 에 "지금과 같다" 로만 적혀 정해지지 않았다'),

dict(id='IT-02-045', flow='관심 공고', kind='예외', title='관심 공고 여부를 못 불러오면 단추를 막는다',
     pre='회원, 공개 공고문 N, GET /bookmarks/{id}/status 가 실패하게 한 환경',
     steps='1. 공고 상세(/search/{N})를 연다\n'
           '2. 관심 공고 단추를 누른다',
     expect='1. 관심 공고 단추가 비활성이다\n'
            '2. 2번 뒤 POST /bookmarks 요청이 가지 않는다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-004'], fc=['BID-BROWSE-004'], screen='/search/{id}', pri='Low'),

dict(id='IT-02-046', flow='홈 첫 화면', kind='정상', title='공고가 있을 때 비로그인 홈 첫 화면의 마감 임박 탭을 본다',
     pre='비로그인, 7일 안 마감 공고가 8건 이상이고 마감 전 공고가 그보다 많은 환경',
     steps='1. 홈(/)을 연다\n'
           '2. GET /opportunities/collected?openOnly=true&latestOnly=true&sort=DEADLINE&dueWithinDays=7&size=7 을 불러 결과를 적는다',
     expect='1. 왼쪽 마감 전 공고 머리에 제목, 탭(마감 임박, 최신 공고), 전체 건수, 빨간 7일 안 마감 건수가 보인다\n'
            '2. 처음 켜진 탭이 마감 임박이고 목록이 7줄이다\n'
            '3. 7줄이 2번 결과와 같은 공고이고 마감이 가까운 순이다\n'
            '4. 줄마다 남은 날(D-n), 한글 제목, 공고번호, 공고 유형, 기지, 마감 월.일(요일)이 있다\n'
            '5. 전체 건수가 마감 전 공고(/search) 숫자 칸 건수와 같다\n'
            '6. 7일 안 마감 건수가 2번 결과의 totalElements 와 같다\n'
            '7. 첫 화면에 들어올 때 두 collected 요청(최신 공고용, 마감 임박용)을 한 번에 부른다',
     judge=['가상', '가상', '서버', '가상', '가상', '서버', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-047', flow='홈 첫 화면', kind='정상', title='최신 공고 탭으로 바꾸고 줄을 눌러 상세로 간다',
     pre='비로그인 뒤 회원으로 로그인, 마감 전 공고가 7건 이상이고 공개 공고문이 있는 공고 F 와 없는 공고 G 가 최신 7줄 안에 있는 환경',
     steps='1. 홈(/)에서 최신 공고 탭을 누른다\n'
           '2. F 줄을 누른다\n'
           '3. 홈으로 돌아와 최신 공고 탭에서 G 줄을 누른다\n'
           '4. 홈 목록 아래 "마감 전 공고 {n}건 모두 보기" 를 누른다',
     expect='1. 1번 뒤 목록이 게시일 최신순 7줄로 바뀐다\n'
            '2. 2번 뒤 공고 상세(/search/{F 의 공고문 id})가 열린다\n'
            '3. 3번 뒤 원본 공고 상세(/search/origin/{G 의 opportunityId})가 열린다\n'
            '4. 4번 뒤 마감 전 공고(/search)가 열린다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-048', flow='홈 첫 화면', kind='정상', title='비로그인 첫걸음 상자와 다섯 단계 띠를 본다',
     pre='비로그인, 마감 전 공고가 1건 이상, 요금 조회에 회원 월 금액이 있는 환경',
     steps='1. 홈(/)을 연다\n'
           '2. 첫걸음 상자의 금액, 무료로 시작, 로그인, 등록 대행을 차례로 눌러 본다\n'
           '3. 다섯 단계 띠의 한 단계를 누른다',
     expect='1. 오른쪽 첫걸음 상자에 사진과 꼬리표 "USFK 입찰 첫걸음", 대표 문장 두 줄, 무료 회원 혜택 3가지, 회원 요금 한 줄, SAM 등록 안내 한 줄이 있다\n'
            '2. 회원 요금 한 줄의 금액이 GET /pricing 의 회원 월 금액과 같다\n'
            '3. 금액을 누르면 요금 안내(/pricing), 무료로 시작은 회원가입(/register), 로그인은 /login, 등록 대행은 SAM 등록 대행(/services/sam-registration)이 열린다\n'
            '4. 다섯 단계 띠에 1 찾기, 2 판단, 3 준비, 4 제출, 5 낙찰 뒤와 단계별 서비스 한 줄이 있다\n'
            '5. 3번 뒤 화면이 아래 서비스 구역으로 옮겨진다\n'
            '6. 큰 제목(대표 문장)이 첫 화면에 한 곳에만 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-049', flow='홈 첫 화면', kind='정상', title='마감 전 공고가 0건일 때 비로그인 홈 첫 화면을 본다',
     pre='비로그인, 마감 전 모은 공고가 0건인 환경',
     steps='1. 홈(/)을 연다\n'
           '2. 무료 회원 상자의 무료로 시작과 로그인을 눌러 본다',
     expect='1. 왼쪽 머리 한 줄에 0건, "준비 중", 빨갛지 않은 기준일이 보인다\n'
            '2. 대표 문장 "주한미군 공사·용역 입찰, 한글로 찾고 순서대로 준비하세요" 가 두 줄로 보인다\n'
            '3. 마감된 공고 예시 요약에 제목, 공고번호, 계약 방식, 계약 기간, 평가 방식, 참가 자격 수·제출 서류 수가 있다\n'
            '4. 오른쪽 무료 회원 상자에 머리 "무료 회원", 사진과 꼬리표 "USFK 입찰 첫걸음", 무료 회원 혜택 3가지가 있다\n'
            '5. 무료 회원 상자에 대표 문장, 회원 요금 한 줄, SAM 등록 안내가 없다\n'
            '6. 무료로 시작을 누르면 회원가입(/register), 로그인을 누르면 /login 이 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-050', flow='홈 첫 화면', kind='정상', title='로그인한 회원의 홈 오른쪽 상자를 본다',
     pre='회원, 관심 공고 2건, 입찰 신청 1건, 안 읽은 알림 3건, 마감 전 공고가 1건 이상인 환경',
     steps='1. 홈(/)을 연다\n'
           '2. 관심 공고 수, 입찰 신청 수, 새 알림 수를 차례로 누른다\n'
           '3. 맞춤 판정 자리의 회사 정보 고치기와 바로가기의 서류함을 누른다',
     expect='1. 오늘 확인할 것에 "{회사명} 님", 관심 공고 2, 입찰 신청 1, 빨간 새 알림 3 이 보인다\n'
            '2. 관심 공고 수를 누르면 /bookmarks, 입찰 신청 수는 /proposals, 새 알림 수는 알림 목록 주소로 간다\n'
            '3. 우리 회사 맞춤 판정 자리에 "준비 중" 안내와 회사 정보 넣기 안내가 있고 판정 수 숫자는 없다\n'
            '4. 회사 정보 고치기를 누르면 /profile 이 열린다\n'
            '5. 바로가기에 서류함 하나만 한 줄을 다 써서 있고 재발주 달력은 없다\n'
            '6. 서류함을 누르면 /documents 가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-051', flow='홈 첫 화면', kind='정상', title='회원이어도 공고가 0건이면 왼쪽은 공고 0건 모양이다',
     pre='회원, 마감 전 모은 공고가 0건인 환경',
     steps='1. 홈(/)을 연다',
     expect='1. 왼쪽에 0건, "준비 중", 대표 문장 "주한미군 공사·용역 입찰, 한글로 찾고 순서대로 준비하세요" 가 보인다\n'
            '2. 오른쪽은 오늘 확인할 것 상자로 "{회사명} 님" 이 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-052', flow='홈 첫 화면', kind='정상', title='7일 안 마감 공고가 없으면 마감 임박 탭이 빈다',
     pre='비로그인, 마감 전 공고는 있으나 모두 8일 뒤 이후 마감인 환경',
     steps='1. 홈(/)을 연다\n'
           '2. 최신 공고 탭을 누른다',
     expect='1. 7일 안 마감 건수가 0 이다\n'
            '2. 마감 임박 탭 목록이 0줄이다\n'
            '3. 2번 뒤 최신 공고 탭 목록이 게시일 최신순 줄로 채워진다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High',
     issue='T3-4 CR-155 는 마감 임박이 없을 때 "지금 문구" 를 쓴다고만 해 문구가 설계서에 없다'),

dict(id='IT-02-053', flow='홈 첫 화면', kind='정상', title='홈 사진 자리는 관리자 사진 묶음에서 고르고 없으면 바탕색만 남긴다',
     pre='비로그인, 관리자 사진 묶음 HOME_INTRO 에 사진 2장, HOME_EMPTY 는 0장인 환경',
     steps='1. 마감 전 공고가 있는 상태에서 홈(/)을 여러 번 새로 연다\n'
           '2. 마감 전 공고가 0건인 상태에서 홈(/)을 연다\n'
           '3. 사진 목록 조회가 느리게 오도록 한 뒤 홈(/)을 연다',
     expect='1. 첫걸음 상자에 관리자가 HOME_INTRO 에 올린 2장 중 한 장이 방문마다 골라져 로드된다\n'
            '2. 2번 무료 회원 상자의 사진 자리에 사진이 없고 칸 바탕색만 있다\n'
            '3. 3번에서 공고 목록과 글자가 사진보다 먼저 그려지고 사진은 목록이 온 뒤 그려진다\n'
            '4. 사진 자리의 꼬리표 "USFK 입찰 첫걸음" 과 문구는 사진과 상관없이 같다',
     judge=['합동', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-054', flow='홈 첫 화면', kind='정상', title='홈 첫 화면 캡처가 화면 정본과 같다',
     pre='비로그인, 1440x900 창, 화면 정본 홈시안_파랑.html 과 shots/정본_파랑_*.png',
     steps='1. 공고 0건, 공고 있음, 회원 세 상태에서 홈(/)을 1440x900 으로 캡처한다\n'
           '2. 정본 #s0, #s1, #s2 캡처와 견준다',
     expect='1. 공고 0건 캡처가 정본 #s0 과 같은 배치다\n'
            '2. 공고 있음 캡처가 정본 #s1 과 같은 배치다\n'
            '3. 회원 캡처가 정본 #s2 와 같은 배치다',
     judge=['QA', 'QA', 'QA'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-055', flow='홈 첫 화면', kind='예외', title='홈 마감 전 공고 목록을 못 받으면 실패 모양을 보인다',
     pre='비로그인, GET /opportunities/collected 가 실패하게 한 환경',
     steps='1. 홈(/)을 연다',
     expect='1. 왼쪽 마감 전 공고 자리에 목록 줄이 없고 불러오지 못했다는 실패 안내가 보인다\n'
            '2. 오른쪽 첫걸음 상자와 다섯 단계 띠는 그대로 그려진다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High',
     issue='T3-4 CR-155 는 홈 목록 실패를 "지금과 같다(실패 모양)" 로만 적어 문구가 정해지지 않았다'),

dict(id='IT-02-056', flow='홈 아래 구역', kind='정상', title='홈 아래 서비스 구역을 본다',
     pre='비로그인',
     steps='1. 홈(/)을 아래로 내린다',
     expect='1. 회색 띠 서비스 구역 제목이 "입찰하는 순서대로 도와 드립니다" 다\n'
            '2. 단계 5열에 단계별 설명, 서비스 이름, 등급 꼬리표(무료, 회원, 건별)가 있다\n'
            '3. 서비스 이름에 재발주 예측과 옵션 연장 알림이 없다\n'
            '4. 범례의 회원 금액이 GET /pricing 의 회원 금액과 같다\n'
            '5. 아래 구역이 회색 띠와 흰 띠를 번갈아 서비스, 회원 혜택, 요금, 도움 순으로 놓인다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-057', flow='홈 아래 구역', kind='정상', title='홈 회원 혜택 구역의 카드와 연결을 본다',
     pre='무료회원',
     steps='1. 홈(/) 회원 혜택 구역을 본다\n'
           '2. 공고문 예시 보기를 누른다\n'
           '3. 회사 정보 넣어 보기를 누른다',
     expect='1. 흰 띠 제목이 "회원은 한 걸음 더 봅니다" 다\n'
            '2. 카드가 공고문 상세 번역, 우리 회사 맞춤 판정 두 장이고 재발주 예측 달력 카드는 없다\n'
            '3. 카드마다 설명과 "화면 예시입니다" 그림이 있다\n'
            '4. 2번 뒤 항목별 준비 안내(/coming-soon/{항목키})가 열린다\n'
            '5. 3번 뒤 회사 정보(/profile) 화면이 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High',
     issue='T3-3 홈 CR-133 줄은 공고문 예시를 항목별 준비 안내로 보내나 CR-135 메뉴 목적지에는 공고문 예시 화면 /bids/sample 이 있다'),

dict(id='IT-02-058', flow='홈 아래 구역', kind='정상', title='홈 요금 구역의 세 칸과 단추를 본다',
     pre='비로그인, 요금 조회에 무료, 회원 단계와 금액이 있는 환경',
     steps='1. 홈(/) 요금 구역을 본다\n'
           '2. 무료로 시작, 회원으로 시작, 상담 신청, 사진 안내 두 칸을 차례로 누른다',
     expect='1. 칸이 무료, 회원(추천), 대행·컨설팅 셋이고 칸마다 이름, 금액, 대상, 포함 항목이 있다\n'
            '2. 무료와 회원 칸의 금액과 단계가 GET /pricing 응답과 같다\n'
            '3. 사진 안내가 등록 대행, 현장 지원 두 칸이고 무료 입찰 교실 칸은 없다\n'
            '4. 무료로 시작은 /register, 회원으로 시작은 /pricing, 상담 신청은 1:1 문의(/support)를 연다\n'
            '5. 사진 안내 등록 대행은 /services/sam-registration, 현장 지원은 /services/site-visit 을 연다',
     judge=['가상', '서버', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-059', flow='홈 아래 구역', kind='정상', title='홈 도움 구역의 탭이 자주 묻는 질문, 공지사항 화면과 같은 목록을 읽는다',
     pre='비로그인, 공지 3건과 자주 묻는 질문 8건이 있는 환경',
     steps='1. 홈(/) 도움 구역의 자주 묻는 질문 탭을 본다\n'
           '2. 공지사항 탭을 누른다\n'
           '3. 더 보기와 줄 하나를 누른다\n'
           '4. 도움 바로가기 3개와 관련 사이트 하나를 누른다',
     expect='1. 자주 묻는 질문 탭의 줄이 /faq 목록 앞쪽과 같은 순서로 최대 6줄이다\n'
            '2. 공지사항 탭의 줄이 /notices 목록과 같은 3줄이다\n'
            '3. 더 보기와 줄은 켜진 탭에 따라 /faq 또는 /notices 를 연다\n'
            '4. 도움 바로가기는 1:1 문의, 카카오톡 알림 받기, 이용 안내 3개이고 1:1 문의는 /support, 카카오톡 알림은 /coming-soon/kakao 를 연다\n'
            '5. 관련 사이트 6개가 있고 누르면 새 창으로 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-060', flow='홈 아래 구역', kind='정상', title='공지가 0건이면 홈 공지사항 탭에 0건 문구를 보인다',
     pre='비로그인, 공지 0건인 환경',
     steps='1. 홈(/) 도움 구역의 공지사항 탭을 누른다',
     expect='1. 공지사항 탭에 "아직 공지가 없습니다" 가 보인다\n'
            '2. 견본 공지 줄이 없다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-005'], fc=['BID-BROWSE-005'], screen='/', pri='High'),

dict(id='IT-02-061', flow='공통 틀', kind='정상', title='유틸 띠가 로그인 여부에 따라 바뀐다',
     pre='비로그인, 무료회원 계정',
     steps='1. 비로그인으로 홈(/)을 연다\n'
           '2. 유틸 띠의 회원가입, 로그인, 고객센터를 차례로 누른다\n'
           '3. 무료회원으로 로그인해 유틸 띠를 본다',
     expect='1. 유틸 띠 왼쪽에 서비스 한 줄이 있고 오른쪽이 회원가입(강조), 로그인, 이용 안내, 고객센터 순이다\n'
            '2. 회원가입은 /register, 로그인은 /login, 고객센터는 /support 를 연다\n'
            '3. 로그인 뒤 오른쪽이 "{회사명} 님"(강조), 로그아웃, 고객센터 순으로 바뀐다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-062', flow='공통 틀', kind='정상', title='머리말의 로고, 검색, 아이콘과 아이콘 숫자를 본다',
     pre='무료회원, 관심 공고 2건, 안 읽은 알림 1건',
     steps='1. 마감 전 공고(/search)에서 머리말 로고를 누른다\n'
           '2. 검색 구분을 펼치고 상세검색을 누른다\n'
           '3. 아이콘 3개를 차례로 누른다\n'
           '4. 로그아웃하고 머리말 아이콘을 본다',
     expect='1. 로고 윗줄에 "미군·연방 조달 입찰 대행" 이 있고 누르면 홈(/)이 열린다\n'
            '2. 검색 구분이 공고명·공고번호·기지명 셋이고 입력 칸, 검색 단추, 추천 검색어 5개, 상세검색이 있다\n'
            '3. 상세검색을 누르면 공고 검색(/search)이 열린다\n'
            '4. 아이콘이 관심 공고, 알림, 서류함 순이고 각각 /bookmarks, 알림 화면, /documents 를 연다\n'
            '5. 관심 공고 아이콘 위 동그라미에 2, 알림 아이콘 위 동그라미에 1 이 보인다\n'
            '6. 4번 뒤 아이콘 위 숫자 동그라미가 없다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-063', flow='공통 틀', kind='정상', title='메뉴 띠 7개와 요금 안내, 전체메뉴 단추를 본다',
     pre='비로그인',
     steps='1. 홈(/)의 메뉴 띠를 본다\n'
           '2. 메뉴 7개를 차례로 누른다\n'
           '3. 요금 안내 단추를 누른다',
     expect='1. 메뉴가 입찰공고, 낙찰정보, 발주분석, 맞춤 판정, 서류함, 등록·컨설팅, 고객센터 7개 순이고 재발주 예측과 입찰 교실은 없다\n'
            '2. 메뉴 오른쪽에 요금 안내 단추와 짙은 칸의 전체메뉴 단추가 있다\n'
            '3. 입찰공고는 /search, 서류함은 /documents, 고객센터는 /support 를 연다\n'
            '4. 낙찰정보는 /awards/recent, 발주분석은 /analysis/monthly, 맞춤 판정은 /profile, 등록·컨설팅은 /services/sam-registration 을 연다\n'
            '5. 요금 안내 단추는 /pricing 을 연다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-064', flow='공통 틀', kind='정상', title='전체메뉴를 열고 닫는다',
     pre='비로그인',
     steps='1. 전체메뉴 단추를 누른다\n'
           '2. 전체메뉴 단추를 다시 누른다\n'
           '3. 전체메뉴를 열고 바깥을 누른다\n'
           '4. 전체메뉴를 열고 항목 하나를 누른다',
     expect='1. 메뉴 띠 아래로 7묶음 34항목이 펼쳐진다\n'
            '2. 2번 뒤 전체메뉴가 닫힌다\n'
            '3. 3번 뒤 전체메뉴가 닫힌다\n'
            '4. 4번 뒤 누른 항목의 화면이 열리고 전체메뉴가 닫힌다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-065', flow='공통 틀', kind='정상', title='전체메뉴 입찰공고, 낙찰정보, 발주분석, 맞춤 판정 묶음의 목적지를 본다',
     pre='회원',
     steps='1. 전체메뉴에서 네 묶음의 항목을 하나씩 누른다',
     expect='1. 입찰공고 묶음은 마감 전 공고 /search, 사전 공고 /bids/pre-solicitation, 변경 공고 /bids/amendments, 관심 공고 /bookmarks, 공고문 예시 /bids/sample 순이다\n'
            '2. 낙찰정보 묶음은 최근 낙찰 /awards/recent, 낙찰 업체 /awards/awardees, 낙찰가 범위 /awards/range 순이다\n'
            '3. 발주분석 묶음은 월별 동향 /analysis/monthly, 업종별 발주 /analysis/naics, 기지별 지도 /analysis/base-map 순이다\n'
            '4. 맞춤 판정 묶음은 회사 정보 /profile, 적격성 진단 /fit/eligibility, 공고별 판정 /coming-soon/opportunity-fit 순이다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High',
     issue='T3-3 메뉴 목적지는 공고별 판정을 준비 안내로 두나 T3-3 회원 전용 화면에는 공고별 판정 화면 /fit/verdicts 가 있다'),

dict(id='IT-02-066', flow='공통 틀', kind='정상', title='전체메뉴 서류함, 등록·컨설팅, 고객센터 묶음의 목적지를 본다',
     pre='회원',
     steps='1. 전체메뉴에서 세 묶음의 항목을 하나씩 누른다',
     expect='1. 서류함 묶음은 공통 서류함 /documents, 공고별 서류 /proposals, 유효기간 알림 /documents/expiry, 제출 전 검사 /documents/pre-check 순이다\n'
            '2. 등록·컨설팅 묶음은 /services/sam-registration, /services/piee-registration, /services/sam-renewal, /services/site-visit, /services/invoicing, /services/translation, /services/teaming 과 제안서 자문 /coming-soon/proposal-review 다\n'
            '3. 고객센터 묶음은 /notices, /faq, /support, /academy/materials, /academy/glossary, /academy/forms, /pricing 과 카카오톡 알림 /coming-soon/kakao 다\n'
            '4. 무료 입찰 교실 /academy/class 와 재발주 예측 세 항목은 어느 묶음에도 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-067', flow='공통 틀', kind='정상', title='화면이 없는 항목은 항목별 준비 안내로 보낸다',
     pre='비로그인',
     steps='1. 전체메뉴 고객센터 > 카카오톡 알림을 누른다\n'
           '2. 지금 쓸 수 있는 것 카드 하나를 누른다\n'
           '3. 다시 열어 문의 띠의 1:1 문의를 누른다',
     expect='1. /coming-soon/kakao 가 속페이지 틀 안에 열린다\n'
            '2. 머리에 등급 꼬리표, 묶음 꼬리표, 항목 이름 "카카오톡 알림", 두세 줄 설명, 모래시계 아이콘과 "준비하고 있습니다" 가 있다\n'
            '3. 여는 날짜를 적은 글자가 없다\n'
            '4. 지금 쓸 수 있는 것 카드가 2~3개이고 카드마다 아이콘, 이름, 한 줄이 있다\n'
            '5. 문의 띠에 "궁금한 점은 1:1 문의로 남겨 주세요" 와 "남겨 주시면 이메일로 답변 드립니다" 가 있다\n'
            '6. 2번 뒤 누른 카드의 화면이 열리고 3번 뒤 /support 가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/coming-soon/{항목키}', pri='High'),

dict(id='IT-02-068', flow='공통 틀', kind='정상', title='모르는 항목 키와 키 없는 준비 안내는 일반 준비 중 안내를 보인다',
     pre='비로그인',
     steps='1. /coming-soon/no-such-key 를 연다\n'
           '2. /coming-soon 을 연다',
     expect='1. 1번 화면에 속페이지 틀이 없고 "준비 중입니다" 와 홈, 마감 전 공고, 1:1 문의 연결이 보인다\n'
            '2. 2번 화면에도 속페이지 틀 없이 "준비 중입니다" 와 홈, 마감 전 공고, 1:1 문의 연결이 보인다',
     judge=['가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/coming-soon/{항목키}', pri='High'),

dict(id='IT-02-069', flow='공통 틀', kind='정상', title='꼬리말 정책 링크와 상호를 본다',
     pre='비로그인',
     steps='1. 홈(/) 꼬리말을 본다\n'
           '2. 정책 링크 6개를 차례로 누른다',
     expect='1. 정책 링크가 이용약관, 개인정보처리방침(강조), 면책 고지, 환불 정책, 고객센터, 제휴 문의 6개 순이다\n'
            '2. 상호·이메일 줄과 저작권 줄이 있다\n'
            '3. 링크마다 그 이름의 정책 화면이 열린다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-070', flow='공통 틀', kind='정상', title='로그인과 회원가입 화면에도 같은 공통 틀을 건다',
     pre='비로그인',
     steps='1. 로그인 화면(/login)을 연다\n'
           '2. 회원가입 화면(/register)을 연다',
     expect='1. 두 화면 모두 유틸 띠, 머리말, 메뉴 띠, 꼬리말이 홈과 같은 모양으로 있다\n'
            '2. 두 화면 모두 본문 너비가 1200px 로 가운데 정렬이다\n'
            '3. 두 화면 모두 전체메뉴 단추를 누르면 홈과 같은 7묶음이 펼쳐진다',
     judge=['가상', '가상', '가상'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/login', pri='High'),

dict(id='IT-02-071', flow='공통 틀', kind='정상', title='강조색 묶음 7개 값만 바꾸면 고객 화면 색이 함께 바뀐다',
     pre='비로그인, 고객 화면을 다시 빌드할 수 있는 개발 환경',
     steps='1. index.css @theme 의 brand, brand-strong, brand-soft, brand-softer, brand-deep, brand-line, brand-on-dark 7개를 차콜과 주황 값으로 바꾸고 다시 빌드한다\n'
           '2. 홈, 마감 전 공고, 로그인, 회원가입 화면을 연다',
     expect='1. 주 단추, 링크, 검색 테두리의 색이 #c2410c 로 바뀐다\n'
            '2. 전체메뉴 단추와 선택된 탭 배경이 #23272e 로 바뀐다\n'
            '3. 꼬리말 개인정보처리방침 글자색이 #fdba8c 로 바뀐다\n'
            '4. danger, success 같은 고정 색은 그대로다',
     judge=['합동', '합동', '합동', '합동'],
     req=['BID-BROWSE-006'], fc=['BID-BROWSE-006'], screen='/', pri='High'),

dict(id='IT-02-072', flow='공고문 상세', kind='정상', title='다시 만든 공고문의 계약 기간은 연차별로, 작업 범위는 과업별로 보인다',
     pre='회원, CR-167 규칙으로 다시 만든 공개 공고문 N(기본 1년과 옵션 연도, 과업 여럿, 약어가 든 원문)',
     steps='1. 공고 상세(/search/{N})를 연다\n'
           '2. 관리자 공고문 관리에서 같은 공고문 상세를 연다',
     expect='1. 공고 요약에 작업 범위 다음 계약 기간 칸이 있다\n'
            '2. 계약 기간이 연차마다 한 줄에 "1년차(기본): 2027년 1월 1일부터 2027년 12월 31일" 꼴로 보이고 연장 조항은 끝 줄에 있다\n'
            '3. 작업 범위가 과업마다 줄이 나뉘어 보이고 줄마다 무엇을 하는 일인지 풀어 쓴 글이 있다\n'
            '4. 요약 글에 처음 나오는 약어와 규정 번호 옆에 한국어 뜻이 있다\n'
            '5. 관리자 공고문의 3. 계약 기간도 같은 연차별 줄로 보인다',
     judge=['가상', '합동', '합동', '합동', '합동'],
     req=['BID-BROWSE-002', 'BID-OPP-006'], fc=['BID-BROWSE-002', 'BID-OPP-006'], screen='/search/{id}', pri='High'),

dict(id='IT-02-073', flow='공고문 상세', kind='정상', title='받아 둔 첨부가 고객 첨부 칸에 보이고 무료회원도 내려받는다',
     pre='PIEE 파일과 SAM.gov 파일을 받아 둔 마감 전 공고 O, O 로 만든 공개 공고문 N, 무료회원 로그인',
     steps='1. 원본 공고 상세(/search/origin/{O})를 연다\n'
           '2. 받아 둔 파일 한 줄의 내려받기 단추를 누른다\n'
           '3. 회원으로 다시 로그인해 공고 상세(/search/{N})를 연다\n'
           '4. 같은 파일을 내려받는다',
     expect='1. 첨부 칸 맨 위에 받아 둔 파일 줄이 파일 이름, 출처 꼬리표(SAM.gov 또는 PIEE), 크기, 내려받기 단추로 보인다\n'
            '2. 그 파일 이름으로 저장되고 열린다\n'
            '3. 공고 상세 첨부 칸에도 같은 받아 둔 파일 줄이 보인다\n'
            '4. 같은 파일이 받아진다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

dict(id='IT-02-074', flow='공고문 상세', kind='예외', title='로그인하지 않으면 첨부를 내려받지 못하고, 받아 둔 파일이 없으면 SAM.gov 주소만 보인다',
     pre='받아 둔 파일이 있는 공고 O, 받아 둔 파일이 없고 SAM.gov 첨부 주소만 있는 공고 P',
     steps='1. 로그아웃한 채 원본 공고 상세 주소(/search/origin/{O})를 연다\n'
           '2. 로그인 토큰 없이 GET /api/opportunities/{O}/attachments/{첨부 id}/download 를 부른다\n'
           '3. O 의 첨부 id 로 GET /api/opportunities/{P}/attachments/{그 id}/download 를 로그인해 부른다\n'
           '4. 원본 공고 상세(/search/origin/{P})를 연다',
     expect='1. 로그인 화면으로 간다\n'
            '2. 401 이다\n'
            '3. 404 다\n'
            '4. 받아 둔 파일 줄 없이 SAM.gov 첨부 주소 줄만 보인다',
     judge=['가상', '서버', '서버', '가상'],
     req=['BID-BROWSE-002'], fc=['BID-BROWSE-002'], screen='/search/origin/{opportunityId}', pri='High'),

]
