# SAM.gov Bidding Agency Platform

**AI 기반 프리미엄 입찰 대행 플랫폼** - 주한미군(USFK) 및 미국 정부 입찰 프로세스 자동화

## 📋 프로젝트 개요

SAM.gov에서 공고를 자동 수집하고, AI가 요구사항을 분석하여 입찰 문서를 생성하며, 검증 및 제출까지 전체 프로세스를 대행하는 B2B 플랫폼입니다.

### 핵심 기능

- **자동 공고 수집**: SAM.gov API 연동, 하루 4회 자동 수집 (06:00, 12:00, 18:00, 23:00 KST)
- **AI 문서 생성**: GPT-4/Claude 기반 입찰 문서 초안 자동 생성
- **수동 편집**: Rich text 에디터로 문서 편집 + 버전 관리
- **BLOCKER 시스템**: 필수 요구사항 누락 시 제출 차단
- **FSM 기반 워크플로우**: 입찰 신청 상태를 체계적으로 관리
- **고객 승인 필수**: 제출 전 고객 확인 + 전자 동의
- **증빙 아카이브**: 제출 증거 보관 (timestamp, hash, screenshots)

---

## 🏗️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.2 + Java 17
- **Build Tool**: Gradle 8.5
- **Database**: MariaDB 11.2
- **Cache/Queue**: Redis 7.2
- **Storage**: MinIO (S3-compatible)
- **AI**: OpenAI GPT-4 / Claude 3.5 Sonnet
- **Security**: JWT + Spring Security
- **Monitoring**: Prometheus + Grafana (optional)

### Frontend
- **Framework**: React 18 + Vite 7
- **Language**: TypeScript 5
- **Styling**: TailwindCSS 3
- **State**: Zustand (권장)
- **HTTP Client**: Axios
- **Form**: React Hook Form

### DevOps
- **Containerization**: Docker + Docker Compose
- **CI/CD**: GitHub Actions (권장)
- **Deployment**: Kubernetes (권장)

---

## 📁 프로젝트 구조

```
bidding-agency-platform/
├── backend/                          # Spring Boot API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/biddingagency/
│   │   │   │   ├── BiddingAgencyApplication.java
│   │   │   │   ├── config/          # 설정 (Security, Redis, S3 등)
│   │   │   │   ├── domain/          # Domain-driven design
│   │   │   │   │   ├── opportunity/ # 공고 수집 및 요구사항 추출
│   │   │   │   │   ├── bid/         # 입찰 신청 FSM + 문서 생성
│   │   │   │   │   ├── compliance/  # 검증 및 Compliance Matrix
│   │   │   │   │   └── submission/  # 제출 및 증빙 아카이브
│   │   │   │   ├── controller/      # REST API
│   │   │   │   ├── service/         # 비즈니스 로직
│   │   │   │   ├── repository/      # JPA Repository
│   │   │   │   └── dto/             # DTO 및 Mapper
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/    # Flyway SQL scripts
│   │   └── test/
│   ├── build.gradle
│   └── settings.gradle
├── frontend/
│   ├── customer-portal/              # 고객 포털 (React + Vite)
│   │   ├── src/
│   │   │   ├── components/
│   │   │   ├── pages/
│   │   │   ├── hooks/
│   │   │   ├── services/            # API clients
│   │   │   └── utils/
│   │   ├── package.json
│   │   └── vite.config.ts
│   └── admin-console/                # 관리자 콘솔 (React + Vite)
│       ├── src/
│       │   ├── features/
│       │   │   ├── opportunities/
│       │   │   ├── bid-requests/
│       │   │   ├── documents/       # TipTap 에디터
│       │   │   └── compliance/
│       │   ├── components/
│       │   └── services/
│       └── package.json
├── docker/
│   ├── docker-compose.yml            # MariaDB, Redis, MinIO
│   └── prometheus/
├── docs/                             # 추가 문서
├── .env.example
├── .gitignore
└── README.md
```

---

## 🚀 빠른 시작

