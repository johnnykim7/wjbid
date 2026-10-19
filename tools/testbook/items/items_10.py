# 영역 10 AI 문서 생성/편집 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19, T1-3 업무 규칙, T1-5 상태 정의, T3-1 데이터 모델, T3-2 API 설계, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-10-001', flow='제안서 자동 작성', kind='정상', title='공고 유형에 맞는 성공 제안서가 있는 입찰 건의 제안서를 AI 가 작성한다',
     pre='관리자 계정, 사전 분석 COMPLETED 이고 industry_type 이 있으며 그 유형에 성공 제안서 1건 이상이 있는 공고의 입찰 건(ANALYZING, 고객 서류 접수 완료), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 생성이 끝날 때까지 기다린 뒤 화면을 새로고침한다\n4. 생성 문서의 [편집] 으로 본문을 연다',
     expect='1. 입찰 건 상태가 GENERATING 을 거쳐 REVIEW 가 되고 상태 이력에 GENERATING, REVIEW 두 줄이 더해진다\n'
            '2. 그 입찰 건에 입찰 문서(bid_documents) 행이 상태 DRAFT 로 생기고 버전 1 의 content_json 에 TipTap JSON 본문이 들어 있다\n'
            '3. 입찰 상세 관리 생성 문서 목록에 그 문서가 문서 유형과 상태 초안으로 보인다\n'
            '4. 제안서의 회사명, 실적, 인력, 장비가 고객이 낸 서류의 내용이고 성공 제안서 원본의 회사명이나 실적이 섞이지 않는다\n'
            '5. 제안서 구성과 서술 방식이 그 공고 유형의 성공 제안서 가이드를 따른다\n'
            '6. 제안서가 공고 사전 분석의 문서 양식과 필요 서류를 반영한다',
     judge=['서버', '서버', '가상', '합동', '합동', '합동'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='T1-1 은 "고객이 필요 문서를 제출하면" 자동 작성이라 하고 T1-5 는 CR-141 뒤 ANALYZING 다음 상태로 관리자가 전환한다고 해 생성이 시작되는 때가 어긋난다. Aimbase 가 만드는 최종 Word, PDF 를 어디서 받는지 T3-2, T3-3 에 없다'),

