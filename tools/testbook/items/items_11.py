# 영역 11 컴플라이언스 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19, T1-3 업무 규칙, T1-5 상태 정의, T3-1 데이터 모델, T3-2 API 설계, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-11-001', flow='고객 서류 충족 매핑', kind='정상', title='고객이 요구 서류를 올리면 고객 서류 충족 매핑이 생기고 빼면 매핑만 지워진다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING, 공고 분석이 끝나 필수 고객 올림 요구 항목이 있음), 50MB 이하 PDF 파일 1개',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. "빠짐" 인 필수 서류 줄의 [올리기] 를 누르고 PDF 를 고른다\n3. 같은 줄의 [빼기] 를 누르고 확인 창에서 예를 누른다',
     expect='1. 2단계 뒤 그 줄의 서류 판정이 "올림" 이다\n'
            '2. 2단계 뒤 requirement_fulfillment_maps 에 그 입찰 건과 그 요구 항목의 행이 fulfillment_type CLIENT_DOCUMENT, status FULFILLED 로 있다\n'
            '3. 3단계의 확인 창에 "이 서류를 이 항목에서 뺍니다" 가 보인다\n'
            '4. 3단계 뒤 그 줄의 서류 판정이 "빠짐" 이다\n'
            '5. 3단계 뒤 그 충족 매핑 행이 지워지고 2단계에서 올린 고객 서류 행은 남아 있다',
     judge=['가상', '서버', '가상', '가상', '서버'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-002', flow='제안서 절 충족 매핑', kind='정상', title='절 검증이 답했다고 낸 REQ 는 충족, 답하지 않은 REQ 는 누락으로 남는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001, REQ-002 가 있는 공고의 입찰 건과 제안서 문서, 절 A 의 대응 REQ ID 가 REQ-001, REQ-002 이고 충족 매핑 행이 아직 없음',
     steps='1. MCP save_verification_result 를 절 A 대상으로 requirementChecks(REQ-001 answered true, REQ-002 answered false)를 실어 부른다\n2. 그 입찰 건의 충족 매핑 행을 조회한다\n3. 관리자 화면 /documents/{documentId}/sections 를 열어 절 A 를 고른다',
     expect='1. req_id 가 REQ-001 인 요구 항목의 충족 매핑 행이 fulfillment_type DOCUMENT_SECTION, document_id 그 제안서 문서, document_section_path 절 A id, status FULFILLED 로 있다\n'
            '2. req_id 가 REQ-002 인 요구 항목의 충족 매핑 행이 fulfillment_type DOCUMENT_SECTION, document_section_path 절 A id, status MISSING 으로 있다\n'
            '3. 우측 절 상세의 대응 REQ ID 에서 REQ-001 은 충족, REQ-002 는 누락으로 보인다\n'
            '4. 좌측 트리의 절 A 옆에 충족 수 1 / 전체 수 2 가 보인다',
     judge=['서버', '서버', '가상', '가상'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-003', flow='제안서 절 충족 매핑', kind='정상', title='같은 절을 다시 검증해 답하지 않음이 나오면 충족이 누락으로 바뀐다',
     pre='관리자 계정과 MCP 열쇠, IT-11-002 를 마친 상태(REQ-001 이 절 A 로 FULFILLED)',
     steps='1. MCP save_verification_result 를 절 A 대상으로 requirementChecks(REQ-001 answered false, REQ-002 answered false)를 실어 부른다\n2. 그 입찰 건의 충족 매핑 행을 조회한다\n3. 관리자 화면 /documents/{documentId}/sections 를 새로고침해 절 A 를 고른다',
     expect='1. req_id 가 REQ-001 인 요구 항목의 충족 매핑 행이 document_section_path 절 A id, status MISSING 으로 바뀐다\n'
            '2. 우측 절 상세의 대응 REQ ID 에서 REQ-001 이 누락으로 보인다',
     judge=['서버', '가상'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-004', flow='제안서 절 충족 매핑', kind='예외', title='다른 절이 충족으로 둔 매핑은 뒤 절의 답하지 않음으로 바뀌지 않는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001 이 있는 공고의 제안서 문서, 절 A 와 절 B 가 모두 대응 REQ ID 로 REQ-001 을 듦',
     steps='1. MCP save_verification_result 를 절 A 대상으로 REQ-001 answered true 를 실어 부른다\n2. MCP save_verification_result 를 절 B 대상으로 REQ-001 answered false 를 실어 부른다\n3. 그 입찰 건의 충족 매핑 행을 조회한다',
     expect='1. req_id 가 REQ-001 인 요구 항목의 충족 매핑 행이 하나이고 document_section_path 절 A id, status FULFILLED 그대로이다',
     judge=['서버'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='', pri='Medium'),

dict(id='IT-11-005', flow='제안서 절 충족 매핑', kind='예외', title='절의 대응 REQ ID 가 아닌 번호와 매트릭스에 없는 번호는 버려진다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001, REQ-002 가 있는 공고의 제안서 문서, 절 A 의 대응 REQ ID 가 REQ-001 뿐이고 REQ-002 충족 매핑 행이 없음',
     steps='1. MCP save_verification_result 를 절 A 대상으로 requirementChecks(REQ-001 answered true, REQ-002 answered true, REQ-999 answered true)를 실어 부른다\n2. 그 입찰 건의 충족 매핑 행을 조회한다',
     expect='1. req_id 가 REQ-001 인 요구 항목의 충족 매핑 행이 절 A id, status FULFILLED 로 있다\n'
            '2. req_id 가 REQ-002 인 요구 항목의 충족 매핑 행이 생기지 않는다\n'
            '3. REQ-999 에 해당하는 충족 매핑 행이 생기지 않는다',
     judge=['서버', '서버', '서버'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='', pri='Medium'),

dict(id='IT-11-006', flow='제안서 절 충족 매핑', kind='예외', title='절 검증 저장은 고객 서류 충족 매핑을 건드리지 않는다',
     pre='관리자 계정과 MCP 열쇠, 고객 서류 충족 매핑(CLIENT_DOCUMENT, FULFILLED) 행이 있는 입찰 건, 그 입찰 건의 제안서 문서에 대응 REQ ID REQ-001 을 든 절 A',
     steps='1. 그 입찰 건의 CLIENT_DOCUMENT 충족 매핑 행을 적어 둔다\n2. MCP save_verification_result 를 절 A 대상으로 REQ-001 answered false 를 실어 부른다\n3. 그 입찰 건의 충족 매핑 행을 다시 조회한다',
     expect='1. CLIENT_DOCUMENT 충족 매핑 행의 수와 각 행의 status, 고객 서류 연결이 1단계와 같다',
     judge=['서버'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='', pri='Medium'),

dict(id='IT-11-007', flow='제안서 절 충족 매핑', kind='정상', title='제안서 설계를 다시 저장하면 그 문서의 제안서 절 충족 매핑이 지워지고 새 절 검증이 다시 채운다',
     pre='관리자 계정과 MCP 열쇠, IT-11-002 를 마친 입찰 건(제안서 절 충족 매핑 2행)과 고객 서류 충족 매핑 1행',
     steps='1. MCP save_proposal_structure 를 같은 문서에 절 A2(대응 REQ ID REQ-001, REQ-002)로 다시 부른다\n2. 그 입찰 건의 충족 매핑 행을 조회한다\n3. MCP save_verification_result 를 새 절 A2 대상으로 REQ-001 answered true 를 실어 부른다\n4. 충족 매핑 행을 다시 조회한다',
     expect='1. 2단계에서 그 문서의 fulfillment_type DOCUMENT_SECTION 행이 하나도 없다\n'
            '2. 2단계에서 CLIENT_DOCUMENT 행 1개가 그대로 있다\n'
            '3. 4단계에서 req_id 가 REQ-001 인 요구 항목의 행이 document_section_path 새 절 A2 id, status FULFILLED 로 있다',
     judge=['서버', '서버', '서버'],
     req=['BID-COMP-001'], fc=['BID-COMP-001'], screen='', pri='Medium'),

dict(id='IT-11-008', flow='커버리지 계산', kind='정상', title='커버리지 검증이 Shall 이면서 플랫폼 생성인 줄만 세어 누락과 매핑 안 된 REQ 를 낸다',
     pre='관리자 계정과 MCP 열쇠, 커버리지 준비 문서: 매트릭스 줄 REQ-001 부터 REQ-004 는 Shall 플랫폼 생성, REQ-005 는 Should 플랫폼 생성, REQ-006 은 Shall 고객 올림. 절 A 대응 REQ ID REQ-001, REQ-002, REQ-005 를 모두 답함, 절 B 대응 REQ ID REQ-003 을 답하지 않음, REQ-004 는 어느 절에도 없음',
     steps='1. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/coverage 를 부른다\n2. MCP save_verification_result 를 절 B 대상으로 REQ-003 answered true 를 실어 부른다\n3. 커버리지를 다시 부른다',
     expect='1. 1단계 응답의 matrixReady 가 true, targetCount 가 4, fulfilledCount 가 2 이다\n'
            '2. 1단계 응답의 missing 이 REQ-003(unmapped false), REQ-004(unmapped true) 두 줄이고 줄마다 원문 text 가 있다\n'
            '3. 1단계 응답의 missing 과 targetCount 에 REQ-005, REQ-006 이 들어가지 않는다\n'
            '4. 1단계 응답의 unknownIds 가 빈 배열이다\n'
            '5. 1단계 응답의 sections 에서 절 A 의 refs 가 REQ-001, REQ-002, REQ-005 모두 fulfilled true, 절 B 의 refs 가 REQ-003 fulfilled false 이다\n'
            '6. 3단계 응답의 fulfilledCount 가 3 이고 missing 이 REQ-004 한 줄이다',
     judge=['서버', '서버', '서버', '서버', '서버', '서버'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='', pri='Medium'),

dict(id='IT-11-009', flow='커버리지 계산', kind='정상', title='절이 든 번호가 매트릭스에서 빠지면 매트릭스에 없는 ID 로 낸다',
     pre='관리자 계정과 MCP 열쇠, IT-11-008 과 같은 커버리지 준비 문서',
     steps='1. MCP save_opportunity_analysis 로 그 공고의 매트릭스를 REQ-003 줄만 뺀 채 다시 저장한다\n2. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/coverage 를 부른다\n3. 관리자 화면 /documents/{documentId}/sections 를 열고 커버리지 띠를 펼친다\n4. 매트릭스에 없는 ID 목록의 절 B 를 누른다',
     expect='1. 응답의 unknownIds 가 REQ-003 한 줄이고 sectionId 와 sectionTitle 이 절 B 의 것이다\n'
            '2. 응답의 targetCount 가 3 이고 missing 에 REQ-003 이 없다\n'
            '3. 커버리지 띠가 경고 색이다\n'
            '4. 펼친 띠의 매트릭스에 없는 ID 목록에 REQ-003 과 절 B 제목이 보인다\n'
            '5. 4단계 뒤 절 B 가 골라지고 우측 절 상세에 REQ-003 이 매트릭스에 없는 번호로 따로 보인다',
     judge=['서버', '서버', '가상', '가상', '가상'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /documents/{id}/sections', pri='Medium',
     issue='제안서 설계 저장이 매트릭스에 없는 ID 를 거부하므로(BIZ-076) 매트릭스에 없는 ID 는 공고를 다시 분석해 줄이 빠질 때만 생긴다. 그때 그 REQ 의 충족 매핑 행을 어떻게 하는지 설계에 없다'),

dict(id='IT-11-010', flow='커버리지 계산', kind='정상', title='누락 REQ 가 있어도 입찰 건 상태 전환을 막지 않는다',
     pre='관리자 계정, REVIEW 상태이고 필수 고객 서류가 다 이어진 입찰 건, 그 제안서 문서의 커버리지에 누락 REQ 가 1개 이상 있음',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 커버리지 띠의 누락 REQ 수를 적어 둔다\n2. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n3. [상태 전환] 에서 CONFIRMED 를 골라 전환한다',
     expect='1. 1단계 띠에 누락 REQ 수가 1 이상으로 보인다\n'
            '2. 입찰 건 상태가 CONFIRMED 로 바뀌고 상태 이력에 CONFIRMED 한 줄이 더해진다',
     judge=['가상', '서버'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-11-011', flow='커버리지 계산', kind='예외', title='없는 문서의 커버리지는 404 다',
     pre='관리자 계정 로그인 토큰',
     steps='1. 관리자 토큰으로 GET /admin/bid-documents/{없는 uuid}/coverage 를 부른다',
     expect='1. 응답이 404 이다',
     judge=['서버'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='', pri='Medium'),

dict(id='IT-11-012', flow='커버리지 계산', kind='예외', title='관리자가 아니면 커버리지를 볼 수 없다',
     pre='회원 계정 로그인 토큰, IT-11-008 과 같은 커버리지 준비 문서',
     steps='1. 회원 토큰으로 GET /admin/bid-documents/{documentId}/coverage 를 부른다',
     expect='1. 응답이 403 이고 targetCount, missing 칸이 응답에 없다',
     judge=['서버'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='', pri='Medium'),

dict(id='IT-11-013', flow='커버리지 화면', kind='정상', title='제안서 Section 관리 트리 위 커버리지 띠에서 누락 REQ 를 찾는다',
     pre='관리자 계정, IT-11-008 과 같은 커버리지 준비 문서',
     steps='1. 관리자 화면 문서 편집에서 [Section 관리] 를 눌러 /documents/{documentId}/sections 를 연다\n2. 커버리지 띠를 펼친다',
     expect='1. 트리 위 띠에 충족 수 2, 대상 수 4, 비율 50%, 누락 REQ 2, 매핑 안 된 REQ 1 이 보인다\n'
            '2. 띠가 경고 색이다\n'
            '3. 펼친 띠의 누락 REQ 목록에 REQ-003, REQ-004 가 원문 앞부분과 함께 보인다\n'
            '4. 누락 REQ 목록에서 REQ-004 에만 매핑 안 된 REQ 표시가 붙는다\n'
            '5. 좌측 트리의 절 A 옆에 3 / 3, 절 B 옆에 0 / 1 이 보인다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-COMP-002', 'BID-COMP-001'], fc=['BID-COMP-002', 'BID-COMP-001'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-014', flow='커버리지 화면', kind='정상', title='누락이 없으면 커버리지 띠는 보통 색에 100% 다',
     pre='관리자 계정, 매트릭스의 Shall 플랫폼 생성 줄이 모두 절로 답해진 제안서 문서',
     steps='1. 관리자 화면 /documents/{documentId}/sections 를 연다',
     expect='1. 커버리지 띠에 비율 100% 와 누락 REQ 0 이 보인다\n'
            '2. 띠가 경고 색이 아닌 보통 색이다',
     judge=['가상', '가상'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-015', flow='커버리지 화면', kind='정상', title='매트릭스가 없는 공고의 제안서는 커버리지 검증을 하지 않는다고 알린다',
     pre='관리자 계정, 매트릭스 줄이 없는 공고의 제안서 문서 1건',
     steps='1. 관리자 화면 /documents/{documentId}/sections 를 연다\n2. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/coverage 를 부른다',
     expect='1. 커버리지 띠 자리에 "매트릭스가 없는 공고라 커버리지 검증을 하지 않습니다" 한 줄이 보인다\n'
            '2. 좌측 트리의 절 옆에 충족 수 / 전체 수가 붙지 않는다\n'
            '3. 응답의 matrixReady 가 false 이고 missing, unknownIds, sections 가 비어 있다',
     judge=['가상', '가상', '서버'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-016', flow='커버리지 화면', kind='정상', title='다시 작성이나 다시 검증이 끝나면 커버리지 띠와 트리의 수를 고친다',
     pre='관리자 계정, IT-11-008 과 같은 커버리지 준비 문서, Aimbase 절 작성과 검증 워크플로우가 도는 상태, 브라우저 개발자 도구의 네트워크 기록',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 절 B 를 고른다\n2. [다시 검증] 을 누르고 끝날 때까지 기다린다\n3. [다시 작성] 을 누르고 끝날 때까지 기다린다',
     expect='1. 2단계가 끝난 뒤 GET .../coverage 요청이 한 번 더 나간다\n'
            '2. 3단계가 끝난 뒤 GET .../coverage 요청이 한 번 더 나간다\n'
            '3. 3단계 뒤 띠의 충족 수, 대상 수와 트리의 절 B 수가 마지막 커버리지 응답의 fulfilledCount, targetCount, 절 B refs 와 같다',
     judge=['가상', '가상', '가상'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-017', flow='커버리지 화면', kind='예외', title='커버리지를 불러오지 못하면 띠에만 오류가 보이고 트리는 그대로다',
     pre='관리자 계정, IT-11-008 과 같은 커버리지 준비 문서, 커버리지 요청만 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 /documents/{documentId}/sections 를 연다\n2. 막은 것을 푼 뒤 띠의 [다시 시도] 를 누른다',
     expect='1. 커버리지 띠 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 좌측 트리에 장과 절 이름이 모두 보이고 절을 고를 수 있다\n'
            '3. 2단계 뒤 띠에 충족 수 2, 대상 수 4 가 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-COMP-002'], fc=['BID-COMP-002'], screen='관리자 /documents/{id}/sections', pri='Medium'),

dict(id='IT-11-018', flow='서류 접수 차단', kind='예외', title='필수 고객 서류가 빠지면 서류 접수로 넘길 수 없다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING), 필수 고객 올림 요구 항목 2개 가운데 1개만 올림',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. 회원 토큰으로 PATCH /bid-requests/{id}/state 를 DOCS_RECEIVED 로 부른다\n3. 고객 화면 입찰 상세 /proposals/{id} 의 제출 서류 탭을 연다',
     expect='1. [서류 제출 완료] 가 꺼져 있고 단추 위에 "필수 서류 1개가 빠졌습니다: {빠진 서류 이름}" 이 보인다\n'
            '2. PATCH 응답이 409 이고 error 가 REQUIREMENT_SLOTS_NOT_FULFILLED, unfulfilledSlots 에 빠진 서류 한 줄이 있다\n'
            '3. 입찰 건 상태가 DOCS_PENDING 그대로이고 상태 이력에 줄이 더해지지 않는다\n'
            '4. /proposals/{id} 제출 서류 탭의 제출 완료 단추도 꺼져 있다',
     judge=['가상', '서버', '서버', '가상'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium',
     issue='같은 단추가 T3-4 제출 전 검사에서는 "서류 제출 완료", T3-3 입찰 상세 제출 서류 탭(CR-010 원래 설계)에서는 "문서 제출 완료" 다'),

dict(id='IT-11-019', flow='서류 접수 차단', kind='예외', title='화면을 연 뒤 서류가 빠지면 서류 제출 완료가 409 로 막히고 빠진 서류 이름이 보인다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING, 필수 고객 서류가 다 올라감), 같은 계정으로 연 브라우저 창 2개',
     steps='1. 창 1 에서 그 입찰 건의 제출 전 검사를 연다\n2. 창 2 에서 같은 화면을 열고 필수 서류 한 줄의 [빼기] 를 눌러 뺀다\n3. 창 1 에서 [서류 제출 완료] 를 누르고 확인한다',
     expect='1. 창 1 의 단추 위에 2단계에서 뺀 서류 이름이 보인다\n'
            '2. 창 1 의 그 서류 줄이 "빠짐" 으로 바뀐다\n'
            '3. 입찰 건 상태가 DOCS_PENDING 그대로이다',
     judge=['가상', '가상', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-020', flow='서류 접수 차단', kind='정상', title='필수 고객 서류가 다 있으면 서류 접수로 넘어간다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING), 필수 고객 서류가 다 올라가 있고 형식·쪽수 판정이 모두 맞음',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. [서류 제출 완료] 를 누르고 확인 창에서 확인한다',
     expect='1. 1단계에서 [서류 제출 완료] 가 켜져 있다\n'
            '2. 2단계를 누르면 확인 창이 뜨고 확인 뒤 진행 단계가 서류 접수로 바뀐다\n'
            '3. 입찰 건 상태가 DOCS_RECEIVED 이고 상태 이력에 DOCS_RECEIVED 한 줄이 더해진다\n'
            '4. 2단계 뒤 서류 줄의 올리기 단추가 사라진다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-021', flow='서류 접수 차단', kind='정상', title='형식 어긋남, 쪽수 넘음, 사람 확인은 경고만 하고 막지 않는다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING), 필수 고객 서류가 다 올라가 있고 그 가운데 1개가 형식 어긋남 판정',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. [서류 제출 완료] 를 누르고 확인 창에서 확인한다',
     expect='1. [서류 제출 완료] 가 켜져 있고 위에 "형식·쪽수가 공고 조건과 다르거나 사람이 볼 서류가 1개 있습니다. 그대로 넘기면 담당자가 확인합니다" 가 보인다\n'
            '2. 입찰 건 상태가 DOCS_RECEIVED 로 바뀐다',
     judge=['가상', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-022', flow='서류 접수 차단', kind='정상', title='요구 항목이 없는 공고(분석 전)의 입찰 건은 서류 접수로 넘어간다',
     pre='회원 계정, 사전 분석 전이라 요구 항목이 없는 공고의 자기 입찰 건 1건(DOCS_PENDING)',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. 회원 토큰으로 PATCH /bid-requests/{id}/state 를 DOCS_RECEIVED 로 부른다',
     expect='1. 요구 서류 자리에 "공고 분석 전이라 요구 서류를 알 수 없습니다" 가 보인다\n'
            '2. PATCH 가 받아들여지고 입찰 건 상태가 DOCS_RECEIVED 로 바뀐다',
     judge=['가상', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-023', flow='서류 접수 차단', kind='정상', title='필수라도 플랫폼이 만드는 서류는 서류 접수를 막지 않는다',
     pre='회원 계정, 자기 입찰 건 1건(DOCS_PENDING), 필수 플랫폼 작성 요구 항목 1개와 필수 고객 올림 요구 항목이 있고 고객 올림 서류는 다 올라감',
     steps='1. 고객 화면 메뉴 [서류함] > [제출 전 검사] 에서 그 입찰 건 줄을 누른다\n2. [서류 제출 완료] 를 누르고 확인 창에서 확인한다',
     expect='1. 플랫폼 작성 항목이 플랫폼이 만드는 서류 칸에 "올리지 않아도 됩니다" 와 함께 보인다\n'
            '2. [서류 제출 완료] 가 켜져 있다\n'
            '3. 입찰 건 상태가 DOCS_RECEIVED 로 바뀐다',
     judge=['가상', '가상', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='/documents/pre-check/{bidRequestId}', pri='Medium'),

dict(id='IT-11-024', flow='관리자 전환 차단', kind='예외', title='관리자도 필수 고객 서류가 빠진 입찰 건을 서류 접수로 넘기지 못한다',
     pre='관리자 계정, DOCS_PENDING 상태이고 필수 고객 올림 요구 항목 하나가 빠진 입찰 건 1건',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 DOCS_RECEIVED 를 골라 전환한다',
     expect='1. 전환 요청이 거부되고 응답에 빠진 서류 한 줄이 있다\n'
            '2. 입찰 건 상태가 DOCS_PENDING 그대로이고 상태 이력에 줄이 더해지지 않는다',
     judge=['서버', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='관리자 /bid-requests/{id}', pri='Medium'),

dict(id='IT-11-025', flow='관리자 전환 차단', kind='예외', title='BLOCKER 가 미충족이면 CONFIRMED 로 전환되지 않는다',
     pre='관리자 계정, REVIEW 상태이고 필수 고객 올림 요구 항목 하나의 충족 매핑이 없는 입찰 건 1건(시험 준비로 그 매핑 행을 지워 만든다)',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 CONFIRMED 를 골라 전환한다',
     expect='1. "필수 요구사항이 미충족 상태입니다" 오류 토스트가 뜬다\n'
            '2. 입찰 건 상태가 REVIEW 그대로이고 상태 이력에 줄이 더해지지 않는다',
     judge=['가상', '서버'],
     req=['BID-COMP-003'], fc=['BID-COMP-003'], screen='관리자 /bid-requests/{id}', pri='Medium',
     issue='T3-4 입찰 상세 관리 2-2 는 CONFIRMED 전이를 막는다 하고 BIZ-007 은 없는 상태 READY_FOR_SUBMISSION 을 적으며 T1-5 CR-141 가드는 DOCS_RECEIVED 전이에만 있다. CONFIRMED 전이 가드가 있는지 설계가 정하지 않았다'),

]
