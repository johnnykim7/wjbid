# 영역 08 제출 전 검사/서류함 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-MKT-020~024, BID-MKT-035), T1-3 BIZ-045~049·065·066, T1-4 POL-028, T1-5 BidRequest 상태, T3-1, T3-2, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-08-001', flow='요구 항목 채우기', kind='정상', title='평가 항목별 제출물이 있는 공고는 제출물마다 요구 항목이 생긴다',
     pre='관리자 계정, 공고 분석 제출 서류에 평가 항목별 제출물 4개(고객 올림 2, 플랫폼 작성 1, 정부 양식 1)가 있는 공고문 1건, 요구 항목 0행',
     steps='1. 관리자 화면 공고문 관리 상세(/notices/{id})에서 [재생성] 으로 공고 분석을 다시 돌려 분석 완료 저장까지 기다린다\n2. 그 공고의 요구 항목(requirement_items)을 조회한다',
     expect='1. 요구 항목이 4행이고 순서 칸이 공고 분석의 제출물 순서다\n'
            '2. 행마다 분류가 DOCUMENT, 제목이 제출물 서류 이름이다\n'
            '3. requirement_json 에 sourceKey(제출물 번호), fulfillmentParty, mandatory, format, pageLimit, sourceRef, notes, factorId, factorTitle 이 분석 값으로 들어 있다\n'
            '4. fulfillmentParty 가 CLIENT_UPLOAD 2행, PLATFORM_GENERATED 1행, SYSTEM_FORM 1행이다\n'
            '5. is_blocker 가 필수이고 CLIENT_UPLOAD 인 행만 true 다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-MKT-020'], fc=['BID-MKT-020'], screen='관리자 /notices/{id}', pri='Medium'),

dict(id='IT-08-002', flow='요구 항목 채우기', kind='정상', title='평가 항목별 제출물이 없으면 평면 서류 목록마다 고객 올림 항목이 생긴다',
     pre='관리자 계정, 공고 분석에 평가 항목별 제출물이 없고 평면 서류 목록 3개만 있는 공고문 1건',
     steps='1. 관리자 화면에서 그 공고 분석을 다시 돌려 분석 완료 저장까지 기다린다\n2. 그 공고의 요구 항목을 조회한다',
     expect='1. 요구 항목이 3행이고 제목이 평면 서류 목록의 서류 이름이다\n'
            '2. 3행 모두 fulfillmentParty 가 CLIENT_UPLOAD 이고 factorId 가 비어 있다\n'
            '3. sourceKey 가 서류 이름이다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-020'], fc=['BID-MKT-020'], screen='관리자 /notices/{id}', pri='Medium'),

dict(id='IT-08-003', flow='요구 항목 채우기', kind='정상', title='항목 열쇠가 겹치면 순번을 붙여 따로 둔다',
     pre='관리자 계정, 평면 서류 목록에 같은 이름 "Resume" 가 2번 있는 공고문 1건',
     steps='1. 관리자 화면에서 그 공고 분석을 다시 돌린다\n2. 그 공고의 요구 항목을 조회한다',
     expect='1. 제목 "Resume" 인 요구 항목이 2행이다\n'
            '2. 두 행의 sourceKey 가 서로 다르고 뒤 행에 순번이 붙어 있다',
     judge=['서버', '서버'],
     req=['BID-MKT-020'], fc=['BID-MKT-020'], screen='관리자 /notices/{id}', pri='Medium'),

dict(id='IT-08-004', flow='요구 항목 채우기', kind='정상', title='관리자 수정으로 다시 맞추면 같은 항목은 고치고 새 항목은 넣고 사라진 항목은 연결과 함께 지운다',
     pre='관리자 계정, 요구 항목 A, B, C 가 있는 공고문, 고객 입찰 건에서 C 에 파일을 이어 둠',
     steps='1. 관리자 화면에서 공고 분석 제출 서류를 고쳐(PATCH 보정) A 의 형식 원문을 "PDF" 로 바꾸고 C 를 지우고 D 를 더한다\n2. 그 공고의 요구 항목, 입찰 건 연결, 입찰 건 서류를 조회한다\n3. 고객이 그 입찰 건 제출 전 검사를 연다',
     expect='1. A 행은 id 가 그대로이고 format 이 "PDF" 로 바뀐다\n'
            '2. B 행은 바뀌지 않는다\n'
            '3. C 행과 C 의 입찰 건 연결 행이 지워진다\n'
            '4. C 에 올렸던 입찰 건 서류(client_documents) 행은 남아 있다\n'
            '5. D 행이 새로 생긴다\n'
            '6. 3번 화면 요구 서류 줄에 C 가 없고 D 가 있다',
     judge=['서버', '서버', '서버', '서버', '서버', '가상'],
     req=['BID-MKT-020'], fc=['BID-MKT-020'], screen='관리자 /notices/{id}', pri='Medium'),

dict(id='IT-08-005', flow='요구 항목 채우기', kind='정상', title='서버가 뜰 때 분석은 끝났는데 요구 항목이 없는 공고를 채운다',
     pre='분석 완료(COMPLETED)이고 제출 서류가 있으나 요구 항목이 0행인 공고문 2건',
     steps='1. 서버를 다시 띄운다\n2. 두 공고의 요구 항목을 조회한다',
     expect='1. 두 공고 모두 제출 서류 수만큼 요구 항목 행이 생긴다\n'
            '2. 이미 요구 항목이 있던 다른 공고의 행 수와 updated_at 이 바뀌지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-020'], fc=['BID-MKT-020'], screen='', pri='Medium'),

