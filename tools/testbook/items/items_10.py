# 영역 10 AI 문서 생성/편집 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19, T1-3 업무 규칙, T1-5 상태 정의, T3-1 데이터 모델, T3-2 API 설계, T3-3, T3-4 화면 설계다.

# 서버 판정이 상태를 건드릴 수 있는 호출(전환, 다시 읽기)은 시험 고객의 입찰 건에만 건다. 결함이 있어도 실제 고객 건이 움직이지 않는다
USER = 'user:qa-cr143@example.com'
ADMIN = 'admin:smoke-admin@example.com'
QA_BIDS = "JOIN members m ON m.id = br.member_id WHERE m.email = 'qa-cr143@example.com'"


def uuid_of(col):
    """바이너리 열쇠를 API 주소 꼴로 바꾼다. 조회 실행기가 INSERT, REPLACE 낱말을 막아 SUBSTR 로 잇는다"""
    return (f"LOWER(CONCAT_WS('-', SUBSTR(HEX({col}),1,8), SUBSTR(HEX({col}),9,4), SUBSTR(HEX({col}),13,4), "
            f"SUBSTR(HEX({col}),17,4), SUBSTR(HEX({col}),21,12)))")


def candidate_shas(bid):
    """그 입찰 건의 읽기 후보 내용. 공고별 서류와 만료되지 않은 서류함 서류다(DocumentReadingService.collect)"""
    return (f"(SELECT cd.content_sha256 FROM client_documents cd WHERE cd.bid_request_id = {bid} "
            f"UNION SELECT md.content_sha256 FROM member_documents md JOIN bid_requests b ON b.member_id = md.member_id "
            f"WHERE b.id = {bid} AND (md.expires_on IS NULL OR md.expires_on >= '{{today}}'))")


def reading_rows(bid):
    return (f"SELECT HEX(dr.id) AS hex, dr.status, dr.read_at FROM document_readings dr "
            f"WHERE dr.content_sha256 IN {candidate_shas(bid)} ORDER BY dr.id")


THIS_BID = "UNHEX('{val:0:hex}')"
READINGS = '/api/admin/bid-requests/{val:0:id}/document-readings'
TRANSITION = '/api/admin/bid-requests/{val:0:id}/transition'
LAST_STEP = (f"SELECT state, JSON_LENGTH(state_history) AS hist, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].fromState')) AS from_state, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")

# MCP 열쇠는 저장소에 둘 수 없어 실행기가 환경변수에서 싣는다(run_server 머리글 값)
MCP_KEY = {'X-API-Key': '$TESTBOOK_MCP_API_KEY'}


def mcp(tool, args):
    return ('api', 'guest', 'POST', '/api/mcp',
            {'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': tool, 'arguments': args}}, MCP_KEY)


def mcp_out(i):
    """MCP 도구 결과는 JSON 을 글자로 한 번 더 싸서 돌려준다"""
    return f"r[{i}]['body']['result']['content'][0]['text']"


def mcp_json(i):
    return f"loads({mcp_out(i)})"


def mcp_rejected(i, reading):
    return (f"r[{i}]['body']['result'].get('isError') and "
            f"('unknownReadingIds: [' + r[0][0]['{reading}'] + ']') in {mcp_out(i)}")


def save_structure(reading_cols):
    return mcp('save_proposal_structure', {'documentId': '{val:0:id}', 'chapters': [
        {'factorLabel': '1', 'factorTitle': 'Past Performance', 'sections': [
            {'subfactorLabel': '1.1', 'title': 'Relevant Contracts', 'scope': 'Recent relevant contracts',
             'requirementRefs': [], 'sourceReadingIds': [f'{{val:0:{c}}}' for c in reading_cols], 'minWords': 200}]}]})


# 근거 서류 E 의 읽기 결과를 Aimbase 가 보내는 꼴로 저장한다. 빠진 칸과 소문자 값을 서버가 저장 꼴로 맞추는지까지 본다
SAVE_E_FACTS = mcp('save_client_document_facts', {
    'bidRequestId': '{val:0:bid}',
    'targets': [{'readingId': '{val:0:e}', 'targetPath': 'docs/equipment.pdf'}],
    'results': [{'index': 0, 'facts': [{'category': 'equipment', 'key': '장비', 'value': '굴삭기 2대'},
                                       {'category': 'EQUIPMENT', 'key': '수량', 'value': '2', 'page': 3, 'readMode': 'vision'}]}]})
