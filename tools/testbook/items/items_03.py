# 영역 03 입찰 접수 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-REQ-001~004), T1-3 BIZ-006·011·015·046·049·060·071, T1-4 POL-007, T1-5 BidRequest, T3-1 BidRequest, T3-2 F·G·G-1, T3-3 공고 상세·내 입찰 현황·입찰 상세, T3-4 공고 상세 입찰 신청·입찰 상세 문서 업로드·제출 전 검사·공고문 회원 전용 이다.

ITEMS = [

dict(id='IT-03-001', flow='입찰 신청', kind='정상', title='회원이 공고문 상세에서 서비스 레벨을 골라 입찰을 신청한다',
     pre='회원, 그 회원이 아직 신청하지 않은 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})에서 [제안서 작성] 을 누른다\n'
           '2. 서비스 레벨 4개 중 하나를 고른다\n'
           '3. [신청하기] 를 누른다\n'
           '4. 확인 창에서 확인을 누른다',
     expect='1. 서비스 레벨 선택지가 4개다\n'
            '2. 고른 레벨이 강조되고 그 레벨에 포함된 서비스가 보인다\n'
            '3. 3번 뒤 확인 창에 "이 공고에 입찰하시겠습니까?" 가 보인다\n'
            '4. 확인 뒤 로딩 표시를 거쳐 새 입찰 건의 입찰 상세(/proposals/{id})가 열린다\n'
            '5. 토스트 "입찰 신청이 완료되었습니다" 가 보인다\n'
            '6. 입찰 요청 기록이 그 회원, 공고 N, 상태 CREATED 로 1건 생긴다\n'
            '7. 그 기록의 service_level 이 2번에서 고른 레벨(MONITORING, ANALYSIS, AI_PROPOSAL, FULL_PACKAGE 중 하나)이다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '서버'],
     req=['BID-REQ-001', 'BID-PRICE-002'], fc=['BID-REQ-001', 'BID-PRICE-002'], screen='/search/{id}', pri='High',
     issue='T3-4 시나리오 1 은 "입찰 참여" 단추와 서비스 레벨 선택, 확인 창을 적으나 CR-157 은 "제안서 작성" 이라 부르고 화면 소스의 제안서 작성 창에는 서비스 레벨 선택이 없다'),

dict(id='IT-03-002', flow='입찰 신청', kind='정상', title='신청한 입찰 건이 내 입찰 목록과 홈 숫자에 반영된다',
     pre='회원, 입찰 건 1건이 있고 IT-03-001 처럼 공고 N 에 새로 신청한 직후',
     steps='1. 서류함 > 공고별 서류(/proposals)를 연다\n'
           '2. 홈(/)을 연다',
     expect='1. 목록에 공고 N 의 제목, 상태 CREATED, 고른 서비스 레벨, 오늘 신청일이 있는 줄이 생긴다\n'
            '2. 목록 줄 수가 신청 전보다 1 많은 2줄이다\n'
            '3. 홈 오늘 확인할 것의 입찰 신청 수가 2 다',
     judge=['가상', '가상', '가상'],
     req=['BID-REQ-001', 'BID-REQ-003'], fc=['BID-REQ-001', 'BID-REQ-003'], screen='/proposals', pri='High'),

dict(id='IT-03-003', flow='입찰 신청', kind='정상', title='입찰 신청 API 로 메모와 함께 신청한다',
     pre='회원 토큰, 그 회원이 신청하지 않은 공개 공고문 N',
     steps='1. POST /bid-requests 에 공고 N, 서비스 레벨 ANALYSIS, 메모 "현장 방문 희망" 을 넣어 부른다\n'
           '2. 응답의 id 로 GET /bid-requests/{id} 를 부른다',
     expect='1. 1번 응답이 성공이고 생성된 입찰 요청 정보(id, 상태 CREATED)를 준다\n'
            '2. 2번 응답의 공고가 N 이고 서비스 레벨이 ANALYSIS 다\n'
            '3. 2번 응답에 메모 "현장 방문 희망" 이 들어 있다',
     judge=['서버', '서버', '서버'],
     req=['BID-REQ-001'], fc=['BID-REQ-001'], screen='', pri='High',
     issue='T1-1 입력에 (선택) 메모가 있으나 T3-1 BidRequest 에 메모 칸이 없다'),

dict(id='IT-03-004', flow='입찰 신청', kind='정상', title='관리자 계정도 입찰을 신청한다',
     pre='회원 기간이 없는 관리자 계정, 그 계정이 신청하지 않은 공개 공고문 N',
     steps='1. POST /bid-requests 에 공고 N 과 서비스 레벨 MONITORING 을 넣어 부른다',
     expect='1. 응답이 성공이고 403 MEMBERSHIP_REQUIRED 가 아니다\n'
            '2. 입찰 요청 기록이 그 관리자 계정, 공고 N, 상태 CREATED 로 1건 생긴다',
     judge=['서버', '서버'],
     req=['BID-REQ-001', 'BID-AUTH-008'], fc=['BID-REQ-001', 'BID-AUTH-008'], screen='', pri='High'),

dict(id='IT-03-005', flow='입찰 신청', kind='정상', title='회원 기간이 끝나도 이미 낸 입찰 건은 그대로 본다',
     pre='회원 기간이 어제 끝나 무료회원이 된 고객, 회원일 때 낸 입찰 건 B1',
     steps='1. 서류함 > 공고별 서류(/proposals)를 연다\n'
           '2. B1 줄을 누른다\n'
           '3. GET /bid-requests/{B1} 을 부른다',
     expect='1. 목록에 B1 줄이 있다\n'
            '2. 2번 뒤 B1 의 입찰 상세(/proposals/{B1})가 열리고 상태 타임라인에 B1 의 지금 상태가 보인다\n'
            '3. 3번 응답이 200 이고 403 MEMBERSHIP_REQUIRED 가 아니다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-001', 'BID-REQ-004'], fc=['BID-REQ-001', 'BID-REQ-004'], screen='/proposals', pri='High'),