dict(id='IT-08-006', flow='제출 전 검사 목록', kind='정상', title='회원이 메뉴로 제출 전 검사를 열어 내 입찰 건 요약을 본다',
     pre='고객 계정, 내 입찰 건 3건(서류 대기 2건 중 1건은 필수 5 중 올림 3, 어긋남 1, 사람 확인 0, 다른 1건은 마감 지남)',
     steps='1. 고객 계정으로 로그인해 메뉴 서류함 > 제출 전 검사 를 누른다',
     expect='1. 주소가 /documents/pre-check 이고 왼쪽 서류함 묶음 메뉴에서 제출 전 검사가 옅은 파랑 바탕과 왼쪽 파란 줄로 켜져 있다\n'
            '2. 머리에 "제출 전 검사" 와 "공고가 요구하는 서류와 올린 서류를 맞춰 빠진 것, 파일 형식, PDF 쪽수를 봅니다. 서류 내용은 사람이 봅니다" 가 보인다\n'
            '3. 순서 세 칸이 입찰 건 고르기, 빠진 서류 올리기, 내용은 검토 신청 순서다\n'
            '4. 입찰 건 3줄이 마감일 가까운 순이다\n'
            '5. 줄마다 한글 제목, 공고번호, 날짜만 있는 마감일, 진행 단계가 있다\n'
            '6. 마감 지난 줄에 "마감 지남" 꼬리표가 있다\n'
            '7. 필수 5 중 3 을 올린 줄에 "필수 5 중 올림 3", 빠짐 2, 어긋남 1, 사람 확인 0 이 있고 0 인 사람 확인이 흐리다\n'
            '8. 목록 응답(GET /bid-requests/my/pre-check)의 그 줄 summary 가 required 5, uploaded 3, missing 2, mismatch 1, needsHuman 0 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-007', flow='제출 전 검사 목록', kind='정상', title='공고 분석 전인 입찰 건은 숫자 대신 안내와 검토 신청을 보인다',
     pre='고객 계정, 공고에 요구 항목이 없는 입찰 건 1건',
     steps='1. /documents/pre-check 에서 그 줄을 본다\n2. 그 줄의 [제출 전 서류 검토 신청] 을 누른다',
     expect='1. 그 줄에 숫자 요약 없이 "공고 분석 전이라 요구 서류를 알 수 없습니다" 가 보인다\n'
            '2. 목록 응답에서 그 건의 analysisReady 가 false 이고 summary 가 null 이다\n'
            '3. 2번에서 1:1 문의(/support)가 열려 유형이 서비스 상담으로 골라져 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-008', flow='제출 전 검사 목록', kind='정상', title='입찰 건이 없으면 빈 안내와 입찰 검색 단추를 보인다',
     pre='입찰 건이 0건인 고객 계정',
     steps='1. /documents/pre-check 를 연다\n2. [입찰 검색] 을 누른다',
     expect='1. 목록 자리에 "입찰 신청한 공고가 없습니다" 가 보인다\n'
            '2. 2번에서 마감 전 공고(/search)가 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-009', flow='제출 전 검사 목록', kind='정상', title='입찰 건 줄을 누르면 건별 검사가 열리고 뒤로 오면 보던 위치다',
     pre='고객 계정, 내 입찰 건 10건 이상',
     steps='1. /documents/pre-check 에서 아래로 내려 8번째 줄을 누른다\n2. 브라우저 뒤로 가기를 누른다',
     expect='1. 1번에서 /documents/pre-check/{그 입찰 건 id} 가 열리고 머리 제목이 그 줄의 한글 제목이다\n'
            '2. 2번 뒤 누르기 전 스크롤 위치로 돌아와 8번째 줄이 화면 안에 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-010', flow='제출 전 검사 목록', kind='정상', title='다른 회원의 입찰 건은 내 목록에 섞이지 않는다',
     pre='고객 계정 A(입찰 건 2건)와 B(입찰 건 3건)',
     steps='1. A 로 /documents/pre-check 를 연다',
     expect='1. 입찰 건 줄이 A 의 2건뿐이다\n'
            '2. 목록 응답의 bidRequestId 가 모두 A 가 낸 입찰 건이다',
     judge=['가상', '서버'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-011', flow='제출 전 검사 목록', kind='예외', title='비로그인은 로그인을 거쳐 제출 전 검사로 돌아온다',
     pre='비로그인',
     steps='1. /documents/pre-check 를 연다\n2. 고객 계정으로 로그인한다',
     expect='1. 1번에서 로그인 화면(/login)으로 간다\n'
            '2. 2번 뒤 /documents/pre-check 로 돌아와 "제출 전 검사" 머리가 보인다',
     judge=['가상', '가상'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-012', flow='제출 전 검사 목록', kind='예외', title='목록을 불러오지 못하면 다시 시도를 준다',
     pre='고객 계정, 목록 조회가 오류로 답하는 상태',
     steps='1. /documents/pre-check 를 연다\n2. 조회가 되살아난 뒤 [다시 시도] 를 누른다',
     expect='1. 1번 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 2번 뒤 입찰 건 줄이 마감일 가까운 순으로 채워진다',
     judge=['가상', '가상'],
     req=['BID-MKT-021'], fc=['BID-MKT-021'], screen='/documents/pre-check', pri='Medium'),

dict(id='IT-08-013', flow='입찰 건별 검사', kind='정상', title='입찰 건별 검사에 요구 서류마다 조건 원문, 올린 파일, 판정 셋과 근거가 보인다',
     pre='고객 계정, 서류 대기 입찰 건 1건(고객 올림 필수 2개, 필수 아님 1개, 플랫폼 작성 1개), 필수 A 는 형식 "PDF/Word", 쪽수 "1 page" 이고 1쪽 PDF 를 올림, 필수 B 는 안 올림',
     steps='1. /documents/pre-check 에서 그 입찰 건 줄을 누른다\n2. A 줄의 공고 설명을 펼친다',
     expect='1. 머리에 한글 제목, 공고번호, 마감일, 진행 단계와 요약 "필수 2 중 올림 1", 빠짐 1, 어긋남 0, 사람 확인 0 이 있다\n'
            '2. 올릴 서류 줄이 필수 A, B 먼저 분석 순서로 오고 필수 아닌 항목이 뒤에 있다\n'
            '3. A 줄에 서류 이름, 필수 표시, 원문 위치, 형식 "PDF/Word", 쪽수 "1 page" 가 있다\n'
            '4. A 줄 올린 파일에 파일 이름, 형식 PDF, 1쪽, 올린 날이 있다\n'
            '5. A 줄 판정이 성공 색 "올림", 성공 색 "형식 맞음", 성공 색 "1쪽, 맞음" 이다\n'
            '6. A 줄 판정 아래 근거 "PDF/Word 에서 PDF, Word 를 읽음" 과 "1 page 에서 1쪽을 읽음" 이 있다\n'
            '7. 2번에서 A 의 공고 설명 글이 펼쳐진다\n'
            '8. 플랫폼이 만드는 서류 칸에 플랫폼 작성 항목 이름, 원문 위치, "올리지 않아도 됩니다" 가 있다\n'
            '9. "서류 내용이 공고 조건에 맞는지(실적 금액, 인증 기준 등)는 사람이 봅니다" 가 보인다\n'
            '10. 건별 조회 응답의 clientItems 가 3개, platformItems 가 1개다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-014', flow='입찰 건별 검사', kind='정상', title='안 올린 항목은 필수 여부에 따라 빠짐과 안 올림으로 나뉘고 형식·쪽수 꼬리표가 없다',
     pre='고객 계정, 서류 대기 입찰 건에 안 올린 필수 항목 B 와 안 올린 필수 아닌 항목 C',
     steps='1. 그 입찰 건별 검사를 연다',
     expect='1. B 줄 서류 판정이 위험 색 "빠짐" 이다\n'
            '2. C 줄 서류 판정이 회색 "안 올림" 이다\n'
            '3. B, C 줄에 형식 꼬리표와 쪽수 꼬리표가 없다\n'
            '4. 건별 조회 응답에서 B 의 presence 가 MISSING, format.verdict 와 pages.verdict 가 NOT_UPLOADED 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-015', flow='입찰 건별 검사', kind='정상', title='제출 전 서류 검토 신청은 공고번호와 걸린 서류 이름을 채운 1:1 문의를 연다',
     pre='고객 계정, 공고번호 W90VN926RA081 입찰 건, 빠짐 서류 B, 형식 어긋남 서류 D, 사람 확인 서류 E',
     steps='1. 그 입찰 건별 검사에서 [제출 전 서류 검토 신청] 을 누른다',
     expect='1. 1:1 문의(/support)가 열려 유형이 서비스 상담으로 골라져 있다\n'
            '2. 제목 칸이 "제출 전 서류 검토 W90VN926RA081" 이다\n'
            '3. 내용 칸에 B, D, E 서류 이름이 있다\n'
            '4. 폼 위에 초록 줄 "문의 유형과 제목을 채워 두었습니다" 가 보인다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-016', flow='입찰 건별 검사', kind='정상', title='공고 분석 전인 입찰 건별 검사는 요구 서류 대신 안내를 보인다',
     pre='고객 계정, 공고에 요구 항목이 없는 입찰 건 1건',
     steps='1. /documents/pre-check/{그 입찰 건 id} 를 연다',
     expect='1. 요구 서류 자리에 "공고 분석 전이라 요구 서류를 알 수 없습니다" 와 [제출 전 서류 검토 신청] 이 있다\n'
            '2. 건별 조회 응답의 analysisReady 가 false 다',
     judge=['가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-017', flow='입찰 건별 검사', kind='정상', title='고객이 올릴 항목이 없으면 그 안내와 플랫폼이 만드는 서류만 보인다',
     pre='고객 계정, 요구 항목이 모두 플랫폼 작성·정부 양식인 공고의 입찰 건 1건',
     steps='1. 그 입찰 건별 검사를 연다',
     expect='1. 올릴 서류 자리에 "이 공고는 고객이 올릴 서류가 없습니다" 가 보인다\n'
            '2. 플랫폼이 만드는 서류 칸에 항목마다 "올리지 않아도 됩니다" 가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-018', flow='입찰 건별 검사', kind='정상', title='입찰 건 상세 제출 서류 탭이 같은 요구 서류 목록을 쓴다',
     pre='고객 계정, 서류 대기 입찰 건 1건(필수 2 중 올림 1)',
     steps='1. 입찰 건 상세(/proposals/{id})의 제출 서류 탭을 연다\n2. [제출 전 검사 보기] 를 누른다',
     expect='1. 1번 탭에 요구 서류 줄이 입찰 건별 검사와 같은 순서, 같은 판정 꼬리표로 있다\n'
            '2. 1번 탭 요약이 "필수 2 중 올림 1" 이다\n'
            '3. 2번에서 /documents/pre-check/{같은 입찰 건 id} 가 열린다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/proposals/{id}', pri='Medium'),

dict(id='IT-08-019', flow='입찰 건별 검사', kind='정상', title='서류 대기가 아닌 입찰 건은 판정만 보이고 올리기 단추가 없다',
     pre='고객 계정, 분석 중(ANALYZING) 입찰 건 1건',
     steps='1. 그 입찰 건별 검사를 연다',
     expect='1. 요구 서류 줄마다 판정 꼬리표가 있다\n'
            '2. [올리기], [서류함에서 고르기], [바꾸기], [빼기] 가 하나도 없다\n'
            '3. [서류 제출 완료] 가 없다\n'
            '4. 건별 조회 응답의 editable 이 false 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-020', flow='입찰 건별 검사', kind='예외', title='남의 입찰 건이나 없는 건은 찾을 수 없다고 답한다',
     pre='고객 계정 A 와 B, B 의 입찰 건 1건',
     steps='1. A 로 /documents/pre-check/{B 의 입찰 건 id} 를 연다\n2. [제출 전 검사 목록] 을 누른다\n3. A 로 /documents/pre-check/{없는 id} 를 연다',
     expect='1. 1번에서 "입찰 건을 찾을 수 없습니다" 가 보이고 B 건의 제목과 요구 서류가 없다\n'
            '2. 1번 건별 조회 응답이 404 다\n'
            '3. 2번에서 /documents/pre-check 가 열린다\n'
            '4. 3번에서 "입찰 건을 찾을 수 없습니다" 가 보인다',
     judge=['가상', '서버', '가상', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-021', flow='입찰 건별 검사', kind='예외', title='건별 검사를 404 아닌 오류로 못 불러오면 다시 시도를 준다',
     pre='고객 계정, 내 입찰 건 1건, 건별 조회가 서버 오류로 답하는 상태',
     steps='1. 그 입찰 건별 검사를 연다\n2. 조회가 되살아난 뒤 [다시 시도] 를 누른다',
     expect='1. 1번에서 "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 2번 뒤 요구 서류 줄과 요약 "필수 n 중 올림 n" 이 채워진다',
     judge=['가상', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-022', flow='서류 올리기·고르기', kind='정상', title='빠진 필수 항목에 PDF 를 올리면 쪽수를 세고 요약을 다시 센다',
     pre='고객 계정, 서류 대기 입찰 건의 빠진 필수 항목 B, 올릴 3쪽 PDF 파일',
     steps='1. B 줄의 [올리기] 를 누르고 3쪽 PDF 를 고른다\n2. 올리기가 끝난 뒤 화면을 본다',
     expect='1. 1번 뒤 B 줄 단추가 꺼지고 "올리는 중" 이 보인다\n'
            '2. 2번에서 B 줄 올린 파일에 그 파일 이름, 형식 PDF, 3쪽, 올린 날이 있고 서류 판정이 "올림" 이다\n'
            '3. 2번에서 머리 요약의 올림이 1 늘고 빠짐이 1 준다\n'
            '4. 입찰 건 서류(client_documents)에 1행이 생기고 page_count 가 3 이다\n'
            '5. B 와 그 서류의 연결이 FULFILLED 로 생긴다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-023', flow='서류 올리기·고르기', kind='정상', title='PDF 가 아니거나 쪽수를 못 세는 파일은 쪽수를 비워 둔다',
     pre='고객 계정, 서류 대기 입찰 건의 쪽수 조건 "5 pages" 인 항목 C 와 D, DOCX 파일과 쪽수를 읽을 수 없는 PDF 파일',
     steps='1. C 줄에 DOCX 파일을 올린다\n2. D 줄에 쪽수를 읽을 수 없는 PDF 를 올린다',
     expect='1. C 와 D 의 입찰 건 서류 page_count 가 비어 있다\n'
            '2. C 줄과 D 줄 올린 파일에 쪽수가 없다\n'
            '3. C 줄과 D 줄 쪽수 판정이 회색 테두리 "사람 확인" 이다',
     judge=['서버', '가상', '가상'],
     req=['BID-MKT-023', 'BID-MKT-024'], fc=['BID-MKT-023', 'BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-024', flow='서류 올리기·고르기', kind='정상', title='서류함에서 고른 서류는 입찰 건 서류로 복사되어 이어진다',
     pre='고객 계정, 서류 대기 입찰 건의 빠진 항목 B, 내 서류함에 서류 3건(1건은 만료일 지남)',
     steps='1. B 줄의 [서류함에서 고르기] 를 누른다\n2. 창에서 만료되지 않은 서류 하나를 고른다',
     expect='1. 창에 내 서류 3건이 줄마다 이름, 분류, 만료일로 있다\n'
            '2. 만료일이 지난 서류의 만료일이 위험 색이다\n'
            '3. 2번 뒤 B 줄 올린 파일에 고른 서류 파일 이름이 있고 서류 판정이 "올림" 이다\n'
            '4. 새 입찰 건 서류 행이 생기고 분류가 B 의 이름, 파일이 서류함 파일의 복사본이다\n'
            '5. 서류함 서류(member_documents) 행과 파일은 바뀌지 않는다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-025', flow='서류 올리기·고르기', kind='정상', title='바꾸기로 새 파일을 이으면 옛 파일은 입찰 건 서류로 남는다',
     pre='고객 계정, 서류 대기 입찰 건의 항목 A 에 old.pdf 가 이어져 있음',
     steps='1. A 줄의 [바꾸기] 를 누르고 new.pdf 를 고른다',
     expect='1. A 줄 올린 파일 이름이 new.pdf 다\n'
            '2. A 의 연결이 new.pdf 의 입찰 건 서류를 가리킨다\n'
            '3. old.pdf 의 입찰 건 서류 행이 남아 있다',
     judge=['가상', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-026', flow='서류 올리기·고르기', kind='정상', title='빼기는 확인 뒤 연결만 풀고 서류는 남긴다',
     pre='고객 계정, 서류 대기 입찰 건의 필수 항목 A 에 a.pdf 가 이어져 있음',
     steps='1. A 줄의 [빼기] 를 누른다\n2. 확인 창에서 예를 누른다',
     expect='1. 1번에서 확인 창 "이 서류를 이 항목에서 뺍니다" 가 열린다\n'
            '2. 2번 뒤 A 줄 서류 판정이 위험 색 "빠짐" 이고 올린 파일이 없다\n'
            '3. A 의 연결 행이 지워진다\n'
            '4. a.pdf 의 입찰 건 서류 행이 남아 있다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-027', flow='서류 올리기·고르기', kind='정상', title='자동 채우기는 항목 이름으로 분류를 찾아 빈 고객 올림 항목만 채운다',
     pre='고객 계정, 서류 대기 입찰 건에 빈 항목 "재무제표"(고객 올림), 이미 이어진 항목 "사업자등록증"(고객 올림), 플랫폼 작성 항목 "Technical Proposal", 서류함에 재무제표 1건과 사업자등록증 1건',
     steps='1. 자동 채우기(POST /bid-requests/{id}/required-document-slots/auto-fill)를 부른다\n2. 그 입찰 건별 검사를 연다',
     expect='1. "재무제표" 항목에 서류함 재무제표의 복사본이 이어진다\n'
            '2. "사업자등록증" 항목의 연결 서류가 부르기 전과 같다\n'
            '3. "Technical Proposal" 항목에 연결이 생기지 않는다\n'
            '4. 2번 화면 "재무제표" 줄 서류 판정이 "올림" 이다',
     judge=['서버', '서버', '서버', '가상'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-028', flow='서류 올리기·고르기', kind='정상', title='항목 이름에 맞는 분류 낱말이 여럿이면 가장 긴 낱말의 분류를 고른다',
     pre='고객 계정, 서류 대기 입찰 건에 빈 고객 올림 항목 "사업자등록증명원", 서류함에 사업자등록증 1건과 사업자등록증명원 영문 1건',
     steps='1. 자동 채우기를 부른다',
     expect='1. "사업자등록증명원" 항목에 사업자등록증명원(BUSINESS_REGISTRATION_CERTIFICATE) 서류의 복사본이 이어진다\n'
            '2. 사업자등록증(BUSINESS_REGISTRATION) 서류는 이어지지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-035', 'BID-MKT-023'], fc=['BID-MKT-035', 'BID-MKT-023'], screen='', pri='Medium'),

dict(id='IT-08-029', flow='서류 올리기·고르기', kind='정상', title='국문·영문 칸 서류를 분류로 고를 때 영문을 먼저, 없으면 국문을 고른다',
     pre='고객 계정, 입찰 건 X 와 Y 에 빈 고객 올림 항목 "지명원", X 회원 서류함 지명원·회사소개 PPT 칸에 국문과 영문 파일',
     steps='1. X 에서 자동 채우기를 부른다\n2. 서류함 지명원·회사소개 PPT 칸 영문 줄을 지운 뒤 Y 에서 자동 채우기를 부른다',
     expect='1. 1번 뒤 X 의 "지명원" 항목에 영문 파일의 복사본이 이어진다\n'
            '2. 2번 뒤 Y 의 "지명원" 항목에 국문 파일의 복사본이 이어진다',
     judge=['서버', '서버'],
     req=['BID-MKT-035', 'BID-MKT-023'], fc=['BID-MKT-035', 'BID-MKT-023'], screen='', pri='Medium'),

dict(id='IT-08-030', flow='서류 올리기·고르기', kind='정상', title='서류함이 비어 있으면 고르기 창이 서류함으로 안내한다',
     pre='서류함 0건인 고객 계정, 서류 대기 입찰 건의 빠진 항목 B',
     steps='1. B 줄의 [서류함에서 고르기] 를 누른다\n2. 창의 [서류함으로] 를 누른다',
     expect='1. 창 안에 "서류함에 올린 서류가 없습니다" 가 보인다\n'
            '2. 2번에서 공통 서류함(/documents)이 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-031', flow='서류 올리기·고르기', kind='예외', title='50MB 를 넘는 파일은 올리지 않는다',
     pre='고객 계정, 서류 대기 입찰 건의 빠진 항목 B, 51MB 파일',
     steps='1. B 줄의 [올리기] 를 누르고 51MB 파일을 고른다',
     expect='1. B 줄에 "파일 크기가 50MB를 넘습니다" 가 보인다\n'
            '2. B 줄 서류 판정이 "빠짐" 그대로다\n'
            '3. 입찰 건 서류 행이 생기지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium',
     issue='CR-141 제출 전 검사는 "파일 크기가 50MB를 넘습니다", 같은 목록을 쓰는 입찰 상세 옛 시나리오는 토스트 "파일 크기가 50MB를 초과합니다" 다'),

dict(id='IT-08-032', flow='서류 올리기·고르기', kind='예외', title='서류함을 불러오지 못하면 고르기 창 안에 알린다',
     pre='고객 계정, 서류 대기 입찰 건, 서류함 조회(GET /member-documents)가 오류로 답하는 상태',
     steps='1. 빠진 항목 줄의 [서류함에서 고르기] 를 누른다',
     expect='1. 창 안에 "서류함을 불러오지 못했습니다" 가 보인다',
     judge=['가상'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-033', flow='서류 올리기·고르기', kind='예외', title='올리기, 고르기, 빼기 저장이 실패하면 줄이 누르기 전으로 돌아간다',
     pre='고객 계정, 서류 대기 입찰 건의 항목 A 에 a.pdf 가 이어져 있음, 빼기가 오류로 답하는 상태',
     steps='1. A 줄의 [빼기] 를 누르고 확인 창에서 예를 누른다',
     expect='1. A 줄 아래 "저장하지 못했습니다" 와 서버 메시지가 보인다\n'
            '2. A 줄 올린 파일이 a.pdf 그대로이고 서류 판정이 "올림" 이다',
     judge=['가상', '가상'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-034', flow='서류 올리기·고르기', kind='예외', title='남의 서류함 서류는 고르지 못한다',
     pre='고객 계정 A 와 B, A 의 서류 대기 입찰 건의 빠진 항목, B 의 서류함 서류 E',
     steps='1. A 토큰으로 서류함에서 고르기(POST .../required-document-slots/{항목 id}/from-vault, memberDocumentId 는 E)를 보낸다',
     expect='1. 응답이 404 다\n'
            '2. 그 항목에 연결과 입찰 건 서류가 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='', pri='Medium'),

dict(id='IT-08-035', flow='서류 올리기·고르기', kind='예외', title='남의 입찰 건에는 올리기, 빼기, 자동 채우기를 하지 못한다',
     pre='고객 계정 A 와 B, B 의 서류 대기 입찰 건(항목 A1 에 서류가 이어짐)',
     steps='1. A 토큰으로 B 입찰 건 항목에 올리기(POST .../upload)를 보낸다\n2. A 토큰으로 B 입찰 건 A1 빼기(DELETE .../required-document-slots/{A1 id})를 보낸다\n3. A 토큰으로 B 입찰 건 자동 채우기를 부른다',
     expect='1. 1번 응답이 404 이고 입찰 건 서류가 생기지 않는다\n'
            '2. 2번 응답이 404 이고 A1 연결이 그대로다\n'
            '3. 3번 응답이 404 다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='', pri='Medium'),

dict(id='IT-08-036', flow='서류 올리기·고르기', kind='예외', title='서류 접수로 넘긴 뒤에는 잇기, 바꾸기, 빼기를 받지 않는다',
     pre='고객 계정, 서류 접수(DOCS_RECEIVED) 입찰 건, 항목 A 에 서류가 이어짐, 빈 항목 C',
     steps='1. 그 입찰 건별 검사를 연다\n2. C 에 올리기(POST .../upload)를 보낸다\n3. C 에 서류함에서 고르기를 보낸다\n4. A 빼기를 보낸다\n5. 자동 채우기를 부른다',
     expect='1. 1번 화면에 [올리기], [서류함에서 고르기], [바꾸기], [빼기] 가 없다\n'
            '2. 2번부터 5번 응답이 모두 409 다\n'
            '3. A 연결과 C 의 빈 상태가 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-MKT-023'], fc=['BID-MKT-023'], screen='/documents/pre-check/{입찰 건}', pri='Medium',
     issue='T3-2 editable 은 접수 전(CREATED)도 true 라 올리기를 받는데 T3-4 는 서류 대기일 때만 올리기를 보인다'),

dict(id='IT-08-037', flow='형식 판정', kind='정상', title='형식 원문이 형식 이름뿐이고 확장자가 그 안에 있으면 형식 맞음이다',
     pre='고객 계정, 서류 대기 입찰 건에 형식 "PDF/Word" 인 항목 A 와 형식 "docx" 인 항목 B',
     steps='1. A 에 a.pdf, B 에 b.DOCX 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 형식 판정이 성공 색 "형식 맞음" 이고 근거가 "PDF/Word 에서 PDF, Word 를 읽음" 이다\n'
            '2. B 줄 형식 판정이 성공 색 "형식 맞음" 이다\n'
            '3. 건별 조회 응답에서 A 의 format 이 verdict MATCH, basis "PDF/Word", allowed PDF·WORD 다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-038', flow='형식 판정', kind='정상', title='확장자가 형식 목록 밖이면 형식 어긋남이고 경고로만 센다',
     pre='고객 계정, 서류 대기 입찰 건에 형식 "PDF" 인 필수 항목 A(필수는 A 하나)',
     steps='1. A 에 a.xlsx 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 형식 판정이 주의 색 "형식 어긋남" 이다\n'
            '2. 머리 요약 어긋남이 1 이다\n'
            '3. [서류 제출 완료] 가 활성이다\n'
            '4. 건별 조회 응답의 canTransition 이 true 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-039', flow='형식 판정', kind='정상', title='형식 원문에 다른 낱말이 섞이면 사람 확인이다',
     pre='고객 계정, 서류 대기 입찰 건에 형식 "Excel sheet (PDF로 제출)" 인 항목 A 와 형식 "PDF (인증서, 이력서)" 인 항목 B',
     steps='1. A 와 B 에 PDF 파일을 올린다\n2. A 줄 판정을 본다',
     expect='1. A 줄과 B 줄 형식 판정이 회색 테두리 "사람 확인" 이다\n'
            '2. A 줄 판정 아래 "공고 조건이 형식 이름(쪽수 하나)으로 읽히지 않아 사람이 봅니다" 가 보인다\n'
            '3. 머리 요약 사람 확인이 2 다\n'
            '4. 건별 조회 응답에서 A 의 format.verdict 가 NEEDS_HUMAN 이고 allowed 가 빈 배열이다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-040', flow='형식 판정', kind='정상', title='형식 원문이 없으면 형식 조건 없음이다',
     pre='고객 계정, 서류 대기 입찰 건에 형식 원문이 빈 항목 A',
     steps='1. A 에 파일을 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 공고 조건 자리에 "조건 없음" 이 있다\n'
            '2. A 줄 형식 판정이 흐린 글 "형식 조건 없음" 이다',
     judge=['가상', '가상'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-041', flow='쪽수 판정', kind='정상', title='쪽수 원문이 숫자 하나와 쪽 단위면 한도 이하 PDF 는 맞음이다',
     pre='고객 계정, 서류 대기 입찰 건에 쪽수 "1 page" 인 항목 A, "10 페이지" 인 항목 B, "5쪽" 인 항목 C, 설정 PRE_CHECK_PAGE_UNITS 처음 값',
     steps='1. A 에 1쪽 PDF, B 에 10쪽 PDF, C 에 4쪽 PDF 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 쪽수 판정이 성공 색 "1쪽, 맞음" 이고 근거가 "1 page 에서 1쪽을 읽음" 이다\n'
            '2. B 줄 쪽수 판정이 "10쪽, 맞음" 이다\n'
            '3. C 줄 쪽수 판정이 "4쪽, 맞음" 이다\n'
            '4. 건별 조회 응답에서 A, B, C 의 pages.limit 이 1, 10, 5 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-042', flow='쪽수 판정', kind='정상', title='한도를 넘는 PDF 는 넘음이고 경고로만 센다',
     pre='고객 계정, 서류 대기 입찰 건에 쪽수 "1 page" 인 필수 항목 A(필수는 A 하나)',
     steps='1. A 에 2쪽 PDF 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 쪽수 판정이 주의 색 "2쪽, 한도 1쪽 넘음" 이다\n'
            '2. 머리 요약 어긋남이 1 이다\n'
            '3. [서류 제출 완료] 가 활성이다\n'
            '4. 건별 조회 응답에서 A 의 pages.verdict 가 OVER 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-043', flow='쪽수 판정', kind='정상', title='숫자가 여럿이거나 합계 조건이면 사람 확인이다',
     pre='고객 계정, 서류 대기 입찰 건에 쪽수 "10 pages, resume 2 pages each" 인 항목 A 와 "total not to exceed 20 pages including appendix" 인 항목 B',
     steps='1. A 와 B 에 3쪽 PDF 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄과 B 줄 쪽수 판정이 회색 테두리 "사람 확인" 이다\n'
            '2. 건별 조회 응답에서 A 의 pages.limit 이 null 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-044', flow='쪽수 판정', kind='정상', title='쪽수 원문에 숫자가 없으면 쪽수 조건 없음이다',
     pre='고객 계정, 서류 대기 입찰 건에 쪽수 "concise" 인 항목 A',
     steps='1. A 에 PDF 를 올린다\n2. 입찰 건별 검사를 본다',
     expect='1. A 줄 쪽수 판정이 흐린 글 "쪽수 조건 없음" 이다',
     judge=['가상'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-045', flow='쪽수 판정', kind='정상', title='쪽 단위 설정이 비어 있으면 쪽수 조건이 모두 사람 확인이다',
     pre='고객 계정, 서류 대기 입찰 건에 쪽수 "1 page" 인 항목 A 와 1쪽 PDF 를 올려 "1쪽, 맞음" 인 상태',
     steps='1. 설정 PRE_CHECK_PAGE_UNITS 값을 비운다\n2. 입찰 건별 검사를 새로고침한다',
     expect='1. A 줄 쪽수 판정이 회색 테두리 "사람 확인" 이다\n'
            '2. 건별 조회 응답에서 A 의 pages.verdict 가 NEEDS_HUMAN 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-024'], fc=['BID-MKT-024'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-046', flow='서류 접수 전환', kind='정상', title='필수 서류가 빠지면 서류 제출 완료가 꺼지고 빠진 이름을 보인다',
     pre='고객 계정, 서류 대기 입찰 건, 필수 고객 올림 항목 B, C 를 안 올림',
     steps='1. 그 입찰 건별 검사를 연다',
     expect='1. [서류 제출 완료] 가 비활성이다\n'
            '2. 단추 위에 "필수 서류 2개가 빠졌습니다: B, C" 가 보인다\n'
            '3. 건별 조회 응답의 canTransition 이 false 다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-047', flow='서류 접수 전환', kind='정상', title='필수 서류가 다 있으면 경고가 있어도 서류 접수로 넘긴다',
     pre='고객 계정, 서류 대기 입찰 건, 필수 고객 올림 항목을 모두 올림, 형식 어긋남 1개와 사람 확인 1개',
     steps='1. 그 입찰 건별 검사를 연다\n2. [서류 제출 완료] 를 누르고 확인 창에서 확인한다\n3. /documents/pre-check 를 연다',
     expect='1. 1번에서 [서류 제출 완료] 가 활성이다\n'
            '2. 1번 단추 위에 "형식·쪽수가 공고 조건과 다르거나 사람이 볼 서류가 2개 있습니다. 그대로 넘기면 담당자가 확인합니다" 가 보인다\n'
            '3. 2번 뒤 머리 진행 단계가 서류 접수로 바뀐다\n'
            '4. 2번 뒤 [올리기], [서류함에서 고르기], [바꾸기], [빼기], [서류 제출 완료] 가 사라진다\n'
            '5. 입찰 건 상태가 DOCS_RECEIVED 로 바뀌고 상태 이력에 DOCS_PENDING 에서 DOCS_RECEIVED 행이 남는다\n'
            '6. 3번 목록에서 그 줄 진행 단계가 서류 접수다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium',
     issue='T3-2 는 PATCH /bid-requests/{id}/state 를 "고객: 취소만" 이라 하나 CR-141 은 고객이 이 요청으로 서류 접수로 넘긴다'),

dict(id='IT-08-048', flow='서류 접수 전환', kind='정상', title='필수 아닌 항목과 플랫폼 항목이 비어도 전환을 막지 않는다',
     pre='고객 계정, 서류 대기 입찰 건, 필수 고객 올림 항목은 모두 올림, 필수 아닌 고객 올림 항목 1개와 플랫폼 작성 항목 1개는 비어 있음',
     steps='1. 그 입찰 건별 검사를 연다\n2. [서류 제출 완료] 를 누르고 확인한다',
     expect='1. 1번에서 [서류 제출 완료] 가 활성이다\n'
            '2. 2번 뒤 입찰 건 상태가 DOCS_RECEIVED 다',
     judge=['가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-049', flow='서류 접수 전환', kind='정상', title='요구 항목이 없는 공고의 입찰 건은 전환을 막지 않는다',
     pre='고객 계정, 공고 분석 전이라 요구 항목이 0개인 서류 대기 입찰 건',
     steps='1. 상태 전이(PATCH /bid-requests/{id}/state, DOCS_RECEIVED)를 보낸다',
     expect='1. 응답이 성공이고 입찰 건 상태가 DOCS_RECEIVED 다',
     judge=['서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='', pri='Medium'),

dict(id='IT-08-050', flow='서류 접수 전환', kind='정상', title='서류 대기가 아닌 입찰 건에는 서류 제출 완료 단추가 없다',
     pre='고객 계정, 접수 전(CREATED) 입찰 건과 서류 접수(DOCS_RECEIVED) 입찰 건',
     steps='1. 두 입찰 건별 검사를 차례로 연다',
     expect='1. 두 화면 모두 [서류 제출 완료] 가 없다\n'
            '2. 두 건의 건별 조회 응답 canTransition 이 false 다',
     judge=['가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-051', flow='서류 접수 전환', kind='예외', title='필수 서류가 빠진 채 전환하면 409 로 막고 빠진 이름을 보인다',
     pre='고객 계정, 서류 대기 입찰 건, 필수 항목 A, B 를 올려 [서류 제출 완료] 가 켜진 화면을 열어 둠',
     steps='1. 다른 창에서 B 를 뺀다\n2. 열어 둔 화면에서 [서류 제출 완료] 를 누르고 확인한다',
     expect='1. 2번 응답이 409 REQUIREMENT_SLOTS_NOT_FULFILLED 이고 unfulfilledSlots 에 B 가 있다\n'
            '2. 단추 위에 빠진 서류 이름 B 가 보인다\n'
            '3. 건별 검사를 다시 불러 B 줄 서류 판정이 "빠짐" 이고 [서류 제출 완료] 가 비활성이다\n'
            '4. 입찰 건 상태가 DOCS_PENDING 그대로다',
     judge=['서버', '가상', '가상', '서버'],
     req=['BID-MKT-022'], fc=['BID-MKT-022'], screen='/documents/pre-check/{입찰 건}', pri='Medium'),

dict(id='IT-08-052', flow='서류함 고정 칸', kind='정상', title='공통 서류함에 고정 칸 다섯이 정한 순서와 안내로 보인다',
     pre='고객 계정, 서류함 0건',
     steps='1. 고객 계정으로 로그인해 메뉴 서류함 > 공통 서류함 을 누른다',
     expect='1. 주소가 /documents 이고 왼쪽 서류함 묶음 메뉴에서 공통 서류함이 켜져 있다\n'
            '2. 머리에 "사업자등록증·지명원 등 공통 서류를 한 번 등록해 두면 제안서 작성 시 자동으로 채워집니다" 가 보인다\n'
            '3. 그 아래 "CAGE 코드와 SAM 등록증은 등록 대행으로 받는 서류라 여기에 올리지 않습니다" 가 보인다\n'
            '4. 고정 칸이 사업자등록증, 사업자등록증명원, 지명원·회사소개 PPT, 재무제표, 인허가증 순서로 다섯이다\n'
            '5. 사업자등록증명원 칸에 "3개월 안에 뗀 것", 지명원·회사소개 PPT 칸에 "원본 파일", 재무제표 칸에 "최근 3개년", 인허가증 칸에 "사업 관련 인허가증" 이 있다\n'
            '6. 사업자등록증명원과 지명원·회사소개 PPT 칸은 국문 줄과 영문 줄 두 줄이다\n'
            '7. 사업자등록증, 재무제표, 인허가증 칸은 언어 없는 줄 하나다\n'
            '8. 고정 칸에 CAGE 증빙, SAM 등록증, 과거 수행실적 칸이 없다\n'
            '9. [기타 서류 추가] 단추가 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-053', flow='서류함 고정 칸', kind='정상', title='국문 줄과 영문 줄에 따로 올리면 언어별로 하나씩 저장된다',
     pre='고객 계정, 사업자등록증명원 칸 두 줄이 빈 상태',
     steps='1. 사업자등록증명원 칸 국문 줄에서 [올리기] 로 ko.pdf 를 올린다\n2. 같은 칸 영문 줄에서 [올리기] 로 en.pdf 를 올린다',
     expect='1. 1번 뒤 국문 줄에 ko.pdf 가 있고 영문 줄은 빈 줄 그대로다\n'
            '2. 2번 뒤 영문 줄에 en.pdf 가 있고 국문 줄은 ko.pdf 그대로다\n'
            '3. 서류함 서류에 분류 BUSINESS_REGISTRATION_CERTIFICATE 행이 2행이고 언어가 KO 하나, EN 하나다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-054', flow='서류함 고정 칸', kind='정상', title='같은 언어로 다시 올리거나 바꾸면 그 언어 파일만 바뀐다',
     pre='고객 계정, 지명원·회사소개 PPT 칸 국문 줄 ko1.pptx, 영문 줄 en1.pptx',
     steps='1. 국문 줄에서 ko2.pptx 로 다시 올린다\n2. 영문 줄의 [바꾸기] 로 en2.pptx 를 고른다',
     expect='1. 1번 뒤 국문 줄이 ko2.pptx, 영문 줄이 en1.pptx 다\n'
            '2. 2번 뒤 영문 줄이 en2.pptx, 국문 줄이 ko2.pptx 다\n'
            '3. 서류함 서류에 그 분류 행이 2행 그대로이고 두 행의 분류와 언어(KO, EN)가 바뀌지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-055', flow='서류함 고정 칸', kind='정상', title='국문 줄을 지우면 영문 줄은 남는다',
     pre='고객 계정, 사업자등록증명원 칸 국문 줄과 영문 줄 모두 파일 있음',
     steps='1. 국문 줄의 [지우기] 를 누르고 확인한다',
     expect='1. 국문 줄이 빈 줄이다\n'
            '2. 영문 줄 파일이 그대로 있다\n'
            '3. 서류함 서류에서 그 분류 KO 행만 지워지고 EN 행은 남는다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-056', flow='서류함 고정 칸', kind='정상', title='언어 없는 칸에 온 언어 값은 무시한다',
     pre='고객 계정, 재무제표 칸 빈 상태',
     steps='1. 서류함 올리기(POST /member-documents)를 category FINANCIAL_STATEMENT, language EN 으로 보낸다\n2. /documents 를 연다',
     expect='1. 1번이 성공하고 저장된 행의 언어가 비어 있다\n'
            '2. 2번 재무제표 칸이 줄 하나이고 올린 파일이 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-057', flow='서류함 고정 칸', kind='정상', title='고정 칸에서 빠진 옛 서류는 기타 서류로 옮겨지고 원래 칸 이름이 남는다',
     pre='V51 적용 전 CAGE 증빙, SAM 등록증, 과거 수행실적 칸에 파일을 올려 둔 고객 계정',
     steps='1. V51 을 적용한다\n2. 그 계정의 서류함 서류를 조회한다\n3. 그 계정으로 /documents 를 연다',
     expect='1. 세 서류의 분류가 OTHER 로 바뀌고 제목이 원래 칸 이름 그대로다\n'
            '2. 3번 기타 서류 목록에 세 서류가 원래 칸 이름으로 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-058', flow='서류함 고정 칸', kind='정상', title='기타 서류 추가는 제목과 파일을 받아 기타 분류로 쌓인다',
     pre='고객 계정',
     steps='1. /documents 에서 [기타 서류 추가] 를 누른다\n2. 제목 "ISO 9001 인증서", 파일, 설명을 넣고 등록한다\n3. 같은 방법으로 1건 더 등록한다',
     expect='1. 1번 창에 문서 제목(필수), 파일(필수), 설명(선택) 칸이 있다\n'
            '2. 3번 뒤 기타 서류 목록에 2줄이 있고 줄마다 [바꾸기] 와 [지우기] 가 있다\n'
            '3. 서류함 서류에 분류 OTHER 행 2행이 제목, 설명 값으로 생긴다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-059', flow='서류함 고정 칸', kind='정상', title='다른 회원의 서류는 내 서류함에 섞이지 않는다',
     pre='고객 계정 A(서류 2건)와 B(서류 3건)',
     steps='1. A 로 /documents 를 연다\n2. A 토큰으로 B 의 서류 지우기(DELETE /member-documents/{B 서류 id})를 보낸다',
     expect='1. 1번 화면 서류가 A 의 2건뿐이다\n'
            '2. 2번 응답이 403 이나 404 이고 B 의 서류 행이 남아 있다',
     judge=['가상', '서버'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

dict(id='IT-08-060', flow='서류함 고정 칸', kind='예외', title='국문·영문 칸에 언어 없이 올리면 400 으로 막는다',
     pre='고객 계정, 사업자등록증명원 칸 빈 상태',
     steps='1. 서류함 올리기(POST /member-documents)를 category BUSINESS_REGISTRATION_CERTIFICATE, language 없이 보낸다\n2. 화면에서 서버가 400 으로 답하게 한 뒤 사업자등록증명원 칸에서 올린다',
     expect='1. 1번 응답이 400 이다\n'
            '2. 서류함 서류 행이 생기지 않는다\n'
            '3. 2번 화면에 언어를 고르라는 안내가 보이고 국문 줄과 영문 줄이 빈 줄 그대로다',
     judge=['서버', '서버', '가상'],
     req=['BID-MKT-035'], fc=['BID-MKT-035'], screen='/documents', pri='Medium'),

]