### 1. 사전 요구사항

- **Java**: 17 이상
- **Node.js**: 18.20 이상 (권장: 20.x)
- **Docker & Docker Compose**: 최신 버전
- **Git**: 2.x

### 2. 저장소 클론

```bash
git clone <repository-url>
cd bidding-agency-platform
```

### 3. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일 편집하여 API 키 등 설정
```

**필수 설정**:
- `SAM_GOV_API_KEY`: SAM.gov API 키 ([신청 방법](https://sam.gov/content/api))
- `OPENAI_API_KEY`: OpenAI API 키
- `JWT_SECRET`: 256비트 이상의 안전한 시크릿

### 4. 인프라 실행 (Docker Compose)

```bash
cd docker
docker-compose up -d
```

**실행 서비스**:
- MariaDB: `localhost:3306`
- Redis: `localhost:6379`
- MinIO: `localhost:9000` (API), `localhost:9001` (Console)

**MinIO 접속**:
- URL: http://localhost:9001
- ID: `minioadmin`
- PW: `minioadmin123`

### 5. Backend 실행

```bash
cd ../backend
./gradlew bootRun
```

**확인**:
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- Actuator: http://localhost:8080/api/actuator/health

### 6. Frontend 실행

#### Customer Portal

```bash
cd ../frontend/customer-portal
npm install
npm run dev
```
→ http://localhost:5173

#### Admin Console

```bash
cd ../frontend/admin-console
npm install
npm run dev
```
→ http://localhost:5174

---

## 📖 주요 개념

### FSM (Finite State Machine) - 입찰 신청 상태

```
CREATED → WAITING_FOR_CLIENT_DOCS → CLIENT_DOCS_RECEIVED →
REQUIREMENT_ANALYSIS → DOCUMENT_DRAFTING → INTERNAL_REVIEW →
CLIENT_CONFIRMATION → READY_FOR_SUBMISSION → SUBMITTING →
SUBMITTED → CLOSED
```

- **BLOCKER**: 필수 요구사항 미충족 시 다음 상태로 전이 불가
- **CLIENT_CONFIRMATION**: 제출 전 고객 승인 필수
- **LOCKED**: 제출 후 문서 수정 불가 (정정은 복제 후 수정)

### 문서 버전 관리

- 모든 저장 시 새 버전 생성 (불변)
- 버전 히스토리 및 롤백 지원
- 제출 시 해당 버전 LOCK → 법적 증빙

### Compliance 검증

- 요구사항 ↔ 문서 섹션 매핑
- Compliance Matrix 자동 생성
- BLOCKER 검출 시 제출 차단

---

## 🗄️ 데이터베이스

### 핵심 테이블

- **bid_requests**: FSM 상태 관리 (state, state_history JSONB)
- **bid_documents**: 문서 컨테이너 (status, current_version_no)
- **bid_document_versions**: 불변 버전 (content_json JSONB)
- **opportunity_requirement_items**: 요구사항 (requirement_json JSONB)
- **requirement_fulfillment_maps**: Compliance 매핑
- **event_logs**: 감사 로그 (audit trail)

### 마이그레이션

Flyway로 관리 (`backend/src/main/resources/db/migration/`)

```bash
# 마이그레이션 실행 (자동)
./gradlew bootRun
```

---

## 🔐 보안

- **JWT**: Access Token (15분) + Refresh Token (7일)
- **RBAC**: ADMIN, ANALYST, WRITER, REVIEWER, SUBMITTER
- **Audit Log**: 모든 상태 변경/문서 편집 기록
- **File Upload**: 타입/크기 검증, 최대 50MB
- **CORS**: 허용 Origin 설정 (application.yml)

---

## 📊 모니터링

### Actuator Endpoints

- Health: `/api/actuator/health`
- Metrics: `/api/actuator/metrics`
- Prometheus: `/api/actuator/prometheus`

### Prometheus + Grafana (Optional)

```bash
docker-compose --profile monitoring up -d
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin123)

