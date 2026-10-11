# 실행맵 - AWS 이전 뒤 AI 작업 Max 연결

> 처음: 3건 (10/12) · 지금: 3건 = 계획 2 + 추가 1
> 근거: 사용자 10/12 06:10 "1"(4개 먼저 Max, 도구 쓰는 5개와 수정 채팅은 시험 뒤), 07:23 "1"(Aimbase 결함을 Aimbase 레포에서 고치고 AWS 에 올린 뒤 5개와 채팅 이동).
> 운영: 상주 컨타 없이 CR-150 창(d68e19dd)이 Aimbase 워커 하나에 요청하고 결과를 받는다. 결재는 사용자.

| CR | 무엇 | 레포 | 워커 | 갈래 | 상태 |
|---|---|---|---|---|---|
| CR-148 | 관리자 공고문 수정 채팅을 https 경로로 열기 | bidding-agency-platform | d68e19dd | 계획 | 완료 |
| CR-150 | Aimbase AI 작업을 Max 구독 연결로 옮김(4개 완료, 5개와 수정 채팅 남음) | bidding-agency-platform | d68e19dd | 계획 | 진행중 |
| Aimbase CR-146 | Max 연결의 앱 도구 호출이 앱 재기동 뒤 옛 연결로 실패하는 결함 고침과 AWS Aimbase 반영 | aimbase | 60940450 | 추가 | 시험중 |

## CR 아닌 처리

- 10/12 Aimbase(AWS) 워크플로 4개(correct-notice-analysis, requirement-extraction, summarize-opportunity-for-selection, type-pattern-extraction)의 연결을 cli-runner-bidding-001 로 바꿈. DB 데이터 변경, 백업 /opt/wjbid/aimbase/backup/workflows-connections-20261012-0611.sql
- 10/12 Aimbase(AWS) 연결 cli-runner-bidding-tools-001(tool_mode AIMBASE) 추가. DB 데이터 변경