FACT_FIELDS = "{'category', 'key', 'value', 'page', 'anchor', 'confidence', 'readMode'}"
SECTION_SOURCES = ("SELECT ps.source_reading_ids FROM proposal_section ps JOIN proposal_chapter pc ON pc.id = ps.chapter_id "
                   f"WHERE pc.document_id = {THIS_BID}")
STRUCTURE = ("SELECT HEX(pc.id) AS chapter, pc.factor_label, HEX(ps.id) AS section, ps.subfactor_label, ps.title, ps.source_reading_ids "
             f"FROM proposal_chapter pc JOIN proposal_section ps ON ps.chapter_id = pc.id WHERE pc.document_id = {THIS_BID} "
             "ORDER BY pc.order_no, ps.order_no")

ITEMS = [

dict(id='IT-10-001', flow='제안서 자동 작성', kind='정상', title='공고 유형에 맞는 성공 제안서가 있는 입찰 건의 제안서를 AI 가 작성한다',
     pre='관리자 계정, 사전 분석 COMPLETED 이고 industry_type 이 있으며 그 유형에 성공 제안서 1건 이상이 있는 공고의 입찰 건(ANALYZING, 고객 서류 접수 완료, 고객 서류 읽기가 끝나 읽음 서류가 있음), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 생성이 끝날 때까지 기다린 뒤 화면을 새로고침한다\n4. 생성 문서의 [편집] 으로 본문을 연다',
     expect='1. 입찰 건 상태가 GENERATING 을 거쳐 REVIEW 가 되고 상태 이력에 GENERATING, REVIEW 두 줄이 더해진다\n'
            '2. 그 입찰 건에 입찰 문서(bid_documents) 행이 상태 DRAFT 로 생기고 버전 1 의 content_json 에 TipTap JSON 본문이 들어 있다\n'
            '3. 입찰 상세 관리 생성 문서 목록에 그 문서가 문서 유형과 상태 초안으로 보인다\n'
            '4. 제안서의 회사명, 실적, 인력, 장비가 고객이 낸 서류의 내용이고 성공 제안서 원본의 회사명이나 실적이 섞이지 않는다\n'
            '5. 제안서 구성과 서술 방식이 그 공고 유형의 성공 제안서 가이드를 따른다\n'
            '6. 제안서가 공고 사전 분석의 문서 양식과 필요 서류를 반영한다',
     judge=['서버', '서버', '가상', '합동', '합동', '합동'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b001_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-001 서버 판정'}),
                           ('poll', ADMIN, '/api/admin/bid-requests/{val:0:id}', "x['body']['state'] != 'GENERATING'", 1800, 30),
                           ('sql', f"SELECT state, JSON_LENGTH(state_history) AS hist, "
                                   f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last-1].toState')) AS prev_state, "
                                   f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")],
                    need="r[0][0]['state'] == 'ANALYZING'",
                    check="r[1]['status'] == 200 and r[3][0]['state'] == 'REVIEW' and int(r[3][0]['hist']) == int(r[0][0]['hist']) + 2 "
                          "and r[3][0]['prev_state'] == 'GENERATING' and r[3][0]['to_state'] == 'REVIEW'"),
            2: dict(steps=[('key', '10.b001_bid'),
                           ('sql', f"SELECT bd.status, JSON_UNQUOTE(JSON_EXTRACT(v.content_json, '$.type')) AS t FROM bid_documents bd "
                                   f"LEFT JOIN bid_document_versions v ON v.document_id = bd.id AND v.version_no = 1 "
                                   f"WHERE bd.bid_request_id = {THIS_BID}")],
                    need="r[0][0]['state'] != 'ANALYZING'",
                    check="len(r[1]) > 0 and all(x['status'] == 'DRAFT' and x['t'] == 'doc' for x in r[1])")},
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

dict(id='IT-10-041', flow='고객 서류 읽기', kind='정상', title='분석 단계로 전환하면 공고별 서류와 서류함 서류를 읽어 회사 사실을 남긴다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 글자 층이 있는 실적 증명서 PDF 1개를 올림), 그 회원 공통 서류함에 만료일 없는 사업자등록증 PDF 1개와 만료일이 어제인 보험증권 PDF 1개, Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 관리자 화면 [입찰 요청 관리] 에서 그 입찰 건의 입찰 상세 관리를 연다\n2. 다음 상태 ANALYZING 으로 전환한다\n'
           '3. 고객 서류 읽기 영역을 본다\n4. 읽기가 끝날 때까지 기다린다\n5. 실적 증명서 줄을 누른다',
     expect='1. 3단계에서 고객 서류 읽기 영역에 실적 증명서(출처 공고별 서류)와 사업자등록증(출처 공통 서류함) 두 줄이 읽는 중으로 보인다\n'
            '2. 3단계에서 만료일이 지난 보험증권 줄은 영역에 없다\n'
            '3. 4단계 뒤 두 줄의 상태가 읽음이고 사실 수가 1 이상이다\n'
            '4. document_readings 에 두 파일의 content_sha256 줄이 status DONE 으로 있고 fact_count 가 화면의 사실 수와 같다\n'
            '5. 5단계에서 펼친 사실 목록에 실적 증명서에 적힌 사업명, 발주처, 계약 금액이 원문 값 그대로 쪽 번호와 함께 보인다',
     judge=['가상', '가상', '가상', '서버', '합동'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={4: dict(steps=[('key', '10.b041_done_bid'),
                           ('api', ADMIN, 'GET', READINGS),
                           ('sql', f"SELECT {uuid_of('dr.id')} AS id, dr.status, dr.fact_count FROM document_readings dr "
                                   f"WHERE dr.content_sha256 IN {candidate_shas(THIS_BID)}")],
                    need="r[1]['status'] == 200 and any(i['status'] == 'DONE' for i in r[1]['body']['items'])",
                    check="all(any(d['id'] == i['readingId'] and d['status'] == 'DONE' and int(d['fact_count']) == i['factCount'] "
                          "for d in r[2]) for i in r[1]['body']['items'] if i['status'] == 'DONE')")}),

dict(id='IT-10-042', flow='고객 서류 읽기', kind='정상', title='같은 내용의 서류는 한 번만 읽고 다른 입찰 건에서도 그 결과를 쓴다',
     pre='관리자 계정, IT-10-041 을 마친 회원의 두 번째 입찰 건(DOCS_RECEIVED, 서류함의 사업자등록증을 그 건의 요구 항목에 넣어 공고별 서류로 복사됨), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. document_readings 의 줄 수를 적어 둔다\n2. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n'
           '3. 고객 서류 읽기 영역을 본다\n4. document_readings 의 줄 수를 다시 센다',
     expect='1. 3단계 영역에 사업자등록증 줄이 하나만 있고 출처가 공고별 서류다\n'
            '2. 3단계에서 그 줄의 상태가 읽는 중을 거치지 않고 바로 읽음이며 사실 수가 IT-10-041 의 사업자등록증 줄과 같다\n'
            '3. 4단계 줄 수가 1단계와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={3: dict(steps=[('key', '10.b042_server_bid'),
                           ('sql', f"SELECT COUNT(*) AS n FROM document_readings WHERE content_sha256 IN {candidate_shas(THIS_BID)}"),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-042 서버 판정'}),
                           ('sql', f"SELECT COUNT(*) AS n FROM document_readings WHERE content_sha256 IN {candidate_shas(THIS_BID)}")],
                    check="r[2]['status'] == 200 and int(r[3][0]['n']) == int(r[1][0]['n'])")}),