dict(id='IT-10-002', flow='제안서 자동 작성', kind='정상', title='미분류 공고의 입찰 건은 성공 제안서 자산 없이 제안서를 작성한다',
     pre='관리자 계정, 사전 분석 COMPLETED 이고 industry_type 이 NULL 인 공고의 입찰 건(ANALYZING), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 생성이 끝날 때까지 기다린다',
     expect='1. 입찰 건 상태가 REVIEW 가 되고 입찰 문서 행이 상태 DRAFT 로 생긴다\n'
            '2. 문서 생성 워크플로우 입력의 successGuide 와 referenceSamples 가 비어 있다\n'
            '3. 제안서에 성공 제안서 원본의 내용이 섞이지 않는다',
     judge=['서버', '단위', '합동'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-003', flow='제안서 자동 작성', kind='예외', title='사전 분석이 COMPLETED 가 아닌 공고의 입찰 건은 제안서를 작성하지 않는다',
     pre='관리자 계정, 사전 분석이 FAILED 인 공고의 입찰 건(ANALYZING)',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 몇 분 기다린 뒤 그 입찰 건의 입찰 문서를 조회한다',
     expect='1. 그 입찰 건에 입찰 문서 행이 생기지 않는다\n'
            '2. 입찰 건 상태가 REVIEW 로 넘어가지 않는다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='사전 분석이 COMPLETED 가 아닐 때 GENERATING 전환을 막는지, 전환은 되고 생성만 하지 않는지 설계서가 적지 않는다'),

dict(id='IT-10-004', flow='제안서 자동 작성', kind='예외', title='제안서 생성이 실패하면 관리자에게 알린다',
     pre='관리자 계정, 문서 생성 워크플로우가 실패하도록 막은 환경, GENERATING 으로 들어가는 입찰 건 1건',
     steps='1. 그 입찰 건의 제안서 생성을 시작한다',
     expect='1. 알림 이력에 notification_type AI_WORKFLOW_FAILED 인 관리자 알림 행이 1건 더해진다',
     judge=['단위'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-005', flow='제안서 설계와 REQ', kind='정상', title='제안서 설계 저장이 절마다 매트릭스의 REQ ID 를 받고 어느 절에도 없는 REQ ID 를 돌려준다',
     pre='관리자 계정과 MCP 열쇠, Shall 이면서 플랫폼 생성인 매트릭스 줄 REQ-001, REQ-002, REQ-003 이 있는 공고의 제안서 문서 1건',
     steps='1. MCP save_proposal_structure 를 그 문서 id 와 장 1개, 절 2개(절 A 는 REQ-001, 절 B 는 REQ-002)로 부른다\n2. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/chapters 를 부른다\n3. 관리자 화면 /documents/{documentId}/sections 를 열어 절 A 를 고른다',
     expect='1. 저장이 받아들여지고 응답의 unassignedReqIds 에 REQ-003 만 있다\n'
            '2. 장과 절 트리 응답에서 절 A 의 requirementRefs 가 REQ-001, 절 B 의 requirementRefs 가 REQ-002 이다\n'
            '3. 우측 절 상세의 대응 REQ ID 에 REQ-001 이 보인다',
     judge=['서버', '서버', '가상'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-006', flow='제안서 설계와 REQ', kind='예외', title='매트릭스에 없는 REQ ID 를 든 제안서 설계는 전체가 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001 부터 REQ-003 이 있는 공고의 제안서 문서 1건(IT-10-005 로 저장한 트리가 있음)',
     steps='1. 장과 절 트리를 조회해 적어 둔다\n2. MCP save_proposal_structure 를 절 2개(절 A 는 REQ-001, 절 B 는 REQ-999)로 부른다\n3. 장과 절 트리를 다시 조회한다',
     expect='1. 저장이 거부되고 응답에 매트릭스에 없는 ID 로 REQ-999 가 들어 있다\n'
            '2. 3단계의 장과 절 트리가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-007', flow='제안서 설계와 REQ', kind='정상', title='매트릭스가 없는 공고의 제안서 설계는 REQ ID 검사 없이 저장된다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄이 없는 공고의 제안서 문서 1건',
     steps='1. MCP save_proposal_structure 를 절 1개(requirementRefs 에 REQ-001)로 부른다\n2. 장과 절 트리를 조회한다',
     expect='1. 저장이 받아들여진다\n'
            '2. 장과 절 트리에 1단계에서 보낸 절이 들어 있다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-008', flow='제안서 설계와 REQ', kind='정상', title='제안서 설계, 절 작성, 절 검증 입력에 대응 REQ 의 원문, 번역, 요구 동사, 평가항목이 들어 있다',
     pre='관리자 계정과 MCP 열쇠, IT-10-005 로 절 A(REQ-001)가 저장된 제안서 문서',
     steps='1. MCP get_proposal_design_input 을 그 문서 id 로 부른다\n2. MCP get_section_context 를 절 A id 로 부른다\n3. MCP get_section_verify_input 을 절 A id 로 부른다',
     expect='1. 제안서 설계 입력의 complianceMatrix 에 매트릭스 줄마다 reqId, level, text, verb, fulfillmentParty, factorId, factorTitle 이 들어 있다\n'
            '2. 절 작성 입력의 requirements 에 REQ-001 한 줄이 reqId, text, translation, verb, level, factorId, factorTitle 과 함께 들어 있다\n'
            '3. 절 검증 입력의 requirements 가 2단계의 requirements 와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-009', flow='제안서 절 관리', kind='정상', title='제안서 Section 관리에서 장과 절 트리와 절 상세를 본다',
     pre='관리자 계정, 장과 절 트리와 절 본문이 저장된 제안서 문서 1건',
     steps='1. 관리자 화면 문서 편집에서 [Section 관리] 를 눌러 /documents/{documentId}/sections 를 연다\n2. 좌측 트리에서 절 하나를 고른다',
     expect='1. 좌측 트리에 장(평가 항목) 이름 아래 그 장의 절 제목이 저장된 순서대로 보인다\n'
            '2. 트리의 절마다 그 절의 상태와 잠금 여부가 보인다\n'
            '3. 우측 절 상세에 고른 절의 제목, 범위, 대응 REQ ID, 본문 블록, 검증 결과가 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-010', flow='제안서 절 관리', kind='정상', title='절 하나만 다시 작성한다',
     pre='관리자 계정, 잠기지 않은 절이 둘 이상인 제안서 문서 1건, Aimbase 절 작성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 절 A 를 고르고 본문 블록을 적어 둔다\n2. 절 B 의 본문 블록도 적어 둔다\n3. 절 A 에서 [다시 작성] 을 누르고 끝날 때까지 기다린다',
     expect='1. 절 A 의 본문 블록이 1단계와 다른 새 본문으로 바뀐다\n'
            '2. 절 B 의 본문 블록은 2단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-011', flow='제안서 절 관리', kind='정상', title='절 본문은 두고 검증만 다시 한다',
     pre='관리자 계정, 본문과 검증 결과가 있는 절이 있는 제안서 문서 1건, Aimbase 절 검증 워크플로우가 도는 상태',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 그 절을 고르고 본문 블록과 검증 결과를 적어 둔다\n2. [다시 검증] 을 누르고 끝날 때까지 기다린 뒤 절 검증 결과(GET .../sections/{sectionId}/verification)를 부른다',
     expect='1. 그 절의 본문 블록이 1단계와 같다\n'
            '2. 절 검증 결과 응답이 1단계 뒤에 만든 새 검증 결과 1건이다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-012', flow='제안서 절 관리', kind='정상', title='절을 잠그고 잠금을 푼다',
     pre='관리자 계정, 잠기지 않은 절이 있는 제안서 문서 1건',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 그 절을 고른다\n2. [잠금] 을 누른다\n3. [잠금 해제] 를 누른다',
     expect='1. 2단계 뒤 좌측 트리의 그 절에 잠금 표시가 붙는다\n'
            '2. 3단계 뒤 좌측 트리의 그 절에서 잠금 표시가 사라진다\n'
            '3. 3단계 뒤 그 절에 다시 작성 요청이 받아들여진다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-013', flow='제안서 절 관리', kind='예외', title='잠긴 절은 다시 작성되지 않는다',
     pre='관리자 계정, 잠긴 절이 있는 제안서 문서 1건',
     steps='1. 그 절의 본문 블록을 적어 둔다\n2. 관리자 토큰으로 POST /admin/bid-documents/{documentId}/sections/{sectionId}/regenerate 를 부른다',
     expect='1. 다시 작성 요청이 거부된다(2xx 가 아니다)\n'
            '2. 그 절의 본문 블록이 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-014', flow='생성 완료 알림', kind='정상', title='제안서 생성이 끝나면 관리자에게 완료 메일이 간다',
     pre='관리자 계정과 관리자 메일함, IT-10-001 과 같은 조건의 입찰 건(ANALYZING), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면에서 그 입찰 건을 GENERATING 으로 전환하고 생성이 끝날 때까지 기다린다\n2. 관리자 메일함을 연다\n3. 메일의 바로가기 링크를 누른다',
     expect='1. 관리자 메일함에 문서 생성 완료 메일이 1통 온다\n'
            '2. 메일에 생성된 문서 유형과 그 입찰 건의 공고 제목이 있다\n'
            '3. 바로가기 링크를 누르면 관리자 화면에서 그 입찰 건이나 그 문서가 열린다\n'
            '4. 알림 이력에 notification_type DOCUMENT_GENERATED, success true 인 행이 그 입찰 건을 참조로 1건 더해진다',
     judge=['합동', '합동', '합동', '서버'],
     req=['BID-DOC-002'], fc=['BID-DOC-002'], screen='', pri='High'),

dict(id='IT-10-015', flow='문서 편집', kind='정상', title='관리자가 초안 문서를 고쳐 저장하면 새 버전이 생긴다',
     pre='관리자 계정, REVIEW 상태 입찰 건의 DRAFT 문서 1건(버전 1)',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 [편집] 을 누른다\n2. 에디터에서 문단 하나를 고친다\n3. [저장] 을 누른다\n4. 변경 요약에 "문단 수정" 을 넣고 확인한다',
     expect='1. 고치기 전에는 [저장] 이 꺼져 있고 2단계에서 고치면 켜진다\n'
            '2. [저장] 을 누르면 변경 요약을 넣는 입력 칸이 있는 확인 창이 뜬다\n'
            '3. 확인 뒤 "v2 저장되었습니다" 알림이 뜬다\n'
            '4. 버전 사이드바에 v2 가 "문단 수정" 변경 요약과 함께 더해진다\n'
            '5. bid_document_versions 에 version_no 2 행이 생기고 content_json 이 고친 본문, edited_by 가 그 관리자이며 문서의 current_version_no 가 2 이다\n'
            '6. 버전 1 행의 content_json 은 고치기 전 본문 그대로이다',
     judge=['가상', '가상', '가상', '가상', '서버', '서버'],
     req=['BID-EDIT-001', 'BID-DOC-003'], fc=['BID-EDIT-001', 'BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='화면 소스의 문서 편집 경로는 /documents/{id}/edit 이고, 변경 요약은 창이 아닌 입력 칸, 저장 알림은 "저장되었습니다." 로 설계와 다르다(소스 기준)'),

dict(id='IT-10-016', flow='문서 편집', kind='정상', title='잠긴 문서는 읽기 전용으로 열리고 잠금 해제와 PDF 만 켜져 있다',
     pre='관리자 계정, CONFIRMED 상태 입찰 건의 LOCKED 문서 1건',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 문서 편집으로 들어간다\n2. 에디터 본문을 고치려 해 본다',
     expect='1. 에디터가 읽기 전용이라 본문 글자가 바뀌지 않는다\n'
            '2. [저장] 이 꺼져 있다\n'
            '3. [확정] 이 없다\n'
            '4. [잠금 해제] 와 [PDF] 가 켜져 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-EDIT-001', 'BID-EDIT-002'], fc=['BID-EDIT-001', 'BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='T3-4 문서 편집 진입 조건은 BidRequest REVIEW, BidDocument DRAFT 인데 같은 절의 상태별 UI 분기는 LOCKED, ARCHIVED 화면을 적어 서로 어긋난다'),

dict(id='IT-10-017', flow='문서 편집', kind='정상', title='보관된 문서는 읽기 전용이고 PDF 만 켜져 있다',
     pre='관리자 계정, SUBMITTED 상태 입찰 건의 ARCHIVED 문서 1건',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 문서 편집으로 들어간다',
     expect='1. 에디터가 읽기 전용이라 본문 글자가 바뀌지 않는다\n'
            '2. [저장] 이 꺼져 있다\n'
            '3. [확정] 과 [잠금 해제] 가 없다\n'
            '4. [PDF] 가 켜져 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-018', flow='문서 편집', kind='예외', title='잠긴 문서에 새 버전 저장 요청은 거부된다',
     pre='관리자 계정, LOCKED 문서 1건',
     steps='1. 그 문서의 버전 수를 적어 둔다\n2. 관리자 토큰으로 POST /bid-documents/{id}/versions 에 고친 본문을 실어 부른다',
     expect='1. 저장 요청이 거부된다(2xx 가 아니다)\n'
            '2. 그 문서의 버전 수와 current_version_no 가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='', pri='High'),

dict(id='IT-10-019', flow='문서 편집', kind='예외', title='입찰 건이 REVIEW 가 아니면 문서 편집에 들어가지 못하고 입찰 상세로 돌아간다',
     pre='관리자 계정, GENERATING 상태 입찰 건의 DRAFT 문서 1건',
     steps='1. 관리자 화면 주소창에 그 문서의 문서 편집 주소를 넣어 연다',
     expect='1. 문서를 편집할 수 없다는 오류 안내가 뜬다\n'
            '2. 그 입찰 건의 입찰 상세 관리(/bid-requests/{id}) 화면이 열린다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-020', flow='문서 편집', kind='예외', title='문서를 불러오지 못하면 입찰 상세로 돌아간다',
     pre='관리자 계정, REVIEW 입찰 건의 DRAFT 문서 1건, 문서 조회 요청이 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 입찰 상세 관리에서 생성 문서의 [편집] 을 누른다',
     expect='1. 문서를 불러오지 못했다는 오류 안내가 뜬다\n'
            '2. 그 입찰 건의 입찰 상세 관리(/bid-requests/{id}) 화면이 열린다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-021', flow='문서 편집', kind='예외', title='버전 목록을 불러오지 못하면 버전 사이드바가 빈 상태다',
     pre='관리자 계정, REVIEW 입찰 건의 DRAFT 문서 1건, 버전 목록 조회 요청만 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 입찰 상세 관리에서 생성 문서의 [편집] 을 누른다',
     expect='1. 에디터에 그 문서의 현재 본문이 열린다\n'
            '2. 버전 사이드바에 버전 줄이 하나도 없다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001', 'BID-DOC-003'], fc=['BID-EDIT-001', 'BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-022', flow='문서 편집', kind='예외', title='관리자가 아니면 문서를 열거나 저장할 수 없다',
     pre='회원 계정 로그인 토큰, DRAFT 문서 1건',
     steps='1. 회원 토큰으로 GET /bid-documents/{id} 를 부른다\n2. 회원 토큰으로 POST /bid-documents/{id}/versions 에 본문을 실어 부른다',
     expect='1. 문서 조회가 403 으로 거부되고 본문이 응답에 없다\n'
            '2. 저장 요청이 403 으로 거부되고 그 문서의 버전 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='', pri='High'),

dict(id='IT-10-023', flow='문서 잠금', kind='정상', title='관리자가 초안 문서를 확정해 잠근다',
     pre='관리자 계정, REVIEW 상태 입찰 건의 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [확정] 을 누른다\n3. 확인 창에서 확인을 누른다\n4. 입찰 상세 관리로 돌아간다',
     expect='1. 확인 창에 "확정 후 편집이 불가합니다. 확정하시겠습니까?" 가 보인다\n'
            '2. 문서 상태가 LOCKED 로 바뀐다\n'
            '3. 에디터가 읽기 전용이 되어 본문 글자가 바뀌지 않는다\n'
            '4. [확정] 자리에 [잠금 해제] 가 보인다\n'
            '5. 입찰 상세 관리 생성 문서 목록의 그 문서 상태가 잠김으로 보인다',
     judge=['가상', '서버', '가상', '가상', '가상'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='화면 소스의 단추 이름은 [확정] 이 아닌 [잠금] 이고 확인 창이 없다(소스 기준)'),

dict(id='IT-10-024', flow='문서 잠금', kind='정상', title='관리자가 잠긴 문서의 잠금을 풀면 문서는 초안, 입찰 건은 REVIEW 로 돌아간다',
     pre='관리자 계정, CONFIRMED 상태 입찰 건의 LOCKED 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [잠금 해제] 를 누른다\n3. 확인 창에서 확인을 누른다',
     expect='1. [잠금 해제] 를 누르면 잠금 해제를 묻는 확인 창이 뜬다\n'
            '2. 문서 상태가 DRAFT 로 바뀐다\n'
            '3. 입찰 건 상태가 REVIEW 로 바뀌고 상태 이력에 REVIEW 한 줄이 더해진다\n'
            '4. 에디터가 편집 상태로 돌아와 [저장] 과 [확정] 이 다시 보인다',
     judge=['가상', '서버', '서버', '가상'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-025', flow='문서 잠금', kind='예외', title='이미 잠긴 문서와 보관된 문서는 다시 잠기지 않는다',
     pre='관리자 계정, LOCKED 문서 1건, ARCHIVED 문서 1건',
     steps='1. 관리자 토큰으로 LOCKED 문서에 POST /bid-documents/{id}/lock 을 부른다\n2. 관리자 토큰으로 ARCHIVED 문서에 POST /bid-documents/{id}/lock 을 부른다',
     expect='1. LOCKED 문서의 잠금 요청이 거부되고 상태가 LOCKED 그대로이다\n'
            '2. ARCHIVED 문서의 잠금 요청이 거부되고 상태가 ARCHIVED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-026', flow='문서 잠금', kind='예외', title='보관된 문서는 잠금을 풀 수 없다',
     pre='관리자 계정, SUBMITTED 입찰 건의 ARCHIVED 문서 1건',
     steps='1. 관리자 토큰으로 POST /bid-documents/{id}/unlock 을 부른다',
     expect='1. 잠금 해제 요청이 거부된다(2xx 가 아니다)\n'
            '2. 문서 상태가 ARCHIVED 그대로이고 입찰 건 상태가 SUBMITTED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-027', flow='문서 잠금', kind='예외', title='관리자가 아니면 문서를 잠그거나 풀 수 없다',
     pre='회원 계정 로그인 토큰, DRAFT 문서 1건과 LOCKED 문서 1건',
     steps='1. 회원 토큰으로 DRAFT 문서에 POST /bid-documents/{id}/lock 을 부른다\n2. 회원 토큰으로 LOCKED 문서에 POST /bid-documents/{id}/unlock 을 부른다',
     expect='1. 잠금 요청이 403 으로 거부되고 문서 상태가 DRAFT 그대로이다\n'
            '2. 잠금 해제 요청이 403 으로 거부되고 문서 상태가 LOCKED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-028', flow='PDF 내보내기', kind='정상', title='문서를 PDF 로 내려받는다',
     pre='관리자 계정, 제목과 목록이 들어 있는 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [PDF] 를 누른다\n3. 내려받은 파일을 연다',
     expect='1. 확장자가 pdf 인 파일 하나가 내려받아진다\n'
            '2. PDF 에 에디터의 현재 본문과 같은 글이 제목, 목록 서식대로 들어 있다',
     judge=['가상', '합동'],
     req=['BID-EDIT-003'], fc=['BID-EDIT-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='Medium'),

dict(id='IT-10-029', flow='문서 버전', kind='정상', title='버전 이력과 이전 버전 본문을 본다',
     pre='관리자 계정, 버전이 3개 있는 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. 버전 사이드바에서 버전 1 을 누른다\n3. 관리자 토큰으로 GET /bid-documents/{id}/versions 를 부른다',
     expect='1. 버전 사이드바에 버전 3줄이 있고 줄마다 버전 번호, 편집자, 편집 시각, 변경 요약이 보인다\n'
            '2. 버전 1 을 누르면 그 버전의 content_json 본문이 보인다\n'
            '3. 버전 목록 응답이 bid_document_versions 의 그 문서 행 3개와 버전 번호, 편집자, 시각, 변경 요약이 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-003'], fc=['BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='Medium',
     issue='T1-1 의 이전 버전 비교가 T3-2, T3-3, T3-4 에 없다'),

dict(id='IT-10-030', flow='문서 버전', kind='예외', title='만들어진 버전은 고치거나 지울 수 없다',
     pre='관리자 계정, 버전이 2개 있는 DRAFT 문서 1건',
     steps='1. 버전 1 행의 content_json 과 change_summary 를 적어 둔다\n2. 관리자 토큰으로 PUT, PATCH /bid-documents/{id}/versions/1 에 다른 본문을 실어 부른다\n3. 관리자 토큰으로 DELETE /bid-documents/{id}/versions/1 을 부른다',
     expect='1. 세 요청 모두 받아들여지지 않는다(2xx 가 아니다)\n'
            '2. 버전 1 행이 남아 있고 content_json 과 change_summary 가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-003'], fc=['BID-DOC-003'], screen='', pri='Medium'),

dict(id='IT-10-031', flow='템플릿 등록', kind='정상', title='관리자가 새 템플릿을 등록한다',
     pre='관리자 계정, 쓰지 않은 템플릿 이름 하나와 TipTap JSON 본문',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 을 연다\n2. [새 템플릿 등록] 을 누른다\n3. 템플릿 이름, 문서 타입 기술 제안서, TipTap JSON 을 넣고 [저장] 을 누른다',
     expect='1. 양식 목록에 넣은 템플릿 이름이 문서 유형 기술 제안서, 버전 1, 활성 상태로 한 줄 더해진다\n'
            '2. document_templates 에 그 이름, document_type TECHNICAL_PROPOSAL, 넣은 content_json, active true 인 행이 생긴다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High',
     issue='T1-1 입력의 태그/분류와 마크다운 본문이 T3-3 양식 등록 칸과 T3-1 문서 양식 표에 없다'),

dict(id='IT-10-032', flow='템플릿 등록', kind='정상', title='같은 문서 유형에 템플릿을 여러 개 등록한다',
     pre='관리자 계정, 기술 제안서 유형 템플릿이 1개 있는 서버',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 [새 템플릿 등록] 을 누른다\n2. 다른 이름, 문서 타입 기술 제안서로 저장한다\n3. 관리자 토큰으로 GET /admin/document-templates/TECHNICAL_PROPOSAL 을 부른다',
     expect='1. 양식 목록에 기술 제안서 유형 템플릿 두 줄이 각각의 이름으로 보인다\n'
            '2. 문서 유형별 조회 응답에 두 템플릿이 모두 들어 있다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-033', flow='템플릿 등록', kind='예외', title='같은 이름의 템플릿은 등록되지 않는다',
     pre='관리자 계정, 이미 등록된 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 [새 템플릿 등록] 을 누른다\n2. 이미 있는 템플릿과 같은 이름을 넣고 [저장] 을 누른다',
     expect='1. 양식 목록에 그 이름의 줄이 더해지지 않고 줄 수가 그대로이다\n'
            '2. document_templates 에 그 이름의 행이 하나뿐이다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-034', flow='템플릿 등록', kind='예외', title='관리자가 아니면 템플릿을 등록할 수 없다',
     pre='회원 계정 로그인 토큰',
     steps='1. 회원 토큰으로 POST /admin/document-templates 에 새 이름의 템플릿을 실어 부른다',
     expect='1. 요청이 403 으로 거부된다\n'
            '2. document_templates 에 그 이름의 행이 없다',
     judge=['서버', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='', pri='High'),

dict(id='IT-10-035', flow='템플릿 조회', kind='정상', title='템플릿 목록과 문서 유형별 템플릿을 본다',
     pre='관리자 계정, 문서 유형이 다른 활성 템플릿 3개 이상',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 을 연다\n2. 관리자 토큰으로 GET /admin/document-templates/COVER_LETTER 를 부른다',
     expect='1. 양식 목록 줄마다 양식명, 문서 유형, 버전, 활성 상태가 보인다\n'
            '2. document_templates 의 활성 템플릿이 모두 양식 목록에 있다\n'
            '3. 문서 유형별 조회 응답에 문서 유형이 COVER_LETTER 인 템플릿만 있다',
     judge=['가상', '가상', '서버'],
     req=['BID-TPL-002'], fc=['BID-TPL-002'], screen='관리자 /templates', pri='High',
     issue='T1-1 입력의 태그 필터와 페이지 정보가 T3-2 템플릿 목록 조회와 T3-3 에 없다'),

dict(id='IT-10-036', flow='템플릿 조회', kind='정상', title='템플릿 하나의 내용을 연다',
     pre='관리자 계정, TipTap JSON 이 저장된 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [편집] 을 누른다',
     expect='1. 열린 창에 그 템플릿의 양식명, 문서 유형, 저장된 TipTap JSON 이 채워져 있다',
     judge=['가상'],
     req=['BID-TPL-002'], fc=['BID-TPL-002'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-037', flow='템플릿 수정', kind='정상', title='관리자가 템플릿 내용을 고친다',
     pre='관리자 계정, 활성 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [편집] 을 누른다\n2. TipTap JSON 의 문구 하나를 바꾸고 [저장] 을 누른다\n3. 관리자 토큰으로 그 문서 유형의 템플릿 조회(GET /admin/document-templates/{documentType})를 부른다\n4. 다시 [편집] 을 누른다',
     expect='1. 문서 유형별 조회 응답의 그 템플릿 content_json 에 2단계에서 바꾼 문구가 들어 있다\n'
            '2. 다시 연 창의 TipTap JSON 에 2단계에서 바꾼 문구가 들어 있다',
     judge=['서버', '가상'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='관리자 /templates', pri='Medium'),

dict(id='IT-10-038', flow='템플릿 수정', kind='정상', title='관리자가 템플릿을 비활성화한다',
     pre='관리자 계정, 어느 입찰 문서도 쓰지 않는 활성 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [비활성화] 를 누르고 확인한다',
     expect='1. 그 템플릿 줄의 활성 상태가 비활성으로 보인다\n'
            '2. document_templates 의 그 행이 남아 있고 active 가 false 이다',
     judge=['가상', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='관리자 /templates', pri='Medium'),

dict(id='IT-10-039', flow='템플릿 수정', kind='예외', title='입찰 문서가 쓰는 템플릿은 지워지지 않고 비활성화만 된다',
     pre='관리자 계정, 입찰 문서의 template_id 가 가리키는 활성 템플릿 1개',
     steps='1. 관리자 토큰으로 DELETE /admin/document-templates/{id} 를 부른다\n2. 그 템플릿 행과 그 템플릿을 쓰는 입찰 문서 행을 조회한다',
     expect='1. document_templates 의 그 행이 지워지지 않고 남아 있으며 active 가 false 이다\n'
            '2. 그 템플릿을 쓰는 입찰 문서의 template_id 가 그대로이다',
     judge=['서버', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='', pri='Medium',
     issue='T3-2 의 DELETE 는 늘 비활성화라 쓰지 않는 템플릿을 지우는 길이 설계에 없다'),

dict(id='IT-10-040', flow='템플릿 수정', kind='예외', title='관리자가 아니면 템플릿을 고치거나 비활성화할 수 없다',
     pre='회원 계정 로그인 토큰, 활성 템플릿 1개',
     steps='1. 회원 토큰으로 PATCH /admin/document-templates/{id} 에 다른 내용을 실어 부른다\n2. 회원 토큰으로 DELETE /admin/document-templates/{id} 를 부른다',
     expect='1. 수정 요청이 403 으로 거부되고 그 템플릿 content_json 이 그대로이다\n'
            '2. 비활성화 요청이 403 으로 거부되고 그 템플릿 active 가 true 그대로이다',
     judge=['서버', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='', pri='Medium'),

]