dict(id='IT-03-006', flow='입찰 신청', kind='예외', title='같은 공고에 두 번 신청하면 받지 않고 기존 입찰 건으로 잇는다',
     pre='회원, 그 회원이 이미 입찰 건 B1 을 낸 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})에서 [제안서 작성] 을 누르고 서비스 레벨을 골라 [신청하기], 확인을 누른다\n'
           '2. 토스트의 기존 입찰 상세 링크를 누른다\n'
           '3. POST /bid-requests 에 같은 공고 N 을 넣어 부른다',
     expect='1. 오류 토스트 "이미 이 공고에 입찰 신청하셨습니다" 와 기존 입찰 상세로 가는 링크가 보인다\n'
            '2. 2번 뒤 B1 의 입찰 상세(/proposals/{B1})가 열린다\n'
            '3. 3번 응답이 실패다\n'
            '4. 그 회원과 공고 N 의 입찰 요청 기록은 B1 1건 그대로다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-REQ-001'], fc=['BID-REQ-001'], screen='/search/{id}', pri='High',
     issue='화면 소스의 중복 문구는 "이미 해당 공고에 대한 입찰 요청이 존재합니다." 로 설계서 문구와 다르다'),

dict(id='IT-03-007', flow='입찰 신청', kind='예외', title='무료회원은 입찰을 신청하지 못한다',
     pre='무료회원, 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 연다\n'
           '2. POST /bid-requests 에 공고 N 과 서비스 레벨 MONITORING 을 넣어 부른다',
     expect='1. 회원 안내 상자 "공고문은 회원 전용입니다" 가 보이고 [제안서 작성] 이 없다\n'
            '2. 2번 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '3. 그 무료회원의 입찰 요청 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-REQ-001', 'BID-AUTH-008'], fc=['BID-REQ-001', 'BID-AUTH-008'], screen='/search/{id}', pri='High'),

dict(id='IT-03-008', flow='입찰 신청', kind='예외', title='로그인하지 않으면 입찰을 신청하지 못하고 로그인 뒤 그 공고로 돌아온다',
     pre='비로그인, 회원 계정, 공개 공고문 N',
     steps='1. 공고 상세(/search/{N})를 주소로 연다\n'
           '2. 회원 계정으로 로그인한다\n'
           '3. 토큰 없이 POST /bid-requests 에 공고 N 을 넣어 부른다',
     expect='1. 1번 뒤 로그인 화면(/login)이 열린다\n'
            '2. 로그인 뒤 공고 상세(/search/{N})로 돌아와 [제안서 작성] 이 보인다\n'
            '3. 3번 응답이 401 과 error LOGIN_REQUIRED 이고 입찰 요청 기록이 생기지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-001'], fc=['BID-REQ-001'], screen='/search/{id}', pri='High',
     issue='T3-4 시나리오 1 의 미로그인 "입찰 참여" 는 CR-157 뒤 공고 상세 자체가 로그인과 회원을 요구해 그대로는 일어나지 않는다'),