dict(id='IT-10-043', flow='고객 서류 읽기', kind='정상', title='글자 층이 없는 스캔 서류는 글자 인식으로 읽는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 글자 층이 없는 스캔 PDF 면허증 1개를 올림), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. 읽기가 끝날 때까지 기다린다\n3. 면허증 줄을 누른다',
     expect='1. 면허증 줄의 상태가 "읽음(글자 인식)" 이다\n'
            '2. 3단계 사실 목록에 면허증의 면허 종류와 면허 번호가 스캔 원본에 적힌 값 그대로 보인다\n'
            '3. document_readings 그 줄의 vision_used 가 true 이고 facts_json 에 readMode VISION 인 사실이 있다',
     judge=['가상', '합동', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={3: dict(steps=[('sql', "SELECT HEX(id) AS hex, file_name, "
                                   "(JSON_SEARCH(facts_json, 'one', 'VISION', NULL, '$[*].readMode') IS NOT NULL) AS has_vision "
                                   "FROM document_readings WHERE status = 'DONE' AND vision_used = 1")],
                    need="len(r[0]) > 0",
                    check="all(x['has_vision'] == '1' for x in r[0])")}),

dict(id='IT-10-044', flow='고객 서류 읽기', kind='정상', title='그림, 엑셀, 워드, 한글 파일 서류도 읽는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 인력 현황 JPG 1개, 장비 목록 XLSX 1개, 회사 소개 DOCX 1개, 회사 소개 HWP 1개를 올림), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. 읽기가 끝날 때까지 기다린다\n3. 네 줄을 하나씩 눌러 사실 목록을 본다',
     expect='1. JPG 줄의 사실 목록에 그림 속 인력 현황의 직책과 인원이 원본 값 그대로 있다\n'
            '2. XLSX 줄의 사실 목록에 장비 목록 표의 장비 종류와 수량이 원본 값 그대로 있다\n'
            '3. DOCX 줄의 사실 목록에 회사 소개의 상호와 설립일이 원본 값 그대로 있다\n'
            '4. HWP 줄의 사실 목록에 회사 소개의 상호와 설립일이 원본 값 그대로 있다',
     judge=['합동', '합동', '합동', '합동'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-045', flow='고객 서류 읽기', kind='예외', title='읽을 수 없는 형식은 읽지 않고 표시하며 문서 작성 중 전환을 막지 않는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 옛 워드 DOC 1개와 글자 층이 있는 PDF 1개를 올림), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. PDF 줄이 읽음이 될 때까지 기다린다\n3. 다음 상태 GENERATING 으로 전환한다',
     expect='1. 2단계에서 DOC 줄의 상태가 "읽을 수 없는 형식" 이다\n'
            '2. document_readings 의 DOC 줄이 status UNSUPPORTED, read_as NONE 이다\n'
            '3. 3단계 전환이 거부되지 않고 state 가 GENERATING 이 된다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b045_server2_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-045 서버 판정'}),
                           ('sql', "SELECT status, read_as FROM document_readings WHERE content_sha256 = '{val:0:doc_sha}'")],
                    check="r[1]['status'] == 200 and len(r[2]) == 1 and r[2][0]['status'] == 'UNSUPPORTED' and r[2][0]['read_as'] == 'NONE'"),
            3: dict(steps=[('key', '10.b045_server3_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-045 서버 판정'}),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-045 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 200 and r[2]['status'] == 200 and r[3][0]['state'] == 'GENERATING'")}),

dict(id='IT-10-046', flow='고객 서류 읽기', kind='예외', title='고객 서류를 읽는 중이면 문서 작성 중으로 전환되지 않는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류 한 줄이 READING 이며 상태가 바뀐 지 5분이 안 된 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 상태 패널의 다음 상태 GENERATING 을 본다\n'
           '3. 관리자 토큰으로 POST /admin/bid-requests/{id}/transition 에 GENERATING 을 실어 부른다\n4. 그 입찰 건의 state 와 state_history 를 조회한다',
     expect='1. 2단계에서 GENERATING 전환이 눌리지 않고 "고객 서류를 읽는 중입니다" 가 보인다\n'
            '2. 3단계 응답이 409 이고 코드가 DOCUMENT_READING_IN_PROGRESS 이다\n'
            '3. 4단계에서 state 가 ANALYZING 그대로이고 state_history 에 줄이 더해지지 않았다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b046_server2_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-046 서버 판정'})],
                    check="r[1]['status'] == 409 and r[1]['body'].get('error') == 'DOCUMENT_READING_IN_PROGRESS'"),
            3: dict(steps=[('key', '10.b046_server3_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-046 서버 판정'}),
                           ('sql', f"SELECT state, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[2][0]['state'] == 'ANALYZING' and r[2][0]['hist'] == r[0][0]['hist']")}),

