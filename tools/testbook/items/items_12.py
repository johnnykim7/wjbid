# 영역 12 관리자/상태 관리 시험항목. 근거는 T1-1 기능요구사항 명세서, T1-3 BIZ-072, T1-5 상태 정의, T3-1, T3-2, T3-3, T3-4 화면 설계다.

# 전이 시험은 회차마다 넣는 시딩 입찰 건에만 건다(tools/testbook/seed/12-*.sql). 실제 고객 건이 움직이지 않는다
ADMIN = 'admin:smoke-admin@example.com'
THIS_BID = "UNHEX('{val:0:hex}')"
TRANSITION = '/api/admin/bid-requests/{val:0:id}/transition'
LAST_STEP = (f"SELECT state, JSON_LENGTH(state_history) AS hist, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].fromState')) AS from_state, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")

ITEMS = [

dict(id='IT-12-001', flow='대시보드', kind='정상', title='관리자가 대시보드 통계 카드에서 전체, 진행 중, 제출 완료 입찰 수를 본다',
     pre='관리자 계정, 상태가 서로 다른 입찰 요청 여러 건(CREATED, REVIEW, SUBMITTED 포함)',
     steps='1. 관리자 화면에 관리자 계정으로 로그인한다\n2. 왼쪽 메뉴 [대시보드] 를 누른다\n'
           '3. 왼쪽 메뉴 [입찰 요청 관리] 에서 상태 탭 [제출 완료] 를 고르고 건수를 센다',
     expect='1. 통계 카드 세 칸에 전체 입찰 요청, 진행 중, 제출 완료 수가 이름과 함께 보인다\n'
            '2. 통계 카드의 제출 완료 수가 입찰 요청 관리에서 제출 완료로 거른 건수와 같다\n'
            '3. 상태별 통계 응답(GET /api/admin/bid-requests/stats)의 상태별 수가 bid_requests 표의 상태별 건수와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Medium'),

dict(id='IT-12-002', flow='대시보드', kind='정상', title='대시보드 최근 입찰 목록에서 입찰 상세로 간다',
     pre='관리자 계정, 입찰 요청 10건 이상, 회원 고객 계정',
     steps='1. 고객 화면에서 회원 계정으로 마감 전 공고 하나에 입찰을 신청한다\n2. 관리자 화면 [대시보드] 를 연다\n'
           '3. 최근 입찰 목록에서 방금 신청한 입찰 건을 누른다',
     expect='1. 최근 입찰 목록에 5~10건이 있고 방금 신청한 입찰 건의 공고 제목이 들어 있다\n'
            '2. 그 입찰 건의 입찰 상세 관리(/bid-requests/{id})가 열려 공고 정보에 같은 공고 제목이 보인다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Medium'),

dict(id='IT-12-003', flow='대시보드', kind='정상', title='대시보드 마감 임박 목록에 D-7 이내 입찰 건만 있다',
     pre='관리자 계정, 마감 5일 남은 입찰 요청 1건과 마감 10일 남은 입찰 요청 1건',
     steps='1. 관리자 화면 [대시보드] 를 연다\n2. 마감 임박 목록에서 마감 5일 남은 입찰 건을 누른다',
     expect='1. 마감 임박 목록에 마감 5일 남은 그 입찰 건이 들어 있다\n'
            '2. 마감 10일 남은 입찰 건은 마감 임박 목록에 없다\n'
            '3. 누른 입찰 건의 입찰 상세 관리(/bid-requests/{id})가 열려 공고 정보에 그 공고 제목이 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Medium',
     issue='화면 소스 대시보드에 마감 임박 영역 문구가 없다'),

dict(id='IT-12-004', flow='대시보드', kind='정상', title='대시보드에서 최근 수집 현황과 문서 생성 상태를 본다',
     pre='관리자 계정, 수집 실행 이력 1건 이상, AI 생성 문서가 있는 입찰 요청 1건 이상',
     steps='1. 관리자 화면 [대시보드] 를 연다\n2. 왼쪽 메뉴 [SAM.gov 수집] 을 열어 마지막 수집 시간을 본다\n'
           '3. AI 생성 문서가 있는 입찰 건의 입찰 상세 관리에서 생성 문서 상태를 본다',
     expect='1. 대시보드에 최근 수집 현황이 있고 그 시각이 SAM.gov 수집 화면의 마지막 수집 시간과 같다\n'
            '2. 대시보드에 문서 생성 상태가 있고 그 값이 입찰 상세 관리의 생성 문서 상태와 같다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Medium',
     issue='T1-1 설명의 최근 수집 현황과 문서 생성 상태가 T3-3 대시보드 영역(통계 카드, 최근 입찰, 마감 임박)에 없다'),

dict(id='IT-12-005', flow='대시보드', kind='예외', title='고객 계정은 관리자 통계를 받지 못한다',
     pre='회원 고객 계정의 로그인 토큰',
     steps='1. 고객 계정 토큰으로 GET /api/admin/bid-requests/stats 를 부른다',
     expect='1. 요청이 권한 없음으로 거부되고 응답에 상태별 통계 값이 없다',
     judge=['서버'],
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='', pri='Medium'),

dict(id='IT-12-006', flow='회원 목록', kind='정상', title='회원 목록 줄마다 회원 여부와 지금 기간의 끝일이 보인다',
     pre='관리자 계정, 오늘을 품은 회원 기간이 있는 고객 A, 회원 기간이 없는 고객 B, 관리자 계정 C',
     steps='1. 관리자 화면 왼쪽 메뉴 [회원 관리] 를 누른다\n2. 고객 A, 고객 B, 관리자 C 의 줄을 찾는다',
     expect='1. 목록 줄마다 이메일, 회사, 담당자, 역할, 가입일, 마지막 로그인, 회원 칸이 있다\n'
            '2. 고객 A 줄 회원 칸이 "회원, YYYY.MM.DD 까지" 꼴로 그 기간의 끝일이다\n'
            '3. 고객 B 줄 회원 칸이 "무료회원" 이다\n'
            '4. 관리자 C 줄 회원 칸이 "관리자" 다\n'
            '5. 회원 목록 응답(GET /api/admin/members)에서 A 는 member true 와 membershipEndsOn 그 끝일, '
            'B 는 member false 와 membershipEndsOn null, C 는 member true 다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-007', flow='회원 목록', kind='정상', title='회원 목록 쪽을 넘긴다',
     pre='관리자 계정, 한 쪽 줄 수보다 많은 회원',
     steps='1. [회원 관리] 를 연다\n2. 목록 아래 [다음] 을 누른다\n3. [이전] 을 누른다',
     expect='1. [다음] 을 누르면 첫 쪽과 겹치지 않는 회원 줄이 보인다\n'
            '2. [이전] 을 누르면 첫 쪽과 같은 회원 줄이 같은 순서로 다시 보인다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-008', flow='회원 기간', kind='정상', title='관리자가 고객 하나에 회원 기간을 넣는다',
     pre='관리자 계정, 회원 기간이 없는 고객 A(무료회원)',
     steps='1. [회원 관리] 에서 고객 A 줄의 [회원 기간] 을 누른다\n'
           '2. 시작일 오늘, 끝일 오늘부터 30일 뒤, 메모 "입금 확인" 을 넣고 [넣기] 를 누른다\n'
           '3. 고객 A 계정으로 고객 화면에 로그인해 내 프로필(/profile)을 연다',
     expect='1. 회원 기간 창 위에 시작일, 끝일, 메모 칸과 [넣기] 가 있고 아래에 기간 이력 표가 있다\n'
            '2. 기간 이력 맨 위에 넣은 시작일, 끝일, 메모 "입금 확인", 넣은 관리자, 넣은 시각이 한 줄로 생긴다\n'
            '3. 새 줄에 지금 기간 표시가 붙는다\n'
            '4. 목록의 고객 A 줄 회원 칸이 "무료회원" 에서 "회원, {30일 뒤 날짜} 까지" 로 바뀐다\n'
            '5. member_memberships 에 고객 A 의 줄이 starts_on 오늘, ends_on 30일 뒤, memo "입금 확인", created_by 그 관리자로 1줄 생긴다\n'
            '6. 고객 A 의 내 프로필에 "회원, {30일 뒤 날짜} 까지" 가 보인다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-009', flow='회원 기간', kind='정상', title='기간 이력이 시작일 늦은 순이고 지금 기간이 표시된다',
     pre='관리자 계정, 고객 A 에 지난 기간(60일 전~30일 전), 지금 기간(오늘을 품음), 다음 기간(40일 뒤~70일 뒤) 세 줄',
     steps='1. [회원 관리] 에서 고객 A 줄의 [회원 기간] 을 누른다',
     expect='1. 기간 이력이 시작일 늦은 순으로 다음 기간, 지금 기간, 지난 기간 순서다\n'
            '2. 오늘을 품은 기간 줄에만 지금 기간 표시가 있다\n'
            '3. 기간 이력 응답(GET /api/admin/members/{memberId}/memberships)이 시작일 늦은 순이고 오늘을 품은 줄만 current true 다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-010', flow='회원 기간', kind='정상', title='기간이 겹치면 가장 늦은 끝일을 지금 기간의 끝일로 보인다',
     pre='관리자 계정, 고객 A 에 오늘을 품은 두 기간(10일 전~10일 뒤, 5일 전~50일 뒤)',
     steps='1. [회원 관리] 에서 고객 A 줄의 회원 칸을 본다',
     expect='1. 고객 A 회원 칸이 "회원, {50일 뒤 날짜} 까지" 다\n'
            '2. 회원 목록 응답의 고객 A membershipEndsOn 이 50일 뒤 날짜다',
     judge=['가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-011', flow='회원 기간', kind='정상', title='시작일과 끝일은 그날을 포함하고 서울 날짜로 가른다',
     pre='관리자 계정, 고객 A 에 시작일 오늘 끝일 오늘인 기간, 고객 B 에 끝일 어제인 기간, 고객 C 에 시작일 내일인 기간',
     steps='1. [회원 관리] 에서 고객 A, B, C 줄의 회원 칸을 본다',
     expect='1. 고객 A 회원 칸이 "회원, {오늘 날짜} 까지" 다\n'
            '2. 고객 B 회원 칸이 "무료회원" 이다\n'
            '3. 고객 C 회원 칸이 "무료회원" 이다\n'
            '4. 회원 목록 응답에서 A 는 member true, B 와 C 는 member false 다\n'
            '5. 서버 시간대가 UTC 여도 서울 날짜로 회원 여부를 가른다',
     judge=['가상', '가상', '가상', '서버', '단위'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-012', flow='회원 기간', kind='정상', title='잘못 넣은 회원 기간을 지운다',
     pre='관리자 계정, 고객 A 에 오늘을 품은 회원 기간 1줄만 있음',
     steps='1. [회원 관리] 에서 고객 A 줄의 [회원 기간] 을 누른다\n2. 그 이력 줄의 [지우기] 를 누른다\n3. 확인 창에서 지우기를 확인한다',
     expect='1. [지우기] 를 누르면 바로 지우지 않고 지울지 묻는 확인 창이 먼저 열린다\n'
            '2. 확인하면 기간 이력에서 그 줄이 빠진다\n'
            '3. 목록의 고객 A 줄 회원 칸이 "무료회원" 으로 바뀐다\n'
            '4. member_memberships 에서 그 줄이 지워진다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-013', flow='회원 기간', kind='예외', title='넣은 회원 기간은 고칠 수 없다',
     pre='관리자 계정, 고객 A 에 회원 기간 1줄',
     steps='1. [회원 관리] 에서 고객 A 줄의 [회원 기간] 을 누른다\n'
           '2. 관리자 토큰으로 PATCH /api/admin/members/{memberId}/memberships/{membershipId} 에 끝일을 바꿔 보낸다',
     expect='1. 기간 이력 줄에 시작일, 끝일, 메모를 고치는 단추나 입력 칸이 없고 [지우기] 만 있다\n'
            '2. member_memberships 의 그 줄 ends_on 이 보내기 전 값 그대로다',
     judge=['가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-014', flow='회원 기간', kind='예외', title='끝일이 시작일보다 앞인 기간은 받지 않는다',
     pre='관리자 계정, 고객 A',
     steps='1. [회원 관리] 에서 고객 A 줄의 [회원 기간] 을 누른다\n'
           '2. 시작일 10일 뒤, 끝일 오늘을 넣고 [넣기] 를 누른다\n'
           '3. 같은 값을 관리자 토큰으로 POST /api/admin/members/{memberId}/memberships 에 보낸다',
     expect='1. "끝일은 시작일과 같거나 뒤여야 합니다" 안내가 보인다\n'
            '2. 기간 이력에 새 줄이 생기지 않는다\n'
            '3. API 응답이 400 MEMBERSHIP_PERIOD_INVALID 이고 member_memberships 에 줄이 늘지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-015', flow='회원 기간', kind='예외', title='없는 회원에게는 회원 기간을 넣지 못한다',
     pre='관리자 계정, 어느 회원에게도 없는 회원 id',
     steps='1. 관리자 토큰으로 POST /api/admin/members/{없는 회원 id}/memberships 에 시작일 오늘, 끝일 30일 뒤를 보낸다',
     expect='1. 응답이 404 MEMBER_NOT_FOUND 다\n2. member_memberships 에 줄이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='', pri='Low'),

dict(id='IT-12-016', flow='회원 기간', kind='예외', title='다른 고객의 회원 기간은 지우지 못한다',
     pre='관리자 계정, 회원 기간이 있는 고객 A, 고객 B',
     steps='1. 관리자 토큰으로 DELETE /api/admin/members/{고객 B id}/memberships/{고객 A 의 기간 id} 를 보낸다',
     expect='1. 응답이 404 MEMBERSHIP_NOT_FOUND 다\n2. member_memberships 에 고객 A 의 그 기간 줄이 그대로 남는다',
     judge=['서버', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='', pri='Low'),

dict(id='IT-12-017', flow='회원 기간', kind='예외', title='고객 계정은 회원 목록과 회원 기간 넣기를 쓰지 못한다',
     pre='회원 고객 계정의 로그인 토큰, 고객 A',
     steps='1. 고객 토큰으로 GET /api/admin/members 를 부른다\n'
           '2. 같은 토큰으로 POST /api/admin/members/{고객 A id}/memberships 에 시작일 오늘, 끝일 30일 뒤를 보낸다',
     expect='1. 회원 목록 요청이 권한 없음으로 거부되고 응답에 회원 목록이 없다\n'
            '2. 회원 기간 넣기 요청이 거부되고 member_memberships 에 줄이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='', pri='Low'),

dict(id='IT-12-018', flow='회원 목록', kind='예외', title='회원 목록을 불러오지 못하면 다시 시도를 보인다',
     pre='관리자 계정, 회원 목록 응답이 실패하도록 막은 상태',
     steps='1. 회원 목록 요청이 실패하는 상태에서 [회원 관리] 를 연다\n2. 요청이 되도록 되돌린 뒤 [다시 시도] 를 누른다',
     expect='1. "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 다시 시도하면 회원 목록에 회원 줄이 이메일과 함께 보인다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Low'),

dict(id='IT-12-019', flow='입찰 요청 목록', kind='정상', title='관리자가 모든 고객의 입찰 요청 목록을 본다',
     pre='관리자 계정, 서로 다른 고객 두 명의 입찰 요청 각 1건 이상',
     steps='1. 관리자 화면 왼쪽 메뉴 [입찰 요청 관리] 를 누른다',
     expect='1. 표 줄마다 고객명, 공고 제목, 상태, 서비스 레벨, 마감일이 있다\n'
            '2. 두 고객의 입찰 요청이 모두 목록에 들어 있다\n'
            '3. 전체 입찰 목록 응답(GET /api/admin/bid-requests)의 totalElements 가 bid_requests 전체 건수와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests', pri='High',
     issue='화면 소스 표 칸은 공고 제목, 기관, 회원, 상태라 T3-3 의 서비스 레벨, 마감일이 없다'),

dict(id='IT-12-020', flow='입찰 요청 목록', kind='정상', title='상태 필터로 입찰 요청을 거른다',
     pre='관리자 계정, 상태 REVIEW 인 입찰 요청 1건 이상과 다른 상태 입찰 요청',
     steps='1. [입찰 요청 관리] 를 연다\n2. 상태 필터 탭에서 REVIEW 를 고른다',
     expect='1. 목록에 상태가 REVIEW 인 입찰 요청만 남는다\n'
            '2. 거른 목록 응답의 totalElements 가 bid_requests 의 REVIEW 건수와 같다',
     judge=['가상', '서버'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests', pri='High'),

dict(id='IT-12-021', flow='입찰 요청 목록', kind='정상', title='고객과 기간으로 입찰 요청을 거른다',
     pre='관리자 계정, 고객 A 와 고객 B 의 입찰 요청, 신청일이 서로 다른 입찰 요청',
     steps='1. [입찰 요청 관리] 를 연다\n2. 고객 필터에 고객 A 를 넣는다\n3. 고객 필터를 풀고 기간 필터에 지난 7일을 넣는다',
     expect='1. 고객 A 로 거르면 고객 A 의 입찰 요청만 남는다\n'
            '2. 지난 7일로 거르면 그 기간에 신청된 입찰 요청만 남는다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests', pri='High',
     issue='T1-1 입력의 고객, 기간 필터가 T3-3 입찰 관리 영역(상태 필터 탭만)과 T3-2 목록 API 설명에 없다'),

dict(id='IT-12-022', flow='입찰 요청 목록', kind='정상', title='입찰 요청 목록 쪽을 넘긴다',
     pre='관리자 계정, 한 쪽 줄 수보다 많은 입찰 요청',
     steps='1. [입찰 요청 관리] 를 연다\n2. 목록 아래 [다음] 을 누른다',
     expect='1. 페이지 표시에 현재 페이지와 전체 페이지가 숫자로 있다\n'
            '2. [다음] 을 누르면 첫 쪽과 겹치지 않는 입찰 요청 줄이 보인다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests', pri='High'),

dict(id='IT-12-023', flow='입찰 상세 관리', kind='정상', title='입찰 요청 줄을 눌러 입찰 상세 관리를 연다',
     pre='관리자 계정, 고객이 제출 문서 1건을 올린 상태 DOCS_RECEIVED 입찰 요청',
     steps='1. [입찰 요청 관리] 에서 그 입찰 요청 줄을 누른다',
     expect='1. 입찰 상세 관리(/bid-requests/{id})가 열려 공고 정보에 그 공고의 제목, 마감일, 요구사항 수가 보인다\n'
            '2. 고객 정보에 신청 고객의 회사명과 연락처가 보인다\n'
            '3. 상태 패널의 현재 상태가 DOCS_RECEIVED 다\n'
            '4. 제출 문서 목록에 고객이 올린 파일 이름이 보인다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-024', flow='입찰 상세 관리', kind='정상', title='입찰 상세 관리에서 고객 제출 문서를 내려받는다',
     pre='관리자 계정, 고객이 제출 문서 1건을 올린 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 제출 문서 목록에서 그 파일을 내려받는다',
     expect='1. 내려받은 파일의 이름, 크기, 내용이 고객이 올린 파일과 같다',
     judge=['서버'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-025', flow='입찰 상세 관리', kind='정상', title='REVIEW 상태 입찰 건에서 생성 문서를 열어 문서 편집으로 간다',
     pre='관리자 계정, AI 생성 문서가 1건 이상 있는 상태 REVIEW 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 생성 문서 목록에서 문서 편집 링크를 누른다',
     expect='1. 생성 문서 목록 줄마다 AI 생성 문서와 그 문서 상태가 있다\n'
            '2. REVIEW 상태라 문서 편집 링크가 켜져 있다\n'
            '3. 누르면 그 문서의 문서 편집(/bid-requests/{id}/documents/{docId})이 열려 그 문서 본문이 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='화면 소스의 문서 편집 주소는 /documents/{id}/edit 로 T3-3 의 /bid-requests/{id}/documents/{docId} 와 다르다'),

dict(id='IT-12-026', flow='입찰 상세 관리', kind='정상', title='CREATED 입찰 건에 담당자를 배정한다',
     pre='관리자 계정 2개(관리자 1, 관리자 2), 상태 CREATED 입찰 요청',
     steps='1. 관리자 1 로 그 입찰 요청의 입찰 상세 관리를 연다\n2. 담당자 배정에서 관리자 2 를 고른다\n'
           '3. 관리자 2 토큰으로 GET /api/admin/bid-requests/assigned-to-me 를 부른다',
     expect='1. CREATED 상태라 담당자 배정이 켜져 있다\n'
            '2. bid_requests 의 그 줄 assigned_to 가 관리자 2 의 id 로 바뀐다\n'
            '3. 관리자 2 의 내 담당 입찰 목록 응답에 그 입찰 요청이 들어 있다',
     judge=['가상', '서버', '서버'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='화면 소스 입찰 상세에 담당자 배정 조작이 보이지 않는다'),

dict(id='IT-12-027', flow='입찰 상세 관리', kind='정상', title='ANALYZING 이후 상태에서는 담당자 배정이 꺼진다',
     pre='관리자 계정, 상태 ANALYZING 입찰 요청과 상태 REVIEW 입찰 요청',
     steps='1. ANALYZING 입찰 요청의 입찰 상세 관리를 연다\n2. REVIEW 입찰 요청의 입찰 상세 관리를 연다',
     expect='1. ANALYZING 건에서 담당자 배정이 꺼져 있어 고를 수 없다\n'
            '2. REVIEW 건에서 담당자 배정이 꺼져 있어 고를 수 없다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-028', flow='입찰 상세 관리', kind='예외', title='없는 입찰 건 주소를 열면 목록으로 돌아간다',
     pre='관리자 계정, 어느 입찰 요청에도 없는 id',
     steps='1. 관리자 화면 주소창에 /bid-requests/{없는 id} 를 넣는다',
     expect='1. 입찰 상세가 열리지 않고 입찰 요청 관리 목록(/bid-requests)으로 돌아간다',
     judge=['가상'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-029', flow='입찰 요청 목록', kind='예외', title='고객 계정은 관리자 입찰 요청 목록을 받지 못한다',
     pre='회원 고객 A 계정의 로그인 토큰, 다른 고객 B 의 입찰 요청',
     steps='1. 고객 A 토큰으로 GET /api/admin/bid-requests 를 부른다',
     expect='1. 요청이 권한 없음으로 거부되고 응답에 고객 B 의 입찰 요청이 없다',
     judge=['서버'],
     req=['BID-ADMIN-003'], fc=['BID-ADMIN-003'], screen='', pri='High'),

dict(id='IT-12-030', flow='상태 전이', kind='정상', title='관리자가 입찰 상세에서 CREATED 를 DOCS_PENDING 으로 전이한다',
     pre='관리자 계정, 상태 CREATED 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 상태 패널에서 다음 상태 DOCS_PENDING 을 고른다\n'
           '3. 노트에 "필요 문서 안내 완료" 를 넣고 전이를 실행한다',
     expect='1. 다음 상태를 고르면 노트를 넣는 칸이 있는 창이 열린다\n'
            '2. 전이를 실행하면 상태 패널의 현재 상태가 DOCS_PENDING 으로 바뀐다\n'
            '3. 상태가 바뀌었다는 토스트 알림이 뜬다\n'
            '4. 상태 이력에 CREATED 에서 DOCS_PENDING 으로 바뀐 줄이 노트 "필요 문서 안내 완료" 와 함께 더해진다\n'
            '5. bid_requests 의 state 가 DOCS_PENDING 이고 state_history 에 fromState CREATED, toState DOCS_PENDING, '
            'userId 그 관리자, notes "필요 문서 안내 완료", automatic false 인 줄이 1개 더해진다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='화면 소스는 허용 상태마다 단추를 두지 않고 [상태 전환] 창에서 "전환할 상태" 를 골라 [전환] 을 누른다'),

dict(id='IT-12-031', flow='상태 전이', kind='정상', title='입찰 요청 관리 목록에서 DOCS_RECEIVED 를 ANALYZING 으로 전이한다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 요청',
     steps='1. [입찰 요청 관리] 목록에서 그 입찰 요청의 상태 전이를 연다\n2. ANALYZING 을 고르고 전이를 실행한다\n'
           '3. 그 입찰 요청의 입찰 상세 관리를 연다\n4. 10분 기다린 뒤 상태를 다시 본다',
     expect='1. 목록 줄의 상태가 ANALYZING 으로 바뀐다\n'
            '2. 입찰 상세 관리 상태 패널의 현재 상태가 ANALYZING 이다\n'
            '3. state_history 에 DOCS_RECEIVED 에서 ANALYZING 으로의 줄이 더해진다\n'
            '4. ANALYZING 진입 때 고객 서류 읽기만 돌고 상태는 바꾸지 않아 10분 뒤에도 state 가 ANALYZING 그대로다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests', pri='High',
     probe={3: dict(steps=[('key', '12.b031_server3_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-12-031 서버 판정'}),
                           ('sql', LAST_STEP)],
                    check="r[1]['status'] == 200 and int(r[2][0]['hist']) == int(r[0][0]['hist']) + 1 "
                          "and r[2][0]['from_state'] == 'DOCS_RECEIVED' and r[2][0]['to_state'] == 'ANALYZING'"),
            4: dict(steps=[('key', '12.b031_server4_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-12-031 서버 판정'}),
                           ('poll', ADMIN, '/api/admin/bid-requests/{val:0:id}', "x['body']['state'] != 'ANALYZING'", 600, 30),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 200 and r[3][0]['state'] == 'ANALYZING'")}),

dict(id='IT-12-032', flow='상태 전이', kind='정상', title='필수 요구 항목을 모두 채운 입찰 건을 DOCS_RECEIVED 로 전이한다',
     pre='관리자 계정, 상태 DOCS_PENDING 이고 공고의 필수 고객 올림 요구 항목을 모두 올린 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 DOCS_RECEIVED 로 전이를 실행한다',
     expect='1. 상태 패널의 현재 상태가 DOCS_RECEIVED 로 바뀐다\n'
            '2. state 가 DOCS_RECEIVED 이고 state_history 에 DOCS_PENDING 에서 DOCS_RECEIVED 로의 줄이 더해진다',
     judge=['가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-033', flow='상태 전이', kind='정상', title='요구 항목이 없는 공고의 입찰 건은 DOCS_RECEIVED 로 바로 넘어간다',
     pre='관리자 계정, 공고 분석에서 요구 항목이 하나도 없는 공고의 상태 DOCS_PENDING 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 DOCS_RECEIVED 로 전이를 실행한다',
     expect='1. 상태 패널의 현재 상태가 DOCS_RECEIVED 로 바뀐다\n'
            '2. state 가 DOCS_RECEIVED 이고 state_history 에 줄이 1개 더해진다',
     judge=['가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-034', flow='상태 전이', kind='정상', title='ANALYZING 을 GENERATING 으로 전이하면 AI 문서 생성이 시작된다',
     pre='관리자 계정, 상태 ANALYZING 이고 공고 사전 분석이 COMPLETED 이며 고객 서류 읽기가 끝나 읽는 중인 서류가 없고 입찰 분석의 관리자 결정이 Go 인 입찰 요청, Aimbase 에 닿는 배포 환경',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 GENERATING 으로 전이를 실행한다\n3. AI 문서 생성이 끝날 때까지 기다린다',
     expect='1. 상태 패널의 현재 상태가 GENERATING 으로 바뀐다\n'
            '2. state 가 GENERATING 이고 state_history 에 ANALYZING 에서 GENERATING 으로의 줄이 더해진다\n'
            '3. 생성이 끝나면 상태가 REVIEW 로 바뀌고 생성 문서 목록에 AI 가 그 공고에 맞춰 쓴 제안서가 생긴다',
     judge=['가상', '서버', '합동'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '12.b034_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-12-034 서버 판정'}),
                           ('sql', LAST_STEP)],
                    check="r[1]['status'] == 200 and r[2][0]['state'] == 'GENERATING' and int(r[2][0]['hist']) == int(r[0][0]['hist']) + 1 "
                          "and r[2][0]['from_state'] == 'ANALYZING' and r[2][0]['to_state'] == 'GENERATING'")}),

dict(id='IT-12-035', flow='상태 전이', kind='정상', title='REVIEW 를 CONFIRMED 로 전이하면 문서가 잠긴다',
     pre='관리자 계정, 상태 REVIEW 이고 생성 문서가 DRAFT 인 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 CONFIRMED 로 전이를 실행한다',
     expect='1. 상태 패널의 현재 상태가 CONFIRMED 로 바뀐다\n'
            '2. 그 입찰 요청의 bid_documents status 가 LOCKED 로 바뀐다\n'
            '3. state_history 에 REVIEW 에서 CONFIRMED 로의 줄이 더해진다',
     judge=['가상', '서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-036', flow='상태 전이', kind='정상', title='REVIEW 에서 GENERATING, CONFIRMED 에서 REVIEW 로 되돌린다',
     pre='관리자 계정, 상태 REVIEW 입찰 요청 1건, 상태 CONFIRMED 입찰 요청 1건',
     steps='1. REVIEW 입찰 요청의 입찰 상세 관리에서 GENERATING 으로 전이를 실행한다\n'
           '2. CONFIRMED 입찰 요청의 입찰 상세 관리에서 REVIEW 로 전이를 실행한다',
     expect='1. REVIEW 였던 건의 현재 상태가 GENERATING 으로 바뀐다\n'
            '2. CONFIRMED 였던 건의 현재 상태가 REVIEW 로 바뀐다\n'
            '3. 두 건의 state_history 에 REVIEW 에서 GENERATING, CONFIRMED 에서 REVIEW 줄이 각각 더해진다',
     judge=['가상', '가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-037', flow='상태 전이', kind='정상', title='CONFIRMED 를 SUBMITTED 로 전이하면 제출 시각이 남는다',
     pre='관리자 계정, 상태 CONFIRMED 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 SUBMITTED 로 전이를 실행한다',
     expect='1. 상태 패널의 현재 상태가 SUBMITTED 로 바뀐다\n'
            '2. bid_requests 의 submitted_at 이 전이한 시각으로 채워진다\n'
            '3. state_history 에 CONFIRMED 에서 SUBMITTED 로의 줄이 더해진다',
     judge=['가상', '서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-038', flow='상태 전이', kind='정상', title='SUBMITTED 입찰 건을 낙찰과 탈락으로 전이한다',
     pre='관리자 계정, 상태 SUBMITTED 입찰 요청 2건',
     steps='1. 첫 입찰 요청의 입찰 상세 관리에서 AWARDED 로 전이를 실행한다\n'
           '2. 두 번째 입찰 요청의 입찰 상세 관리에서 NOT_AWARDED 로 전이를 실행한다',
     expect='1. 첫 건의 현재 상태가 AWARDED 로 바뀐다\n'
            '2. 두 번째 건의 현재 상태가 NOT_AWARDED 로 바뀐다\n'
            '3. 두 건의 state 가 AWARDED, NOT_AWARDED 이고 state_history 에 SUBMITTED 에서 각 상태로의 줄이 더해진다',
     judge=['가상', '가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='T3-4 상태별 UI 분기와 T3-1 BidRequestState 는 SUBMITTED 를 최종 상태로 적고 AWARDED, NOT_AWARDED 가 없다'),

dict(id='IT-12-039', flow='상태 전이', kind='정상', title='CREATED, DOCS_PENDING, DOCS_RECEIVED 입찰 건을 CLOSED 로 종료한다',
     pre='관리자 계정, 상태 CREATED, DOCS_PENDING, DOCS_RECEIVED 입찰 요청 각 1건',
     steps='1. 세 입찰 요청마다 입찰 상세 관리를 열어 노트 "고객 요청 취소" 로 CLOSED 전이를 실행한다',
     expect='1. 세 건 모두 상태 패널의 현재 상태가 CLOSED 로 바뀐다\n'
            '2. 세 건의 bid_requests closed_at 이 전이한 시각으로 채워진다\n'
            '3. 세 건의 state_history 에 각 상태에서 CLOSED 로의 줄이 노트 "고객 요청 취소" 와 함께 더해진다',
     judge=['가상', '서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-040', flow='상태 전이', kind='예외', title='허용 목록에 없는 전이는 거부된다',
     pre='관리자 계정, 상태 ANALYZING 입찰 요청 1건, 상태 SUBMITTED 입찰 요청 1건',
     steps='1. 관리자 토큰으로 POST /api/admin/bid-requests/{ANALYZING 건 id}/transition 에 목표 상태 REVIEW 를 보낸다\n'
           '2. 같은 방법으로 SUBMITTED 건에 목표 상태 REVIEW 를 보낸다',
     expect='1. 두 요청 모두 실패 응답으로 거부된다\n'
            '2. 두 건의 state 가 ANALYZING, SUBMITTED 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='', pri='High'),

dict(id='IT-12-041', flow='상태 전이', kind='예외', title='다른 창에서 종료된 건을 전이하면 불가 안내가 뜬다',
     pre='관리자 계정, 상태 CREATED 입찰 요청, 관리자 화면 창 두 개',
     steps='1. 창 1 에서 그 입찰 요청의 입찰 상세 관리를 열어 둔다\n2. 창 2 에서 같은 건을 CLOSED 로 전이한다\n'
           '3. 창 1 에서 새로고침 없이 DOCS_PENDING 으로 전이를 실행한다',
     expect='1. 창 1 에 "현재 상태에서 해당 전이가 불가합니다" 토스트가 뜬다\n'
            '2. state 가 CLOSED 그대로이고 state_history 에 CLOSED 뒤의 줄이 더해지지 않는다',
     judge=['가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-042', flow='상태 전이', kind='예외', title='필수 요구 항목이 빠지면 DOCS_RECEIVED 전이가 막힌다',
     pre='관리자 계정, 상태 DOCS_PENDING 이고 필수 고객 올림 요구 항목 1개를 올리지 않은 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 DOCS_RECEIVED 로 전이를 실행한다',
     expect='1. 상태 패널의 현재 상태가 DOCS_PENDING 그대로다\n'
            '2. 전이 응답에 올리지 않은 그 요구 항목이 미충족 슬롯 목록으로 들어 있다\n'
            '3. state 가 DOCS_PENDING 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-12-043', flow='상태 전이', kind='예외', title='BLOCKER 요구사항이 미충족이면 CONFIRMED 전이가 막힌다',
     pre='관리자 계정, 상태 REVIEW 이고 BLOCKER 요구사항이 미충족인 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 다음 상태 CONFIRMED 로 전이를 실행한다',
     expect='1. "필수 요구사항이 미충족 상태입니다" 토스트가 뜬다\n'
            '2. state 가 REVIEW 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['가상', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='BLOCKER 가드 자리가 T3-4 는 CONFIRMED 전이, BIZ-007 은 없는 상태 READY_FOR_SUBMISSION 전이, T1-5 는 DOCS_RECEIVED 전이로 서로 다르다'),

dict(id='IT-12-044', flow='상태 전이', kind='예외', title='SUBMITTED 가 아닌 건은 낙찰로 전이하지 못한다',
     pre='관리자 계정, 상태 CONFIRMED 입찰 요청',
     steps='1. 관리자 토큰으로 POST /api/admin/bid-requests/{id}/transition 에 목표 상태 AWARDED 를 보낸다',
     expect='1. 요청이 실패 응답으로 거부되고 state 가 CONFIRMED 그대로다\n'
            '2. state_history 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='', pri='High'),

dict(id='IT-12-045', flow='상태 전이', kind='예외', title='고객은 자기 입찰 건을 취소 말고는 전이하지 못한다',
     pre='회원 고객 계정, 그 고객의 상태 CREATED 입찰 요청',
     steps='1. 고객 토큰으로 PATCH /api/bid-requests/{id}/state 에 목표 상태 DOCS_PENDING 을 보낸다',
     expect='1. 응답이 403 이다\n2. state 가 CREATED 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-FSM-001'], fc=['BID-FSM-001'], screen='', pri='High'),

dict(id='IT-12-046', flow='상태 이력', kind='정상', title='입찰 상세 관리에서 상태 변경 타임라인을 본다',
     pre='관리자 계정, CREATED 에서 DOCS_PENDING, DOCS_PENDING 에서 DOCS_RECEIVED 로 노트를 넣어 두 번 전이한 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 상태 이력 영역을 본다\n3. 관리자 토큰으로 GET /api/bid-requests/{id}/history 를 부른다',
     expect='1. 상태 이력에 CREATED 에서 DOCS_PENDING, DOCS_PENDING 에서 DOCS_RECEIVED 두 줄이 있다\n'
            '2. 줄마다 전이 시각, 변경자, 이전 상태, 다음 상태, 메모가 있다\n'
            '3. 이력 응답의 줄이 bid_requests state_history 와 같은 수, 같은 값이다',
     judge=['가상', '가상', '서버'],
     req=['BID-FSM-002'], fc=['BID-FSM-002'], screen='관리자 /bid-requests/{id}', pri='Medium',
     issue='화면 소스 입찰 상세에 상태 이력 영역 문구가 보이지 않는다'),

dict(id='IT-12-047', flow='상태 이력', kind='예외', title='상태 이력을 불러오지 못하면 이력만 비운다',
     pre='관리자 계정, 전이 이력이 있는 입찰 요청, 이력 응답이 실패하도록 막은 상태',
     steps='1. 이력 요청이 실패하는 상태에서 그 입찰 요청의 입찰 상세 관리를 연다',
     expect='1. 상태 이력 영역의 이력 줄이 0개다\n'
            '2. 상태 패널에는 그 건의 현재 상태가 보이고 공고 정보에 그 공고 제목이 보인다',
     judge=['가상', '가상'],
     req=['BID-FSM-002'], fc=['BID-FSM-002'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-12-048', flow='상태 이력', kind='예외', title='다른 고객의 입찰 건 상태 이력은 받지 못한다',
     pre='회원 고객 A 계정의 로그인 토큰, 고객 B 의 입찰 요청',
     steps='1. 고객 A 토큰으로 GET /api/bid-requests/{고객 B 건 id}/history 를 부른다',
     expect='1. 응답이 404 "입찰 건을 찾을 수 없습니다" 다\n2. 응답에 고객 B 건의 상태 이력이 없다',
     judge=['서버', '서버'],
     req=['BID-FSM-002'], fc=['BID-FSM-002'], screen='', pri='Medium'),

dict(id='IT-12-049', flow='다음 가능 상태', kind='정상', title='CREATED 입찰 건의 허용 다음 상태를 본다',
     pre='관리자 계정, 상태 CREATED 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. 상태 패널의 허용 다음 상태가 DOCS_PENDING, CLOSED 둘이다\n'
            '2. 문서 편집 링크가 꺼져 있다\n'
            '3. 다음 상태 응답이 DOCS_PENDING, CLOSED 둘이다',
     judge=['가상', '가상', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-12-050', flow='다음 가능 상태', kind='정상', title='REVIEW 입찰 건의 허용 다음 상태와 문서 편집 링크를 본다',
     pre='관리자 계정, 상태 REVIEW 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. 상태 패널의 허용 다음 상태가 CONFIRMED, GENERATING 둘이다\n'
            '2. 문서 편집 링크가 켜져 있다\n'
            '3. 다음 상태 응답이 CONFIRMED, GENERATING 둘이다',
     judge=['가상', '가상', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-12-051', flow='다음 가능 상태', kind='정상', title='ANALYZING 입찰 건의 허용 다음 상태는 GENERATING, CLOSED 둘이다',
     pre='관리자 계정, 상태 ANALYZING 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. 상태 패널의 허용 다음 상태가 GENERATING, CLOSED 둘이다\n'
            '2. 다음 상태 응답이 GENERATING, CLOSED 다',
     judge=['가상', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('sql', "SELECT LOWER(CONCAT_WS('-', SUBSTR(HEX(id),1,8), SUBSTR(HEX(id),9,4), SUBSTR(HEX(id),13,4), "
                                  "SUBSTR(HEX(id),17,4), SUBSTR(HEX(id),21,12))) AS id FROM bid_requests "
                                  "WHERE state = 'ANALYZING' ORDER BY updated_at DESC LIMIT 1"),
                           ('api', ADMIN, 'GET', '/api/bid-requests/{val:0:id}/next-states')],
                    need="len(r[0]) == 1",
                    check="r[1]['status'] == 200 and sorted(r[1]['body']) == ['CLOSED', 'GENERATING']")}),

dict(id='IT-12-052', flow='다음 가능 상태', kind='정상', title='나머지 진행 상태의 허용 다음 상태가 설계표와 같다',
     pre='관리자 계정, 상태 DOCS_PENDING, DOCS_RECEIVED, GENERATING, CONFIRMED 입찰 요청 각 1건',
     steps='1. 네 입찰 요청마다 관리자 토큰으로 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. DOCS_PENDING 건의 응답이 DOCS_RECEIVED, CLOSED 다\n'
            '2. DOCS_RECEIVED 건의 응답이 ANALYZING, CLOSED 다\n'
            '3. GENERATING 건의 응답이 REVIEW 하나다\n'
            '4. CONFIRMED 건의 응답이 SUBMITTED, REVIEW 다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='', pri='Medium'),

dict(id='IT-12-053', flow='다음 가능 상태', kind='정상', title='SUBMITTED 입찰 건의 허용 다음 상태는 낙찰과 탈락이다',
     pre='관리자 계정, 상태 SUBMITTED 입찰 요청',
     steps='1. 그 입찰 요청의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. 상태 패널의 허용 다음 상태가 AWARDED, NOT_AWARDED 둘이다\n'
            '2. 다음 상태 응답이 AWARDED, NOT_AWARDED 둘이다',
     judge=['가상', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium',
     issue='T1-5 는 SUBMITTED 다음에 AWARDED, NOT_AWARDED 를 두고 T3-4 상태별 UI 분기는 SUBMITTED 를 최종으로 적는다'),

dict(id='IT-12-054', flow='다음 가능 상태', kind='정상', title='CLOSED, AWARDED, NOT_AWARDED 입찰 건에는 허용 다음 상태가 없다',
     pre='관리자 계정, 상태 CLOSED, AWARDED, NOT_AWARDED 입찰 요청 각 1건',
     steps='1. CLOSED 입찰 요청의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 세 건 각각 GET /api/bid-requests/{id}/next-states 를 부른다',
     expect='1. CLOSED 건 상태 패널에 전이를 실행할 다음 상태가 0개다\n'
            '2. CLOSED 건의 다음 상태 응답이 빈 목록이다\n'
            '3. AWARDED 건과 NOT_AWARDED 건의 다음 상태 응답도 빈 목록이다',
     judge=['가상', '서버', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-12-055', flow='다음 가능 상태', kind='예외', title='다음 상태를 불러오지 못하면 전이 단추가 꺼진다',
     pre='관리자 계정, 상태 CREATED 입찰 요청, 다음 상태 응답이 실패하도록 막은 상태',
     steps='1. 다음 상태 요청이 실패하는 상태에서 그 입찰 요청의 입찰 상세 관리를 연다',
     expect='1. 상태 패널의 상태 전이 단추가 꺼져 있어 누를 수 없다',
     judge=['가상'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='관리자 /bid-requests/{id}', pri='Medium',
     issue='화면 소스는 다음 상태를 서버에서 받지 않고 화면 안의 표로 정한다'),

dict(id='IT-12-056', flow='다음 가능 상태', kind='예외', title='다른 고객의 입찰 건 다음 상태는 받지 못한다',
     pre='회원 고객 A 계정의 로그인 토큰, 고객 B 의 입찰 요청',
     steps='1. 고객 A 토큰으로 GET /api/bid-requests/{고객 B 건 id}/next-states 를 부른다',
     expect='1. 응답이 404 "입찰 건을 찾을 수 없습니다" 다\n2. 응답에 고객 B 건의 다음 상태가 없다',
     judge=['서버', '서버'],
     req=['BID-FSM-003'], fc=['BID-FSM-003'], screen='', pri='Medium'),

dict(id='IT-12-057', flow='공고 관리', kind='정상', title='관리자 공고 목록에서 첨부파일 수, 분석 상태, 노출 상태를 본다',
     pre='관리자 계정, 첨부가 있는 공고와 첨부가 없는 공고',
     steps='1. 관리자 화면 왼쪽 메뉴 [원본 공고] 를 누른다\n2. 관리자 토큰으로 GET /api/admin/opportunities 를 부른다',
     expect='1. 목록 줄마다 첨부파일 수, 분석 상태, 노출 상태가 있다\n'
            '2. 첨부가 없는 공고 줄의 첨부파일 수가 0 이다\n'
            '3. 공고 목록 응답 줄마다 첨부파일 수, 분석 상태, 노출 상태 값이 있고 DB 의 그 공고 값과 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='관리자 /opportunities', pri='High',
     issue='T3-3 에 관리자 공고 관리 화면 절이 없다. 화면 소스 원본 공고 목록 칸은 공고(원문), 공고유형, 공고일, 마감일, 첨부, 생성여부로 분석 상태와 노출 상태 칸이 없다'),

dict(id='IT-12-058', flow='공고 관리', kind='정상', title='첨부파일 유무로 공고를 거른다',
     pre='관리자 계정, 첨부가 있는 공고와 첨부가 없는 공고',
     steps='1. [원본 공고] 를 연다\n2. 첨부 필터에서 첨부 있음을 고른다\n3. 첨부 필터에서 첨부 없음을 고른다',
     expect='1. 첨부 있음으로 거르면 첨부파일 수가 1 이상인 공고만 남는다\n'
            '2. 첨부 없음으로 거르면 첨부파일 수가 0 인 공고만 남는다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='관리자 /opportunities', pri='High'),

dict(id='IT-12-059', flow='공고 관리', kind='정상', title='분석 상태와 노출 상태로 공고를 거른다',
     pre='관리자 계정, 분석 상태와 노출 상태가 서로 다른 공고 여러 건',
     steps='1. [원본 공고] 를 연다\n2. 분석 상태 필터에서 COMPLETED 를 고른다\n3. 분석 상태 필터를 풀고 노출 상태 필터에서 VISIBLE 을 고른다',
     expect='1. 분석 상태 COMPLETED 로 거르면 분석 상태가 COMPLETED 인 공고만 남는다\n'
            '2. 노출 상태 VISIBLE 로 거르면 노출 상태가 VISIBLE 인 공고만 남는다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='관리자 /opportunities', pri='High',
     issue='화면 소스에는 분석 상태, 노출 상태 필터가 없고 공고유형, 첨부, 생성여부 필터만 있다'),

dict(id='IT-12-060', flow='공고 관리', kind='정상', title='첨부파일 없는 공고에 첨부를 수동으로 올린다',
     pre='관리자 계정, 첨부파일이 없는 공고',
     steps='1. [원본 공고] 에서 그 공고를 눌러 상세(/opportunities/{id})를 연다\n2. [수동 업로드] 로 PDF 파일 1개를 올린다\n'
           '3. [원본 공고] 목록으로 돌아간다',
     expect='1. 상세 첨부파일 목록에 올린 파일 이름이 생긴다\n'
            '2. 목록의 그 공고 줄 첨부파일 수가 0 에서 1 로 바뀐다\n'
            '3. 그 공고의 첨부 줄이 downloadStatus SUCCESS 로 1줄 생긴다\n'
            '4. 그 공고의 사전 분석 상태가 PENDING 이 된다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-ADMIN-004', 'BID-OPP-007'], fc=['BID-ADMIN-004', 'BID-OPP-007'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-12-061', flow='공고 관리', kind='정상', title='가져와야 함 표식이 있는 첨부를 수동으로 올리면 그 줄이 채워진다',
     pre='관리자 계정, 첨부 중 MANUAL_FETCH_REQUIRED 줄이 1개 있는 공고, 그 첨부를 외부에서 받아 둔 파일',
     steps='1. 그 공고 상세(/opportunities/{id})를 연다\n2. [수동 업로드] 로 받아 둔 파일을 올린다',
     expect='1. 첨부 줄 수가 늘지 않고 그 MANUAL_FETCH_REQUIRED 줄이 downloadStatus SUCCESS 로 바뀐다\n'
            '2. 그 첨부 줄의 "가져와야 함" 표식이 사라진다',
     judge=['서버', '가상'],
     req=['BID-ADMIN-004', 'BID-OPP-007'], fc=['BID-ADMIN-004', 'BID-OPP-007'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-12-062', flow='공고 관리', kind='정상', title='공고 상세에서 사전 분석 결과를 확인한다',
     pre='관리자 계정, 사전 분석이 COMPLETED 인 공고',
     steps='1. [원본 공고] 에서 그 공고를 눌러 상세를 연다\n2. 관리자 토큰으로 GET /api/admin/opportunities/{id}/analysis 를 부른다',
     expect='1. 분석 상태가 COMPLETED 로 보인다\n'
            '2. 사전 분석 결과에 요약·양식·필요서류가 보인다\n'
            '3. 화면의 요약, 양식, 필요서류가 분석 결과 응답의 값과 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-12-063', flow='공고 관리', kind='정상', title='사전 분석 결과를 관리자가 보정한다',
     pre='관리자 계정, 사전 분석이 COMPLETED 인 공고',
     steps='1. 그 공고 상세에서 분석 결과의 요약 끝에 "관리자 보정" 을 더해 저장한다\n2. 상세를 새로고침한다',
     expect='1. 새로고침해도 요약 끝에 "관리자 보정" 이 보인다\n'
            '2. 그 공고 사전 분석 결과의 요약이 "관리자 보정" 을 더한 값으로 저장된다',
     judge=['가상', '서버'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-12-064', flow='공고 관리', kind='정상', title='분석이 끝난 공고를 노출 승인하고 다시 해제한다',
     pre='관리자 계정, 사전 분석이 COMPLETED 이고 노출 상태 HIDDEN 인 공고',
     steps='1. 그 공고 상세에서 노출 승인을 한다\n2. 목록에서 그 공고의 노출 상태를 본다\n3. 같은 공고를 노출 해제한다',
     expect='1. 승인 뒤 목록의 그 공고 노출 상태가 VISIBLE 이다\n'
            '2. 승인 뒤 DB 의 그 공고 노출 상태가 VISIBLE 이다\n'
            '3. 해제 뒤 목록의 노출 상태가 HIDDEN 으로 바뀐다\n'
            '4. 해제 뒤 DB 의 노출 상태가 HIDDEN 이다',
     judge=['가상', '서버', '가상', '서버'],
     req=['BID-ADMIN-004', 'BID-OPP-008'], fc=['BID-ADMIN-004', 'BID-OPP-008'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-12-065', flow='공고 관리', kind='예외', title='분석이 끝나지 않은 공고는 노출 승인되지 않는다',
     pre='관리자 계정, 사전 분석이 PENDING 이고 노출 상태 HIDDEN 인 공고',
     steps='1. 관리자 토큰으로 POST /api/admin/opportunities/{id}/approve 를 보낸다',
     expect='1. 요청이 실패 응답으로 거부된다\n2. 그 공고 노출 상태가 HIDDEN 그대로다',
     judge=['서버', '서버'],
     req=['BID-ADMIN-004', 'BID-OPP-008'], fc=['BID-ADMIN-004', 'BID-OPP-008'], screen='', pri='High'),

dict(id='IT-12-066', flow='공고 관리', kind='예외', title='고객 계정은 관리자 공고 목록을 받지 못한다',
     pre='회원 고객 계정의 로그인 토큰',
     steps='1. 고객 토큰으로 GET /api/admin/opportunities 를 부른다',
     expect='1. 요청이 권한 없음으로 거부되고 응답에 공고 목록이 없다',
     judge=['서버'],
     req=['BID-ADMIN-004'], fc=['BID-ADMIN-004'], screen='', pri='High'),

dict(id='IT-12-067', flow='관리자 공통 틀', kind='정상', title='화면 이름은 위 머리줄에만 한 번 나오고 본문 제목과 설명 문구가 없다',
     pre='관리자 계정, 입찰 요청 1건 이상',
     steps='1. 관리자 화면에 로그인해 왼쪽 메뉴 [대시보드] 를 누른다\n2. 왼쪽 메뉴 [입찰 요청 관리] 를 누른다\n'
           '3. 표의 한 줄을 눌러 입찰 상세로 들어간다',
     expect='1. 위 머리줄에 "대시보드" 가 한 번 보이고 "Admin Console" 글자가 없다\n'
            '2. 대시보드 본문에 "대시보드" 제목과 "입찰 요청 현황을 한눈에 확인합니다." 문구가 없다\n'
            '3. 입찰 요청 관리 본문과 카드 머리에 "입찰 요청 관리" 제목과 "입찰 요청의 상태를 조회하고 전환합니다." 문구가 없다\n'
            '4. 입찰 상세의 위 머리줄에 "입찰 요청 관리" 가 보이고, 본문 머리에 그 건의 공고 제목, 상태 배지, 목록으로 돌아가기가 있다',
     judge=['가상', '가상', '가상', '가상'],
     pair={1: [1], 2: [1], 3: [2], 4: [3]},
     req=['BID-ADMIN-001', 'BID-ADMIN-003'], fc=['BID-ADMIN-001', 'BID-ADMIN-003'], screen='관리자 /', pri='Medium'),

dict(id='IT-12-068', flow='관리자 공통 틀', kind='정상', title='목록 화면이 카드 하나에 상태 탭, 표, 쪽 넘김으로 보이고 상태 배지 색이 상태를 가른다',
     pre='관리자 계정, 입찰 요청 21건 이상(신청 접수, 문서 대기, 분석 중, 제출 완료, 탈락 상태 포함)',
     steps='1. 왼쪽 메뉴 [입찰 요청 관리] 를 연다\n2. 왼쪽 메뉴 [회원 관리], [원본 공고], [공고문 관리], [기지 관리] 를 차례로 연다\n'
           '3. 입찰 요청 관리로 돌아와 상태 배지를 본다',
     expect='1. 입찰 요청 관리 본문이 카드 하나이고 카드 머리에 상태 탭, 카드 몸에 표, 카드 바닥에 쪽 넘김이 보인다\n'
            '2. 다른 네 목록 화면도 같은 카드 틀이고 표 머리 글자, 줄 높이, 선 색이 입찰 요청 관리와 같다\n'
            '3. 상태 배지가 신청 접수는 회색, 문서 대기는 주황, 분석 중은 파랑, 제출 완료는 초록, 탈락은 빨강이다',
     judge=['합동', '합동', '합동'],
     pair={1: [1], 2: [2], 3: [3]},
     req=['BID-ADMIN-003', 'BID-ADMIN-002', 'BID-ADMIN-004'], fc=['BID-ADMIN-003', 'BID-ADMIN-002', 'BID-ADMIN-004'], screen='관리자 /bid-requests', pri='Medium'),

dict(id='IT-12-069', flow='관리자 공통 틀', kind='정상', title='글꼴은 Pretendard 이고 아이콘은 Font Awesome 없이 보인다',
     pre='관리자 계정',
     steps='1. 브라우저 개발자 도구의 네트워크 탭을 연 채 관리자 화면에 로그인한다\n2. 왼쪽 메뉴 [대시보드] 를 열고 글자를 본다\n'
           '3. 왼쪽 메뉴와 화면 버튼의 아이콘을 본다',
     expect='1. 네트워크 요청에 Font Awesome 과 Inter 글꼴 요청이 없고 Pretendard 글꼴 요청이 있다\n'
            '2. 한글, 영문, 숫자가 Pretendard 로 보이고 본문 글자가 15px, 작은 글자가 13px 이다\n'
            '3. 왼쪽 메뉴 11개와 화면 버튼의 아이콘이 빈 네모나 빠진 자리 없이 보인다',
     judge=['합동', '합동', '합동'],
     pair={1: [1], 2: [2], 3: [3]},
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Low'),

dict(id='IT-12-070', flow='관리자 공통 틀', kind='정상', title='왼쪽 메뉴 구성과 순서가 그대로이고 1:1 문의 미답변 수가 보인다',
     pre='관리자 계정, 답변하지 않은 1:1 문의 2건',
     steps='1. 관리자 화면에 로그인해 왼쪽 메뉴를 위에서부터 읽는다\n2. 1:1 문의 메뉴 옆 숫자를 본다\n'
           '3. 왼쪽 메뉴 [1:1 문의] 에서 그중 1건에 답변한 뒤 왼쪽 메뉴 [대시보드] 로 옮긴다',
     expect='1. 왼쪽 메뉴가 위에서부터 대시보드, 회원 관리, 입찰 요청 관리, 원본 공고, 공고문 관리, SAM.gov 수집, 기지 관리, 고객 화면 사진, 문서 템플릿, 성공 제안서 패턴, 1:1 문의 11개다\n'
            '2. 1:1 문의 메뉴 옆에 미답변 수 2 가 보인다\n'
            '3. 대시보드로 옮긴 뒤 1:1 문의 메뉴 옆 미답변 수가 1 이다',
     judge=['가상', '가상', '가상'],
     pair={1: [1], 2: [2], 3: [3]},
     req=['BID-ADMIN-001'], fc=['BID-ADMIN-001'], screen='관리자 /', pri='Medium'),

]