dict(id='IT-03-009', flow='서류 올리기', kind='정상', title='서류 대기 입찰 건의 빠진 요구 서류에 PDF 를 올린다',
     pre='회원, 상태 DOCS_PENDING 이고 필수 고객 올림 요구 항목 2개가 모두 빠진 입찰 건 B1, 10MB PDF 파일',
     steps='1. 입찰 상세(/proposals/{B1})의 제출 서류 탭을 연다\n'
           '2. 첫 빠짐 줄의 "올리기" 를 누르고 PDF 를 고른다',
     expect='1. 요구 항목이 필수 먼저 한 줄씩 있고 두 줄 모두 위험 색 "빠짐" 이다\n'
            '2. 요약이 "필수 2 중 올림 0" 이다\n'
            '3. 파일을 고르면 그 줄 단추가 꺼지고 "올리는 중" 이 보인다\n'
            '4. 끝나면 그 줄이 성공 색 "올림" 과 올린 파일 이름, 형식, 쪽수, 올린 날로 바뀐다\n'
            '5. 요약이 "필수 2 중 올림 1" 로 바뀐다\n'
            '6. 입찰 건 서류(ClientDocument) 1건과 그 요구 항목 연결(CLIENT_DOCUMENT, FULFILLED) 1건이 생긴다\n'
            '7. 그 서류의 page_count 에 PDF 쪽수가 들어간다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-010', flow='서류 올리기', kind='정상', title='지원 형식 DOCX, XLSX, JPG, PNG 를 올린다',
     pre='회원, 상태 DOCS_PENDING 이고 고객 올림 요구 항목이 4개인 입찰 건 B1, 50MB 이하 DOCX, XLSX, JPG, PNG 파일 하나씩',
     steps='1. 입찰 상세 제출 서류 탭에서 네 줄에 DOCX, XLSX, JPG, PNG 를 하나씩 올린다',
     expect='1. 네 줄 모두 "올림" 과 올린 파일 이름, 형식이 보인다\n'
            '2. 입찰 건 서류(ClientDocument)가 4건 생긴다\n'
            '3. 요구 항목 연결이 4건 모두 FULFILLED 다',
     judge=['가상', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-011', flow='서류 올리기', kind='정상', title='제출 문서 API 로 올리고, 목록을 보고, 지운다',
     pre='회원 토큰, 상태 DOCS_PENDING 인 본인 입찰 건 B1, 1MB PDF',
     steps='1. POST /bid-requests/{B1}/client-documents 에 PDF 를 multipart 로 올린다\n'
           '2. GET /bid-requests/{B1}/client-documents 를 부른다\n'
           '3. DELETE /bid-requests/{B1}/client-documents/{1번 서류 id} 를 부른다\n'
           '4. GET /bid-requests/{B1}/client-documents 를 다시 부른다',
     expect='1. 1번 응답이 성공이고 올린 서류 id 를 준다\n'
            '2. 2번 목록에 올린 서류 1건이 파일 이름과 함께 있다\n'
            '3. 3번 응답이 성공이다\n'
            '4. 4번 목록에 그 서류가 없다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='', pri='High'),

dict(id='IT-03-012', flow='서류 올리기', kind='정상', title='올리기 크기 한도 설정을 바꾸면 그 한도로 막는다',
     pre='회원 토큰, 상태 DOCS_PENDING 인 본인 입찰 건 B1, 서버 설정을 바꿔 다시 띄울 수 있는 환경, 9MB 와 11MB PDF',
     steps='1. spring.servlet.multipart.max-file-size 를 10MB 로 바꾸고 서버를 다시 띄운다\n'
           '2. POST /bid-requests/{B1}/client-documents 에 9MB PDF 를 올린다\n'
           '3. 같은 경로에 11MB PDF 를 올린다\n'
           '4. 설정을 50MB 로 되돌린다',
     expect='1. 2번 응답이 성공이다\n'
            '2. 3번 응답이 400 이다\n'
            '3. 11MB 서류의 기록이 생기지 않는다',
     judge=['서버', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='', pri='High'),

dict(id='IT-03-013', flow='서류 올리기', kind='예외', title='50MB 를 넘는 파일은 올리지 않는다',
     pre='회원, 상태 DOCS_PENDING 이고 빠진 필수 항목이 있는 입찰 건 B1, 51MB PDF',
     steps='1. 입찰 상세 제출 서류 탭의 빠짐 줄에서 "올리기" 를 누르고 51MB PDF 를 고른다\n'
           '2. POST /bid-requests/{B1}/client-documents 에 51MB PDF 를 올린다',
     expect='1. 그 줄에 "파일 크기가 50MB를 넘습니다" 가 보이고 줄은 "빠짐" 그대로다\n'
            '2. 2번 응답이 400 이다\n'
            '3. 입찰 건 서류와 요구 항목 연결이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High',
     issue='T3-4 입찰 상세 시나리오 2 는 "파일 크기가 50MB를 초과합니다" 토스트, CR-141 시나리오 2-1 은 "파일 크기가 50MB를 넘습니다" 로 문구가 다르다'),

dict(id='IT-03-014', flow='서류 올리기', kind='예외', title='지원하지 않는 형식의 파일은 저장하지 않는다',
     pre='회원, 상태 DOCS_PENDING 인 본인 입찰 건 B1, 1MB ZIP 파일',
     steps='1. 입찰 상세 제출 서류 탭의 빠짐 줄에서 "올리기" 를 누르고 ZIP 파일을 고른다\n'
           '2. POST /bid-requests/{B1}/client-documents 에 ZIP 파일을 올린다',
     expect='1. 그 줄이 "빠짐" 그대로다\n'
            '2. 2번 응답이 실패다\n'
            '3. ZIP 파일의 입찰 건 서류 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High',
     issue='T1-1 과 POL-007 은 지원 형식을 PDF, DOCX, XLSX, JPG, PNG 로 정하나 다른 형식을 올렸을 때의 문구와 응답 코드는 설계서에 없다'),

dict(id='IT-03-015', flow='서류 올리기', kind='예외', title='고객 올림이 아닌 요구 항목에는 올리지 못한다',
     pre='회원 토큰, 플랫폼 작성 요구 항목 R 이 있는 본인 입찰 건 B1(DOCS_PENDING), 1MB PDF',
     steps='1. POST /bid-requests/{B1}/required-document-slots/{R}/upload 에 PDF 를 올린다',
     expect='1. 응답이 400 이다\n'
            '2. R 의 요구 항목 연결이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='', pri='High'),

dict(id='IT-03-016', flow='서류 올리기', kind='예외', title='올리기가 서버에서 실패하면 그 줄을 누르기 전으로 되돌린다',
     pre='회원, 상태 DOCS_PENDING 인 입찰 건 B1, 슬롯 올리기 API 가 500 과 메시지를 주게 한 환경',
     steps='1. 입찰 상세 제출 서류 탭의 빠짐 줄에서 "올리기" 를 누르고 PDF 를 고른다',
     expect='1. 그 줄 아래 "저장하지 못했습니다" 와 서버가 준 메시지가 보인다\n'
            '2. 그 줄이 누르기 전과 같이 "빠짐" 과 켜진 "올리기" 로 돌아간다\n'
            '3. 요약 "필수 n 중 올림 n" 의 숫자가 그대로다',
     judge=['가상', '가상', '가상'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-017', flow='서류 제출 완료', kind='정상', title='필수 서류를 모두 올리고 서류 제출 완료로 서류 접수 상태가 된다',
     pre='회원, 상태 DOCS_PENDING 이고 필수 고객 올림 항목 2개를 모두 올린 입찰 건 B1',
     steps='1. 입찰 상세(/proposals/{B1}) 제출 서류 탭에서 "서류 제출 완료" 를 누른다\n'
           '2. 확인 창에서 확인을 누른다\n'
           '3. 진행 이력 탭을 연다',
     expect='1. "서류 제출 완료" 단추가 켜져 있다\n'
            '2. 확인 뒤 진행 단계가 서류 접수로 바뀌고 "올리기" 단추가 사라진다\n'
            '3. B1 의 상태가 DOCS_RECEIVED 다\n'
            '4. B1 의 state_history 에 fromState DOCS_PENDING, toState DOCS_RECEIVED, 그 회원 userId, automatic false 인 줄 1개가 더해진다\n'
            '5. 진행 이력 탭 맨 뒤 줄이 서류 접수로의 변화와 그 시각이다',
     judge=['가상', '가상', '서버', '서버', '가상'],
     req=['BID-REQ-002', 'BID-REQ-004'], fc=['BID-REQ-002', 'BID-REQ-004'], screen='/proposals/{id}', pri='High',
     issue='T3-3 입찰 상세는 단추 이름을 "문서 제출 완료", T3-4 CR-141 은 "서류 제출 완료" 로 적는다'),

dict(id='IT-03-018', flow='서류 제출 완료', kind='정상', title='필수 서류가 다 있으면 형식 어긋남이 있어도 경고만 하고 넘긴다',
     pre='회원, 상태 DOCS_PENDING 이고 필수 항목 2개를 모두 올렸으나 하나가 형식 어긋남인 입찰 건 B1',
     steps='1. 입찰 상세 제출 서류 탭을 연다\n'
           '2. "서류 제출 완료" 를 누르고 확인한다',
     expect='1. 단추 위에 "형식·쪽수가 공고 조건과 다르거나 사람이 볼 서류가 1개 있습니다. 그대로 넘기면 담당자가 확인합니다" 가 보인다\n'
            '2. "서류 제출 완료" 단추가 켜져 있다\n'
            '3. 2번 뒤 B1 의 상태가 DOCS_RECEIVED 다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-019', flow='서류 제출 완료', kind='정상', title='요구 항목이 없는 공고의 입찰 건은 바로 서류 접수로 넘어간다',
     pre='회원 토큰, 공고 분석 전이라 요구 항목이 없는 공고의 입찰 건 B2(DOCS_PENDING)',
     steps='1. PATCH /bid-requests/{B2}/state 에 DOCS_RECEIVED 를 넣어 부른다',
     expect='1. 응답이 성공이고 409 가 아니다\n'
            '2. B2 의 상태가 DOCS_RECEIVED 이고 state_history 에 DOCS_PENDING 에서 DOCS_RECEIVED 로 간 줄이 더해진다',
     judge=['서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='', pri='High'),

dict(id='IT-03-020', flow='서류 제출 완료', kind='예외', title='필수 서류가 빠지면 서류 제출 완료 단추를 끈다',
     pre='회원, 상태 DOCS_PENDING 이고 필수 항목 3개 중 "Business License", "Insurance Certificate" 2개가 빠진 입찰 건 B1',
     steps='1. 입찰 상세(/proposals/{B1}) 제출 서류 탭을 연다\n'
           '2. "서류 제출 완료" 를 눌러 본다',
     expect='1. 단추 위에 "필수 서류 2개가 빠졌습니다: Business License, Insurance Certificate" 가 보인다\n'
            '2. "서류 제출 완료" 단추가 비활성이다\n'
            '3. B1 의 상태가 DOCS_PENDING 그대로다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-021', flow='서류 제출 완료', kind='예외', title='필수 서류가 빠진 채 전환을 요청하면 409 와 빠진 항목을 준다',
     pre='회원, 상태 DOCS_PENDING 이고 필수 항목 2개를 다 올려 단추가 켜진 입찰 건 B1 을 두 창(창 A, 창 B)에 연 상태',
     steps='1. 창 B 에서 올린 줄 하나를 "빼기" 로 뺀다\n'
           '2. 창 A 에서 "서류 제출 완료" 를 누르고 확인한다\n'
           '3. PATCH /bid-requests/{B1}/state 에 DOCS_RECEIVED 를 넣어 부른다',
     expect='1. 창 A 단추 위에 응답의 빠진 서류 이름이 보인다\n'
            '2. 창 A 제출 서류 탭이 다시 불려 뺀 줄이 "빠짐" 으로 보인다\n'
            '3. 3번 응답이 409 와 error REQUIREMENT_SLOTS_NOT_FULFILLED, unfulfilledSlots 에 뺀 항목의 requirementItemId 와 title 이다\n'
            '4. B1 의 상태가 DOCS_PENDING 그대로이고 state_history 줄 수가 그대로다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-022', flow='내 입찰 목록', kind='정상', title='메뉴로 내 입찰 목록을 열어 본인 입찰 건을 본다',
     pre='회원, 상태가 CREATED, DOCS_PENDING, CLOSED 인 본인 입찰 건 3건',
     steps='1. 전체메뉴 서류함 > 공고별 서류를 누른다\n'
           '2. GET /bid-requests/my 를 부른다',
     expect='1. /proposals 가 열리고 목록 열이 공고 제목, 상태, 서비스 레벨, 신청일 이다\n'
            '2. 목록이 본인 입찰 건 3줄이고 줄마다 그 건의 공고 제목, 상태, 서비스 레벨, 신청일이 기록과 같다\n'
            '3. 2번 응답이 3건이다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High',
     issue='화면 소스의 /proposals 제목은 "내 제안서 관리" 로 설계서 화면 이름 "내 입찰 현황" 과 다르다'),

dict(id='IT-03-023', flow='내 입찰 목록', kind='정상', title='상태 필터로 내 입찰 목록을 거른다',
     pre='회원, 진행 중인 입찰 건(DOCS_PENDING) 1건과 끝난 입찰 건(CLOSED) 1건',
     steps='1. 내 입찰 목록(/proposals)의 상태 필터를 본다\n'
           '2. 진행중을 고른다\n'
           '3. 완료를 고른다\n'
           '4. 전체를 고른다',
     expect='1. 상태 필터가 전체, 진행중, 완료 셋이다\n'
            '2. 진행중을 고르면 DOCS_PENDING 건만 남는다\n'
            '3. 완료를 고르면 CLOSED 건만 남는다\n'
            '4. 전체를 고르면 2건이 모두 보인다',
     judge=['가상', 'QA', 'QA', '가상'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High',
     issue='진행중과 완료가 어느 상태를 묶는지 설계서에 없고 GET /bid-requests/my 에 상태 거르기 값이 없다'),

dict(id='IT-03-024', flow='내 입찰 목록', kind='정상', title='내 입찰 목록 API 를 쪽으로 나눠 받는다',
     pre='회원 토큰, 본인 입찰 건 3건',
     steps='1. GET /bid-requests/my?page=0&size=2 를 부른다\n'
           '2. GET /bid-requests/my?page=1&size=2 를 부른다',
     expect='1. 1번 응답이 2건이고 전체 건수가 3 이다\n'
            '2. 2번 응답이 1번과 겹치지 않는 1건이다',
     judge=['서버', '서버'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='', pri='High'),

dict(id='IT-03-025', flow='내 입찰 목록', kind='정상', title='입찰 건이 없으면 빈 상태 안내와 공고 검색 가는 길을 둔다',
     pre='입찰 건이 0건인 회원',
     steps='1. 내 입찰 목록(/proposals)을 연다\n'
           '2. 빈 상태의 공고 검색 이동 단추를 누른다',
     expect='1. 목록 줄이 없고 신청 건이 없다는 안내가 보인다\n'
            '2. 2번 뒤 공고 검색(/search)이 열린다',
     judge=['가상', '가상'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High'),

dict(id='IT-03-026', flow='내 입찰 목록', kind='정상', title='내 입찰 목록 줄과 홈 숫자에서 입찰 상세로 간다',
     pre='회원, 본인 입찰 건 B1',
     steps='1. 내 입찰 목록(/proposals)에서 B1 줄을 누른다\n'
           '2. 홈(/) 오늘 확인할 것의 입찰 신청 수를 누른다',
     expect='1. 1번 뒤 B1 의 입찰 상세(/proposals/{B1})가 열린다\n'
            '2. 2번 뒤 내 입찰 목록(/proposals)이 열린다',
     judge=['가상', '가상'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High'),

dict(id='IT-03-027', flow='내 입찰 목록', kind='예외', title='내 입찰 목록에 다른 고객의 입찰 건은 섞이지 않는다',
     pre='회원 A 의 입찰 건 2건, 회원 B 의 입찰 건 1건(B 만 신청한 공고)',
     steps='1. A 로 내 입찰 목록(/proposals)을 연다\n'
           '2. A 토큰으로 GET /bid-requests/my 를 부른다',
     expect='1. 목록이 A 의 2줄이고 B 가 신청한 공고 제목 줄은 없다\n'
            '2. 2번 응답이 A 의 2건뿐이다',
     judge=['가상', '서버'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High'),

dict(id='IT-03-028', flow='내 입찰 목록', kind='예외', title='로그인하지 않으면 내 입찰 목록을 열지 않는다',
     pre='비로그인',
     steps='1. 내 입찰 목록(/proposals)을 주소로 연다\n'
           '2. 토큰 없이 GET /bid-requests/my 를 부른다',
     expect='1. 로그인 화면(/login)이 열린다\n'
            '2. 2번 응답이 401 이다',
     judge=['가상', '서버'],
     req=['BID-REQ-003'], fc=['BID-REQ-003'], screen='/proposals', pri='High'),

dict(id='IT-03-029', flow='입찰 상세', kind='정상', title='입찰 상세의 상태 타임라인과 공고 정보를 본다',
     pre='회원, 관리자가 CREATED 에서 DOCS_PENDING 으로 넘긴 본인 입찰 건 B1',
     steps='1. 입찰 상세(/proposals/{B1})를 연다\n'
           '2. 진행 이력 탭을 연다\n'
           '3. GET /bid-requests/{B1}/history 를 부른다\n'
           '4. 공고 정보의 공고 제목을 누른다',
     expect='1. 상태 타임라인의 현재 상태가 DOCS_PENDING 이다\n'
            '2. 진행 이력에 CREATED 에서 DOCS_PENDING 으로 간 변화와 그 시각이 있다\n'
            '3. 3번 응답의 줄이 fromState CREATED, toState DOCS_PENDING, userId, username, timestamp, notes, automatic 을 갖는다\n'
            '4. 공고 정보에 그 공고의 제목, 기관명, 마감일이 있다\n'
            '5. 4번 뒤 그 공고의 공고 상세(/search/{id})가 열린다',
     judge=['가상', '가상', '서버', '가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-030', flow='입찰 상세', kind='정상', title='입찰 상세 제출 서류 탭에서 입찰 건별 제출 전 검사로 간다',
     pre='회원, 상태 DOCS_PENDING 인 본인 입찰 건 B1',
     steps='1. 입찰 상세(/proposals/{B1}) 제출 서류 탭을 연다\n'
           '2. "제출 전 검사 보기" 를 누른다',
     expect='1. 제출 서류 탭이 GET /bid-requests/{B1}/pre-check 로 요구 서류 목록을 그린다\n'
            '2. 2번 뒤 입찰 건별 제출 전 검사(/documents/pre-check/{B1})가 열린다\n'
            '3. 두 화면의 요구 항목 줄과 "필수 n 중 올림 n" 숫자가 같다',
     judge=['가상', '가상', '가상'],
     req=['BID-REQ-004', 'BID-REQ-002'], fc=['BID-REQ-004', 'BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-031', flow='입찰 상세', kind='정상', title='관리자가 바꾼 상태가 새로고침한 입찰 상세에 반영된다',
     pre='회원, 상태 CREATED 인 본인 입찰 건 B1 을 입찰 상세로 열어 둔 상태, 관리자 계정',
     steps='1. 관리자 화면 입찰 상세 관리(/bid-requests/{B1})에서 상태를 DOCS_PENDING 으로 넘긴다\n'
           '2. 고객의 입찰 상세(/proposals/{B1})를 새로고침한다',
     expect='1. 상태 타임라인의 현재 상태가 CREATED 에서 DOCS_PENDING 으로 바뀐다\n'
            '2. 제출 서류 탭에 "올리기" 단추가 켜진다\n'
            '3. 진행 이력에 CREATED 에서 DOCS_PENDING 으로 간 줄이 더해진다',
     judge=['가상', '가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-032', flow='입찰 상세', kind='정상', title='CREATED 와 DOCS_PENDING 입찰 건의 단추 상태를 본다',
     pre='회원, 상태 CREATED 인 본인 입찰 건 B1 과 DOCS_PENDING 인 B2',
     steps='1. B1 의 입찰 상세를 연다\n'
           '2. B2 의 입찰 상세를 연다',
     expect='1. B1 의 문서 업로드가 비활성이다\n'
            '2. B1 의 취소 단추가 활성이다\n'
            '3. B1 의 생성 문서 자리에 문서가 없다\n'
            '4. B2 의 문서 업로드가 활성이다\n'
            '5. B2 의 취소 단추가 활성이다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High',
     issue='T3-3 은 취소를 "DOCS_PENDING 이전" 으로 적으나 T3-4 상태별 UI 분기는 DOCS_PENDING 에서도 취소를 활성으로 둔다'),

dict(id='IT-03-033', flow='입찰 상세', kind='정상', title='DOCS_RECEIVED 와 CLOSED 입찰 건은 업로드와 취소를 막는다',
     pre='회원, 상태 DOCS_RECEIVED 인 본인 입찰 건 B3 과 CLOSED 인 B4',
     steps='1. B3 의 입찰 상세를 연다\n'
           '2. B4 의 입찰 상세를 연다',
     expect='1. B3 의 문서 업로드가 비활성이다\n'
            '2. B3 의 취소 단추가 비활성이다\n'
            '3. B4 의 문서 업로드가 비활성이다\n'
            '4. B4 의 취소 단추가 비활성이다\n'
            '5. B4 의 생성 문서 자리에 문서가 없다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-034', flow='입찰 상세', kind='정상', title='ANALYZING 과 GENERATING 입찰 건은 AI 작성 중을 보인다',
     pre='회원, 상태 ANALYZING 인 본인 입찰 건 B5 와 GENERATING 인 B6',
     steps='1. B5 의 입찰 상세 생성 문서 탭을 연다\n'
           '2. B6 의 입찰 상세 생성 문서 탭을 연다',
     expect='1. B5 생성 문서 자리에 "AI 작성 중" 이 보이고 업로드와 취소가 비활성이다\n'
            '2. B6 생성 문서 자리에 "AI 작성 중" 이 보이고 업로드와 취소가 비활성이다',
     judge=['가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-035', flow='입찰 상세', kind='정상', title='REVIEW, CONFIRMED, SUBMITTED 입찰 건의 생성 문서를 본다',
     pre='회원, 생성 문서가 DRAFT 인 REVIEW 입찰 건 B7, 생성 문서가 LOCKED 인 CONFIRMED 입찰 건 B8 과 SUBMITTED 입찰 건 B9',
     steps='1. B7 의 입찰 상세 생성 문서 탭에서 문서를 연다\n'
           '2. B8 의 생성 문서 탭에서 문서를 연다\n'
           '3. B9 의 생성 문서 탭에서 문서를 연다\n'
           '4. GET /bid-requests/{B7}/documents 를 부른다',
     expect='1. B7 생성 문서 목록에 AI 생성 제안서가 상태 DRAFT 로 있고 누르면 그 문서의 미리보기 창에 해당 제안서 본문이 뜬다\n'
            '2. B8 생성 문서 목록에 상태 LOCKED 문서가 있고 누르면 그 문서의 미리보기 창에 해당 문서 본문이 뜬다\n'
            '3. B9 생성 문서 자리에서 최종 문서가 열린다\n'
            '4. 세 건 모두 업로드와 취소가 비활성이다\n'
            '5. 4번 응답이 B7 의 생성 문서 목록과 상태 DRAFT 다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-036', flow='입찰 상세', kind='정상', title='허용 다음 상태 API 는 T1-5 의 허용 상태 밖을 주지 않는다',
     pre='회원 토큰, 상태 CREATED 인 본인 입찰 건 B1',
     steps='1. GET /bid-requests/{B1}/next-states 를 부른다',
     expect='1. 응답에 CLOSED 가 있다\n'
            '2. 응답에 DOCS_PENDING, CLOSED 밖의 상태가 없다',
     judge=['서버', '서버'],
     req=['BID-REQ-004', 'BID-FSM-001'], fc=['BID-REQ-004', 'BID-FSM-001'], screen='', pri='High'),

dict(id='IT-03-037', flow='입찰 상세', kind='예외', title='입찰 상세를 못 불러오면 오류와 목록 가는 길을 둔다',
     pre='회원, 본인 입찰 건 B1, GET /bid-requests/{id} 가 500 으로 실패하게 한 환경',
     steps='1. 입찰 상세(/proposals/{B1})를 연다',
     expect='1. 상세 자리에 불러오지 못했다는 오류 문구가 보인다\n'
            '2. 내 입찰 목록(/proposals)으로 가는 링크가 있다',
     judge=['가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-038', flow='입찰 상세', kind='예외', title='상태 이력을 못 불러오면 이력 자리만 빈 상태로 둔다',
     pre='회원, 본인 입찰 건 B1, GET /bid-requests/{id}/history 가 실패하게 한 환경',
     steps='1. 입찰 상세(/proposals/{B1})를 열고 진행 이력 탭을 연다',
     expect='1. 진행 이력 자리가 빈 상태다\n'
            '2. 상태 타임라인의 현재 상태와 공고 정보의 제목, 기관명, 마감일은 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-039', flow='입찰 상세', kind='예외', title='제출 서류를 못 불러오면 다시 시도 단추를 둔다',
     pre='회원, 본인 입찰 건 B1, GET /bid-requests/{id}/pre-check 가 500 으로 실패하게 한 환경',
     steps='1. 입찰 상세(/proposals/{B1}) 제출 서류 탭을 연다\n'
           '2. 실패를 풀고 다시 시도를 누른다',
     expect='1. 제출 서류 자리에 "불러오지 못했습니다" 와 다시 시도 단추가 보인다\n'
            '2. 2번 뒤 요구 서류 줄과 "필수 n 중 올림 n" 요약이 그려진다',
     judge=['가상', '가상'],
     req=['BID-REQ-004', 'BID-REQ-002'], fc=['BID-REQ-004', 'BID-REQ-002'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-040', flow='입찰 상세', kind='예외', title='생성 문서를 못 불러오면 생성 문서 자리만 빈 상태로 둔다',
     pre='회원, 상태 REVIEW 인 본인 입찰 건 B7, GET /bid-requests/{id}/documents 가 실패하게 한 환경',
     steps='1. 입찰 상세(/proposals/{B7}) 생성 문서 탭을 연다',
     expect='1. 생성 문서 자리가 빈 상태다\n'
            '2. 상태 타임라인의 현재 상태 REVIEW 는 그대로 보인다',
     judge=['가상', '가상'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-041', flow='입찰 취소', kind='정상', title='고객이 CREATED 입찰 건을 취소한다',
     pre='회원, 상태 CREATED 인 본인 입찰 건 B1',
     steps='1. 입찰 상세(/proposals/{B1})에서 [취소] 를 누른다\n'
           '2. 내 입찰 목록(/proposals)을 연다',
     expect='1. 상태 타임라인의 현재 상태가 CLOSED 로 바뀐다\n'
            '2. 취소 단추가 비활성으로 바뀐다\n'
            '3. B1 의 상태가 CLOSED 이고 closed_at 이 채워진다\n'
            '4. B1 의 state_history 에 fromState CREATED, toState CLOSED, 그 회원 userId 인 줄이 더해진다\n'
            '5. 내 입찰 목록 B1 줄의 상태가 CLOSED 다',
     judge=['가상', '가상', '서버', '서버', '가상'],
     req=['BID-REQ-004', 'BID-FSM-001'], fc=['BID-REQ-004', 'BID-FSM-001'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-042', flow='입찰 취소', kind='정상', title='고객이 DOCS_PENDING 입찰 건을 취소한다',
     pre='회원, 상태 DOCS_PENDING 인 본인 입찰 건 B2',
     steps='1. 입찰 상세(/proposals/{B2})에서 [취소] 를 누른다',
     expect='1. 상태 타임라인의 현재 상태가 CLOSED 로 바뀐다\n'
            '2. 제출 서류 탭의 "올리기" 단추가 사라진다\n'
            '3. B2 의 상태가 CLOSED 이고 state_history 에 fromState DOCS_PENDING, toState CLOSED 인 줄이 더해진다',
     judge=['가상', '가상', '서버'],
     req=['BID-REQ-004', 'BID-FSM-001'], fc=['BID-REQ-004', 'BID-FSM-001'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-043', flow='입찰 취소', kind='예외', title='고객은 취소와 서류 접수 밖의 상태로 넘기지 못한다',
     pre='회원 토큰, 상태 DOCS_RECEIVED 인 본인 입찰 건 B3',
     steps='1. PATCH /bid-requests/{B3}/state 에 ANALYZING 을 넣어 부른다',
     expect='1. 응답이 403 이다\n'
            '2. B3 의 상태가 DOCS_RECEIVED 그대로이고 state_history 줄 수가 그대로다',
     judge=['서버', '서버'],
     req=['BID-REQ-004', 'BID-FSM-001'], fc=['BID-REQ-004', 'BID-FSM-001'], screen='', pri='High'),

dict(id='IT-03-044', flow='남의 입찰 건 차단', kind='예외', title='남의 입찰 건 상세는 없는 건과 같이 찾을 수 없다고 답한다',
     pre='회원 A, 회원 B 의 입찰 건 BB',
     steps='1. A 로 입찰 상세(/proposals/{BB})를 연다\n'
           '2. A 토큰으로 GET /bid-requests/{BB} 를 부른다\n'
           '3. A 토큰으로 GET /bid-requests/{BB}/history 를 부른다\n'
           '4. A 토큰으로 GET /bid-requests/{BB}/documents 를 부른다\n'
           '5. A 토큰으로 GET /bid-requests/{BB}/next-states 를 부른다',
     expect='1. 1번 화면에 "입찰 건을 찾을 수 없습니다" 가 보이고 BB 의 공고 제목과 상태는 없다\n'
            '2. 2번 응답이 404 와 "입찰 건을 찾을 수 없습니다" 다\n'
            '3. 3번 응답이 404 와 "입찰 건을 찾을 수 없습니다" 다\n'
            '4. 4번 응답이 404 와 "입찰 건을 찾을 수 없습니다" 다\n'
            '5. 5번 응답이 404 와 "입찰 건을 찾을 수 없습니다" 다',
     judge=['가상', '서버', '서버', '서버', '서버'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High',
     issue='T3-4 입찰 상세 진입 불가는 "403 (타인 입찰) 또는 404" 로 적어 BIZ-060 의 404 와 다르고, 화면 소스 문구는 "제안서를 찾을 수 없습니다." 다'),

dict(id='IT-03-045', flow='남의 입찰 건 차단', kind='예외', title='없는 입찰 건 번호도 같은 404 로 답한다',
     pre='회원 A, 없는 입찰 건 id(임의 UUID)',
     steps='1. A 로 입찰 상세(/proposals/{없는 id})를 연다\n'
           '2. A 토큰으로 GET /bid-requests/{없는 id} 를 부른다',
     expect='1. 1번 화면에 "입찰 건을 찾을 수 없습니다" 가 보인다\n'
            '2. 2번 응답이 404 와 "입찰 건을 찾을 수 없습니다" 이고 IT-03-044 의 남의 입찰 건 응답과 본문이 같다',
     judge=['가상', '서버'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='/proposals/{id}', pri='High'),

dict(id='IT-03-046', flow='남의 입찰 건 차단', kind='예외', title='남의 입찰 건에 서류를 올리거나 목록을 보지 못한다',
     pre='회원 A 토큰, 회원 B 의 입찰 건 BB(DOCS_PENDING, 고객 올림 요구 항목 R), 1MB PDF',
     steps='1. POST /bid-requests/{BB}/client-documents 에 PDF 를 올린다\n'
           '2. GET /bid-requests/{BB}/client-documents 를 부른다\n'
           '3. POST /bid-requests/{BB}/required-document-slots/{R}/upload 에 PDF 를 올린다',
     expect='1. 1번 응답이 404 다\n'
            '2. 2번 응답이 404 이고 B 의 서류 이름이 담기지 않는다\n'
            '3. 3번 응답이 404 다\n'
            '4. BB 의 입찰 건 서류와 요구 항목 연결이 늘지 않는다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-REQ-002', 'BID-REQ-004'], fc=['BID-REQ-002', 'BID-REQ-004'], screen='', pri='High'),

dict(id='IT-03-047', flow='남의 입찰 건 차단', kind='예외', title='남의 입찰 건 상태를 바꾸지 못한다',
     pre='회원 A 토큰, 회원 B 의 입찰 건 BB(CREATED)',
     steps='1. PATCH /bid-requests/{BB}/state 에 CLOSED 를 넣어 부른다',
     expect='1. 응답이 404 와 "입찰 건을 찾을 수 없습니다" 다\n'
            '2. BB 의 상태가 CREATED 그대로이고 state_history 줄 수가 그대로다',
     judge=['서버', '서버'],
     req=['BID-REQ-004', 'BID-FSM-001'], fc=['BID-REQ-004', 'BID-FSM-001'], screen='', pri='High'),

dict(id='IT-03-048', flow='남의 입찰 건 차단', kind='예외', title='다른 입찰 건의 서류를 내 입찰 건 경로로 지우지 못한다',
     pre='회원 A 토큰, A 의 입찰 건 BA, 회원 B 의 입찰 건 BB 에 올린 서류 D',
     steps='1. DELETE /bid-requests/{BA}/client-documents/{D} 를 부른다',
     expect='1. 응답이 404 와 "서류를 찾을 수 없습니다" 다\n'
            '2. 서류 D 의 기록이 BB 에 그대로 있다',
     judge=['서버', '서버'],
     req=['BID-REQ-002'], fc=['BID-REQ-002'], screen='', pri='High'),

dict(id='IT-03-049', flow='남의 입찰 건 차단', kind='정상', title='관리자는 고객의 입찰 건을 본다',
     pre='관리자 토큰, 회원 B 의 입찰 건 BB',
     steps='1. GET /bid-requests/{BB} 를 부른다\n'
           '2. GET /bid-requests/{BB}/history 를 부른다',
     expect='1. 1번 응답이 200 이고 BB 의 상태와 공고를 준다\n'
            '2. 2번 응답이 200 이고 BB 의 상태 이력을 준다',
     judge=['서버', '서버'],
     req=['BID-REQ-004'], fc=['BID-REQ-004'], screen='', pri='High'),

]