dict(id='IT-10-047', flow='고객 서류 읽기', kind='예외', title='읽기가 워크플로우 대기 한도를 넘게 끝나지 않은 서류는 읽지 못함이 되어 전환을 막지 않는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류 한 줄이 READING 이며 status_at 이 워크플로우 대기 한도(설정 app.aimbase.polling 간격과 횟수, 10/25 상용 60분)보다 오래 전인 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 다음 상태 GENERATING 으로 전환한다',
     expect='1. 1단계에서 그 줄의 상태가 "읽지 못함" 이고 사유 "읽기 시간 초과" 가 보인다\n'
            '2. 2단계 전환이 거부되지 않고 state 가 GENERATING 이 된다',
     judge=['가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b047_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-047 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 200 and r[2][0]['state'] == 'GENERATING'")}),

dict(id='IT-10-048', flow='고객 서류 읽기', kind='정상', title='읽지 못한 서류를 다시 읽는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류가 읽지 못함 1줄(글자 층이 있는 PDF)과 읽음 1줄인 입찰 건, Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 읽음 줄의 document_readings read_at 을 적어 둔다\n2. 그 입찰 건의 입찰 상세 관리 고객 서류 읽기 영역에서 [다시 읽기] 를 누른다\n3. 읽기가 끝날 때까지 기다린다',
     expect='1. 읽지 못함이던 줄이 읽는 중을 거쳐 읽음이 되고 사실 수가 1 이상이다\n'
            '2. 읽음이던 줄의 read_at 이 1단계와 같다',
     judge=['가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b048_server_bid'),
                           ('sql', f"SELECT HEX(dr.id) AS hex, dr.read_at FROM document_readings dr WHERE dr.status = 'DONE' "
                                   f"AND dr.content_sha256 IN {candidate_shas(THIS_BID)} ORDER BY dr.id"),
                           ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
                           ('poll', ADMIN, READINGS, "x['status'] == 200 and not x['body']['inProgress']", 1800, 20),
                           ('sql', f"SELECT HEX(dr.id) AS hex, dr.read_at FROM document_readings dr "
                                   f"WHERE dr.content_sha256 IN {candidate_shas(THIS_BID)} ORDER BY dr.id")],
                    check="r[2]['status'] == 200 and [x for x in r[4] if x['hex'] in [y['hex'] for y in r[1]]] == r[1]")}),