---

## 🧪 테스트

### Backend

```bash
cd backend
./gradlew test
./gradlew integrationTest  # (설정 후)
```

### Frontend

```bash
cd frontend/customer-portal
npm test

cd ../admin-console
npm test
```

---

## 📚 API 문서

### Swagger UI

http://localhost:8080/api/swagger-ui.html

### 주요 엔드포인트

#### Opportunity

- `GET /api/opportunities` - 공고 리스트
- `GET /api/opportunities/{id}` - 공고 상세
- `POST /api/admin/opportunities` - 수동 입력

#### Bid Request

- `POST /api/bid-requests` - 입찰 신청 생성
- `GET /api/bid-requests/{id}` - 상세 + 타임라인
- `POST /api/bid-requests/{id}/approve` - 최종 승인

#### Document

- `POST /api/documents/{docId}/generate` - AI 생성
- `POST /api/documents/{docId}/versions` - 버전 저장
- `POST /api/documents/{docId}/export/pdf` - PDF 내보내기

#### Compliance

- `POST /api/bid-requests/{bidId}/compliance/validate` - 검증 실행
- `GET /api/bid-requests/{bidId}/compliance/blockers` - BLOCKER 조회

---

## 📝 개발 가이드

### 코드 스타일

- **Backend**: Google Java Style Guide
- **Frontend**: Airbnb React Style Guide
- **Formatter**: Prettier (프론트), Spotless (백)

### Git Workflow

```bash
# Feature 브랜치 생성
git checkout -b feature/opportunity-collection

# 커밋
git commit -m "feat: Add SAM.gov API client"

# Push
git push origin feature/opportunity-collection
```

### 브랜치 전략

- `main`: Production
- `develop`: Development
- `feature/*`: 기능 개발
- `bugfix/*`: 버그 수정
- `hotfix/*`: 긴급 수정

---

## 🔧 트러블슈팅

### MariaDB 연결 실패

```bash
# Docker 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs bidding-agency-mariadb
```

### Redis 연결 실패

```bash
# Redis CLI 테스트
docker exec -it bidding-agency-redis redis-cli
AUTH redis_password
PING
```

### Frontend 빌드 오류

```bash
# 캐시 삭제
rm -rf node_modules package-lock.json
npm install
```

---

## 📦 배포

### Production 빌드

#### Backend

```bash
cd backend
./gradlew clean build
java -jar build/libs/bidding-agency-platform-0.0.1-SNAPSHOT.jar
```

#### Frontend

```bash
cd frontend/customer-portal
npm run build
# dist/ 폴더를 정적 파일 서버에 배포

cd ../admin-console
npm run build
```

### Docker 배포

```bash
# 추후 Dockerfile 추가 예정
```

---

## 🤝 기여

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 라이선스

추후 결정

---

## 📞 연락처

프로젝트 관련 문의: [이메일 주소]

---

## 🗺️ 로드맵

### Phase 1 (MVP) - 22-24주
- [x] 프로젝트 초기화
- [ ] SAM.gov 자동 수집
- [ ] AI 문서 생성
- [ ] FSM 기반 워크플로우
- [ ] 고객 포털 + 관리자 콘솔
- [ ] Compliance 검증
- [ ] 제출 증빙 관리

### Phase 2 (Enhancement)
- [ ] 자동 제출 엔진 (지원 채널)
- [ ] 고급 협업 기능
- [ ] Vector DB 기반 RAG
- [ ] 성공 보수 자동 정산
- [ ] 모바일 앱

---

## ⚙️ 설정 참고

### SAM.gov API Key 발급

1. https://sam.gov/content/api 방문
2. 계정 생성 및 API 키 요청
3. `.env` 파일에 `SAM_GOV_API_KEY` 설정

### OpenAI API Key 발급

1. https://platform.openai.com/api-keys 방문
2. API 키 생성
3. `.env` 파일에 `OPENAI_API_KEY` 설정

---

**Made with ❤️ for USFK Bidding Automation**