dict(id='IT-10-049', flow='고객 서류 읽기', kind='예외', title='분석 단계가 아닌 입찰 건은 다시 읽기를 할 수 없다',
     pre='관리자 계정, 상태 GENERATING 이고 후보 서류에 읽지 못함 1줄이 있는 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 POST /admin/bid-requests/{id}/document-readings/run 을 부른다',
     expect='1. 1단계 고객 서류 읽기 영역에 [다시 읽기] 단추가 없다\n'
            '2. 2단계 응답이 409 이고 코드가 NOT_ANALYZING 이다\n'
            '3. 2단계 뒤 그 줄의 status 가 FAILED 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b049_bid'),
            ('sql', reading_rows(THIS_BID)),
            ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
            ('sql', reading_rows(THIS_BID))],
                    check="r[2]['status'] == 409 and r[2]['body'].get('error') == 'NOT_ANALYZING'"),
            3: dict(steps=[('key', '10.b049_bid'),
            ('sql', reading_rows(THIS_BID)),
            ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
            ('sql', reading_rows(THIS_BID))],
                    check="r[3] == r[1] and all(x['status'] == 'FAILED' for x in r[3] if x['hex'] in [y['hex'] for y in r[1] if y['status'] == 'FAILED'])")}),

dict(id='IT-10-050', flow='절별 근거 서류', kind='정상', title='제안서 설계가 읽은 고객 서류를 절마다 근거 서류로 받는다',
     pre='관리자 계정과 MCP 열쇠, 고객 서류 읽기가 끝난 입찰 건의 제안서 문서(읽음 서류 A 는 요구 항목 1개에 충족 매핑된 실적 증명서, 읽음 서류 B, 읽지 못함 서류 C)',
     steps='1. MCP get_proposal_design_input 을 그 문서 id 로 부른다\n2. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [A] 로 부른다\n'
           '3. 관리자 화면 /documents/{documentId}/sections 에서 절 1 을 고른다',
     expect='1. 1단계 clientDocuments 에 A, B, C 세 줄이 status DONE, DONE, FAILED 로 있다\n'
            '2. 1단계 A 줄의 fulfills 에 A 가 충족하는 요구 항목 이름이 있다\n'
            '3. 2단계 저장이 되고 응답의 unusedReadingIds 가 [B] 이다\n'
            '4. proposal_section 절 1 의 source_reading_ids 가 [A] 이다\n'
            '5. 3단계 우측 절 상세의 근거 서류에 A 의 파일 이름 꼬리표가 A 의 사실 수와 함께 보인다',
     judge=['서버', '서버', '서버', '서버', '가상'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='관리자 /documents/{id}/sections', pri='High',
     probe={1: dict(steps=[('key', '10.b050_doc'), mcp('get_proposal_design_input', {'documentId': '{val:0:id}'})],
                    check=f"len({mcp_json(1)}['clientDocuments']) == 3 and "
                          f"{{(d['readingId'], d['status']) for d in {mcp_json(1)}['clientDocuments']}} == "
                          "{(r[0][0]['a'], 'DONE'), (r[0][0]['b'], 'DONE'), (r[0][0]['c'], 'FAILED')}"),
            2: dict(steps=[('key', '10.b050_doc'), mcp('get_proposal_design_input', {'documentId': '{val:0:id}'})],
                    check=f"any(f['requirementTitle'] == r[0][0]['req'] for d in {mcp_json(1)}['clientDocuments'] "
                          "if d['readingId'] == r[0][0]['a'] for f in d['fulfills'])"),
            3: dict(steps=[('key', '10.b050_doc'), save_structure(['a'])],
                    check=f"not r[1]['body']['result'].get('isError') and {mcp_json(1)}['unusedReadingIds'] == [r[0][0]['b']]"),
            4: dict(steps=[('key', '10.b050_doc'), save_structure(['a']), ('sql', SECTION_SOURCES)],
                    check="len(r[2]) == 1 and loads(r[2][0]['source_reading_ids']) == [r[0][0]['a']]")}),

dict(id='IT-10-051', flow='절별 근거 서류', kind='예외', title='읽음이 아니거나 다른 입찰 건의 서류를 근거로 든 제안서 설계는 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, IT-10-050 을 마친 제안서 문서, 다른 입찰 건의 읽음 서류 D',
     steps='1. 그 문서의 장과 절을 적어 둔다\n2. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [A, C] 로 부른다\n'
           '3. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [D] 로 부른다\n4. 그 문서의 장과 절을 다시 조회한다',
     expect='1. 2단계 응답이 거부이고 unknownReadingIds 가 [C] 이다\n'
            '2. 3단계 응답이 거부이고 unknownReadingIds 가 [D] 이다\n'
            '3. 4단계 장과 절이 1단계와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b051_doc'), save_structure(['a', 'c'])],
                    check=mcp_rejected(1, 'c')),
            2: dict(steps=[('key', '10.b051_doc'), save_structure(['d'])],
                    check=mcp_rejected(1, 'd')),
            3: dict(steps=[('key', '10.b051_doc'), ('sql', STRUCTURE), save_structure(['a', 'c']), save_structure(['d']), ('sql', STRUCTURE)],
                    check="len(r[1]) > 0 and r[4] == r[1]")}),

dict(id='IT-10-052', flow='절별 근거 서류', kind='정상', title='절 작성과 절 검증 입력에 근거 서류의 사실이 들어 있다',
     pre='관리자 계정과 MCP 열쇠, IT-10-050 으로 절 1 의 근거 서류가 [A] 인 제안서 문서',
     steps='1. MCP get_section_context 를 절 1 id 로 부른다\n2. MCP get_section_verify_input 을 절 1 id 로 부른다',
     expect='1. 1단계 sourceDocuments 에 A 한 줄이 있고 facts 수가 document_readings A 줄의 fact_count 와 같다\n'
            '2. 1단계 facts 줄마다 category, key, value, page, anchor, confidence, readMode 칸이 있다\n'
            '3. 2단계 sourceDocuments 가 1단계 sourceDocuments 와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'}),
                           ('sql', "SELECT fact_count FROM document_readings WHERE id = UNHEX('{val:0:e_hex}')")],
                    check=f"len({mcp_json(2)}['sourceDocuments']) == 1 and {mcp_json(2)}['sourceDocuments'][0]['readingId'] == r[0][0]['e'] "
                          f"and int(r[3][0]['fact_count']) > 0 and len({mcp_json(2)}['sourceDocuments'][0]['facts']) == int(r[3][0]['fact_count'])"),
            2: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'})],
                    check=f"len({mcp_json(2)}['sourceDocuments']) > 0 and "
                          f"all(d['facts'] and all({FACT_FIELDS} <= set(f) for f in d['facts']) for d in {mcp_json(2)}['sourceDocuments'])"),
            3: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'}),
                           mcp('get_section_verify_input', {'sectionId': '{val:0:id}'})],
                    check=f"len({mcp_json(2)}['sourceDocuments']) > 0 and {mcp_json(3)}['sourceDocuments'] == {mcp_json(2)}['sourceDocuments']")}),

dict(id='IT-10-053', flow='절별 근거 서류', kind='정상', title='제안서 본문의 회사 사실은 근거 서류에서 오고 근거가 없으면 자리표시가 들어간다',
     pre='관리자 계정, 고객 서류 읽기가 끝난 ANALYZING 입찰 건(실적 증명서는 읽음, 인력 서류는 내지 않음, 공고 평가 항목에 수행 실적과 인력이 있음), Aimbase 제안서 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 GENERATING 으로 전환한다\n2. 생성이 끝날 때까지 기다린다\n'
           '3. 그 제안서의 /documents/{documentId}/sections 에서 수행 실적 절과 인력 절을 차례로 고른다',
     expect='1. 수행 실적 절의 근거 서류에 실적 증명서가 걸려 있다\n'
            '2. 수행 실적 절 본문의 사업명, 발주처, 계약 금액이 실적 증명서 사실 목록의 값과 같다\n'
            '3. 인력 절 본문에 지어낸 인원이나 자격 대신 "[Client data required:" 로 시작하는 자리표시가 있다\n'
            '4. 인력 절 검증 결과의 지어낸 말 목록에 자리표시가 들어 있지 않다',
     judge=['가상', '합동', '합동', '합동'],
     req=['BID-DOC-005', 'BID-DOC-001'], fc=['BID-DOC-005', 'BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-054', flow='고객 서류 읽기', kind='예외', title='관리자가 아니면 고객 서류 읽기 결과를 보거나 다시 읽힐 수 없고 서명 없는 서류 내려받기는 막힌다',
     pre='회원 계정 로그인 토큰, 읽음 줄이 있는 ANALYZING 입찰 건',
     steps='1. 회원 토큰으로 GET /admin/bid-requests/{id}/document-readings 를 부른다\n2. 회원 토큰으로 POST /admin/bid-requests/{id}/document-readings/run 을 부른다\n'
           '3. X-API-Key 머리글과 sig 없이 GET /mcp/document-readings/{readingId}/download 를 부른다',
     expect='1. 1단계 응답이 403 이다\n'
            '2. 2단계 응답이 403 이고 document_readings 줄들의 status 가 부르기 전과 같다\n'
            '3. 3단계 응답이 401 이고 본문이 비어 있다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b041_done_bid'),
                           ('api', USER, 'GET', '/api/admin/bid-requests/{val:0:id}/document-readings')],
                    check="r[1]['status'] == 403"),
            2: dict(steps=[('key', '10.b041_done_bid'),
                           ('sql', reading_rows(THIS_BID)),
                           ('api', USER, 'POST', '/api/admin/bid-requests/{val:0:id}/document-readings/run', {'readingIds': None}),
                           ('sql', reading_rows(THIS_BID))],
                    check="r[2]['status'] == 403 and r[3] == r[1]"),
            3: dict(steps=[('key', '10.b054_reading'),
                           ('api', 'guest', 'GET', '/api/mcp/document-readings/{val:0:id}/download')],
                    check="r[1]['status'] == 401 and not r[1]['body']")}),

]
