# FUB (For Your Buying)

FUB는 사용자의 구매 이력, 개인 위키, 일정, 상품 가격 이력을 함께 분석해 **무엇을 살지**뿐 아니라 **언제 사야 하는지**까지 제안하는 선행구매 쇼핑 서비스입니다.

이 문서는 현재 코드와 데이터베이스로 동작하는 기능만 설명합니다.

## 핵심 가치

- **생활 맥락 기반 추천**: 개인 위키와 일정에서 필요한 상품을 찾습니다.
- **구매 시점 판단**: 최근 30일 가격 이력으로 최저가, 최저가 근접, 다음 확인일을 계산합니다.
- **실상품 연결**: 네이버쇼핑과 11번가 검색 결과를 상품 상세 및 판매처로 연결합니다.
- **설명 가능한 Agent**: 추천 상품, 일정 마감일, 가격 표본과 판단 근거를 함께 반환합니다.
- **사용자 통제형 개인화**: 사용자가 개인 위키를 직접 조회, 수정, 보관할 수 있습니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React 19, Vite 8, Tailwind CSS 4, React Router 7, Redux Toolkit, Axios |
| Backend | Java 21, Spring Boot 4.1, Spring MVC, Spring Security, Spring Data JPA |
| Database | PostgreSQL, JSONB |
| AI | OpenAI Responses API 연동, 규칙 기반 응답 대체 경로 |
| External Commerce | Naver Shopping Search API, 11st Open API |
| Messaging | RabbitMQ 상품 수집 이벤트 발행 |
| Build & Runtime | Maven, npm, Docker (Eclipse Temurin 21) |

## 시스템 구성

```mermaid
flowchart LR
    User["사용자"] --> Web["React SPA"]
    Web --> API["Spring Boot API<br/>/delivery"]

    subgraph Backend["FUB Backend"]
        API --> Auth["인증·회원"]
        API --> Product["상품·가격"]
        API --> Wiki["개인 위키"]
        API --> Calendar["일정 준비"]
        API --> Agent["Agent Orchestrator"]

        Agent --> Harness["Briefing Harness"]
        Harness --> Product
        Harness --> Wiki
        Harness --> Calendar
        Agent --> Audit["세션·실행·판단 감사"]
    end

    Product --> Naver["Naver Shopping API"]
    Product --> Eleven["11st Open API"]
    Agent --> OpenAI["OpenAI API"]

    Auth --> DB[("PostgreSQL")]
    Product --> DB
    Wiki --> DB
    Calendar --> DB
    Audit --> DB
```

## 구현 기능 명세

### 1. 회원과 인증

| 기능 | 동작 |
| --- | --- |
| 회원가입 | 이메일 중복 확인 후 BCrypt로 비밀번호를 해시해 저장합니다. |
| 로그인 | 서버 RSA 공개키로 이메일과 비밀번호를 암호화해 전송하고, 검증 후 Access/Refresh JWT 쿠키를 발급합니다. |
| 로그인 상태 | JWT의 `userSq`를 기준으로 회원별 데이터에 접근합니다. |
| 로그아웃 | 인증 쿠키, 세션, Security Context를 함께 초기화합니다. |

### 2. 개인화 온보딩과 개인 위키

- 회원가입 후 가구원 수, 반려동물, 쇼핑 기준, 관심 카테고리, 음식, 요리 빈도, 취미, 추가 요청을 저장합니다.
- 위키 항목은 사용자 ID와 카테고리·키 조합으로 분리됩니다.
- 마이페이지에서 활성 위키를 조회하고 내용을 수정하거나 보관할 수 있습니다.
- 대화에서 발견한 중요 정보는 즉시 확정하지 않고 위키 후보로 저장한 뒤 사용자 승인을 요구합니다.
- 수정 전후 데이터는 버전별 이력으로 보관합니다.

### 3. 상품 탐색과 개인화

| 기능 | 비로그인 | 로그인 |
| --- | --- | --- |
| 홈 피드 | 생활용품 중심의 네이버쇼핑 상품 | 구매 클릭 이력을 우선하고, 이력이 없으면 개인 위키 관심사를 사용 |
| 검색 | 네이버쇼핑·11번가 실상품 통합 검색 | 같은 검색 결과에 회원별 구매 행동 정렬 적용 |
| 정렬 | 최저가순, 인기순 | 최저가순, 인기순, 내 구매순 |
| 상품 상세 | 상품명, 가격, 판매처, 카테고리, 이미지 | 동일 |
| 상품 요약 | 저장된 상품 속성에 한정한 AI 요약 | 동일 |

개인화 키워드 우선순위는 다음과 같습니다.

1. 최근 구매 클릭 이력
2. 활성 개인 위키
3. 기본 인기 카테고리

### 4. 가격 이력과 구매 타이밍

- 상품과 판매처별 제안을 분리해 저장합니다.
- 기본 상품군 가격을 주기적으로 수집하고 판매처별 가격 이력을 누적합니다.
- 최근 30일의 현재가, 최저가, 평균가와 표본 수를 사용합니다.
- 현재가가 최저가와 같으면 `30일 최저가`, 최저가의 3% 이내면 `최저가 근접`으로 판단합니다.
- 표본이 3개 이상이면 단순 가격 추세를 계산하고 최대 14일 범위의 다음 가격 확인일을 제안합니다.
- 일정 상품은 예상 가격일보다 `recommendedBuyBy`를 우선해 배송 준비가 늦어지지 않게 합니다.

### 5. 일정 기반 준비물

- 회원별로 저장된 향후 일정을 조회합니다.
- 일정 제목, 설명, 장소, 시작일을 기반으로 준비 키워드와 필요 이유를 연결합니다.
- 일정별 추천 상품, 우선순위, 구매 마감일을 제공합니다.
- AI 브리핑은 앞으로 30일의 일정 준비물을 가격 분석 결과와 함께 표시합니다.

### 6. AI 브리핑과 쇼핑 채팅

- 일정 준비 상품, 최근 가격 기회, 기다려볼 상품을 하나의 브리핑으로 구성합니다.
- 채팅은 상품 추천, 가격 비교, 구매 시점, 주문·배송 준비 범위로 제한됩니다.
- 사용자 질문과 직접 관련된 추천만 골라 상품 카드와 함께 반환합니다.
- Agent 입력은 구매 클릭, 개인 위키, 일정, 상품 제안, 가격 이력, 최근 대화 DB에서 구성합니다.
- OpenAI 호출에 실패하거나 키가 없으면 동일한 근거 데이터로 규칙 기반 응답을 반환합니다.
- 모든 실행은 `traceId`, 라우팅 판단, 모델, 프롬프트 버전과 함께 저장됩니다.

### 7. 구매 행동 기록

- 판매처 링크 클릭을 구매 의향 이벤트로 기록합니다.
- 클릭 시점 가격, 상품, 판매처, 화면 출처를 회원별로 저장합니다.
- 기록은 구매 목록과 이후 관심 카테고리·재구매 추천의 근거로 사용합니다.
- 링크 클릭은 실제 결제 완료가 아니라 구매 의향 지표로 취급합니다.

## 화면 경로

| 경로 | 화면 | 접근 |
| --- | --- | --- |
| `/app/main` | 개인화 홈 | 전체 |
| `/app/search` | 실상품 검색·정렬 | 전체 |
| `/app/products/:productSq` | 상품 상세·판매처 연결 | 전체 |
| `/app/briefing` | AI 브리핑·쇼핑 채팅 | 전체, 로그인 시 개인화 |
| `/app/imports` | 구매 클릭 기반 구매 목록 | 로그인 |
| `/app/calendar` | 저장 일정과 준비물 | 로그인 |
| `/app/my` | 회원 정보·개인 위키 관리 | 로그인 |
| `/app/login` | 로그인 | 전체 |
| `/app/signup` | 회원가입·개인화 온보딩 | 전체 |

## Agent 목록

| Agent | 책임 | 주요 입력 | 출력 |
| --- | --- | --- | --- |
| `CONVERSATION_ORCHESTRATOR` | 쇼핑 범위 판별과 담당 Agent 라우팅 | 사용자 메시지 | 담당 Agent 또는 범위 제한 안내 |
| `BRIEFING_SHOPPING` | 일정·관심사·가격을 합친 구매 브리핑 | 홈 피드, 일정 추천, 가격 이력 | 지금 구매·대기·기한 내 구매 상품 |
| `CALENDAR_PREPARATION` | 일정별 준비물과 주문 마감 계산 | 일정, 준비 키워드, 상품 제안 | 준비 상품, 필요 이유, 구매 마감일 |
| `PRICE_INTELLIGENCE` | 최근 가격 표본 기반 구매 시점 분석 | 현재가, 30일 최저·평균가, 표본 | 최저가 여부, 추세, 다음 확인일 |
| `PERSONAL_WIKI` | 개인화 정보 조회와 변경 후보 관리 | 사용자 입력, 기존 위키 | 위키 조회 또는 승인 대기 후보 |

## Agent 처리 시퀀스

```mermaid
sequenceDiagram
    actor U as 사용자
    participant UI as React UI
    participant AC as AgentController
    participant AS as AgentService
    participant M as 개인정보 마스커
    participant H as Briefing Harness
    participant DB as PostgreSQL
    participant LLM as OpenAI API

    U->>UI: 쇼핑·가격·일정 질문
    UI->>AC: 메시지 전송
    AC->>AS: userSq + sessionSq + message
    AS->>M: 메시지와 컨텍스트 마스킹
    AS->>AS: 범위 검사 및 Agent 라우팅
    AS->>DB: 최근 대화·위키 조회
    AS->>H: 담당 Agent 컨텍스트 요청
    H->>DB: 구매 클릭·일정·상품·가격 이력 조회
    DB-->>H: 사용자 범위 데이터
    H-->>AS: 추천 상품과 판단 근거
    AS->>LLM: 마스킹된 근거와 응답 지침
    alt OpenAI 응답 성공
        LLM-->>AS: 자연어 요약
    else 호출 불가 또는 실패
        AS->>AS: 규칙 기반 응답 사용
    end
    AS->>DB: 메시지·실행·라우팅 판단 저장
    AS-->>AC: 응답 + 상품 카드
    AC-->>UI: 공통 API 응답
    UI-->>U: 답변과 상품 상세 링크
```

## 핵심 데이터 모델

```mermaid
erDiagram
    USER ||--o{ WIKI_ENTRY : owns
    WIKI_ENTRY ||--o{ WIKI_ENTRY_HISTORY : versions

    USER ||--o{ PURCHASE_CLICK : records
    PRODUCT ||--o{ PRODUCT_OFFER : has
    PRODUCT_OFFER ||--o{ PRICE_HISTORY : tracks
    PRODUCT ||--o{ PURCHASE_CLICK : targets
    PRODUCT_OFFER ||--o{ PURCHASE_CLICK : selects

    USER ||--o{ CALENDAR_EVENT : owns
    CALENDAR_EVENT ||--o{ CALENDAR_SUGGESTION : generates
    PRODUCT o|--o{ CALENDAR_SUGGESTION : recommends
    PRODUCT_OFFER o|--o{ CALENDAR_SUGGESTION : links

    USER ||--o{ AGENT_SESSION : owns
    AGENT_SESSION ||--o{ AGENT_MESSAGE : contains
    AGENT_SESSION ||--o{ AGENT_RUN : executes
    AGENT_RUN ||--o{ AGENT_DECISION : records
    AGENT_RUN o|--o{ AGENT_MESSAGE : produces

    USER {
        bigint user_sq PK
        string email
        string password
        string status
    }
    PRODUCT {
        bigint product_sq PK
        string product_key UK
        string name
        string category_path
        string image_url
    }
    PRODUCT_OFFER {
        bigint offer_sq PK
        bigint product_sq FK
        string provider
        string external_product_id
        bigint total_price
    }
    PRICE_HISTORY {
        bigint price_history_sq PK
        bigint offer_sq FK
        bigint total_price
        timestamp collected_at
    }
    PURCHASE_CLICK {
        bigint purchase_click_sq PK
        bigint user_sq FK
        bigint product_sq FK
        bigint offer_sq FK
        string source_context
    }
    CALENDAR_EVENT {
        bigint calendar_event_sq PK
        bigint user_sq FK
        string title
        timestamp starts_at
    }
    CALENDAR_SUGGESTION {
        bigint calendar_suggestion_sq PK
        bigint calendar_event_sq FK
        date recommended_buy_by
        string priority
    }
    WIKI_ENTRY {
        bigint wiki_entry_sq PK
        bigint user_sq FK
        string category
        string entry_key
        jsonb content_json
        int version
    }
    WIKI_ENTRY_HISTORY {
        bigint wiki_history_sq PK
        bigint wiki_entry_sq FK
        jsonb snapshot_json
    }
    AGENT_SESSION {
        bigint agent_session_sq PK
        bigint user_sq FK
        jsonb context_json
    }
    AGENT_MESSAGE {
        bigint agent_message_sq PK
        bigint agent_session_sq FK
        bigint agent_run_sq FK
        string role
        jsonb payload_json
    }
    AGENT_RUN {
        bigint agent_run_sq PK
        bigint agent_session_sq FK
        string agent_type
        string trace_id UK
    }
    AGENT_DECISION {
        bigint agent_decision_sq PK
        bigint agent_run_sq FK
        string decision_type
        jsonb evidence_json
    }
```

## API 명세

모든 API는 기본적으로 `/delivery` 컨텍스트 경로를 사용합니다.

- `/wp/**`: 비로그인 사용이 가능한 API
- `/wb/**`: JWT 회원 컨텍스트가 필요한 API

공통 응답 형식:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "success",
  "data": {}
}
```

### 인증

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/wp/user/crypto-public-key` | 로그인 입력 암호화용 RSA 공개키 조회 |
| `POST` | `/wp/user/signup` | 회원가입 |
| `POST` | `/wp/user/login` | RSA 암호문 검증 후 JWT 쿠키 발급 |
| `GET` | `/wb/user/me` | 현재 회원 조회 |
| `GET` | `/wb/user/logout` | 인증 쿠키와 세션 초기화 |

회원가입 요청:

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "8자 이상의 비밀번호"
}
```

### 상품·가격·구매 행동

| Method | Path | 주요 파라미터 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/wp/home/feed` | - | 비로그인 홈 상품 피드 |
| `GET` | `/wb/home/feed` | - | 구매 이력·위키 기반 개인화 홈 피드 |
| `GET` | `/wp/products` | `query`, `size`, `sort` | 실상품 통합 검색 |
| `GET` | `/wb/products` | `query`, `size`, `sort` | 회원별 검색·구매순 정렬 |
| `GET` | `/wp/products/{productSq}` | 상품 식별자 | 상품 상세와 안전한 AI 요약 |
| `GET` | `/wp/product-images/{provider}/{providerCode}` | `width=320|640|960` | 허용 도메인 원본 이미지 리사이즈 프록시 |
| `POST` | `/wp/products/{provider}/{providerCode}/purchase-click` | `sourceContext` | 비로그인 구매 클릭 기록 |
| `POST` | `/wb/products/{provider}/{providerCode}/purchase-click` | `sourceContext` | 회원별 구매 클릭 기록 |
| `GET` | `/wp/imports/items` | `status`, `page`, `size` | 회원 구매 클릭 기반 구매 목록 조회 |

`sort` 값은 `LOW_PRICE`, `POPULAR`, `PURCHASE`를 사용합니다.

구매 클릭 요청:

```json
{
  "sourceContext": "AGENT_CHAT"
}
```

`sourceContext`는 `HOME_HOT`, `HOME_DEAL`, `SEARCH`, `AGENT_CHAT`, `CALENDAR`, `OTHER` 중 하나입니다.

### 개인 위키

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/wb/agent/wiki` | 상태·카테고리·검색어 기준 위키 조회 |
| `POST` | `/wb/agent/wiki/onboarding` | 온보딩 응답을 개인 위키로 저장 |
| `POST` | `/wb/agent/wiki/entries` | 명시적 위키 항목 생성 |
| `POST` | `/wb/agent/wiki/entries/{wikiEntrySq}` | 사용자 소유 위키 항목 수정 |
| `POST` | `/wb/agent/wiki/entries/{wikiEntrySq}/archive` | 위키 항목 보관 |
| `POST` | `/wb/agent/wiki/entries/{wikiEntrySq}/confirm` | 대화에서 추출된 위키 후보 승인·거절 |

온보딩 요청:

```json
{
  "householdSize": 2,
  "pets": ["강아지"],
  "shoppingPriorities": ["최저가", "빠른 배송"],
  "preferredCategories": ["반려동물", "생활용품"],
  "favoriteFoods": ["한식"],
  "cookingFrequency": "주 3~4회",
  "hobbies": ["캠핑", "홈카페"],
  "prompt": "일정 이틀 전까지 준비물을 주문하고 싶어요."
}
```

### 일정과 Agent

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/wb/calendar/purchase-suggestions` | `from`~`to` 기간의 저장 일정과 준비 상품 조회 |
| `GET` | `/wb/agent/briefing/today` | 일정·가격·개인화 기반 오늘의 브리핑 |
| `POST` | `/wb/agent/sessions` | 통합 쇼핑 Agent 세션 생성 |
| `GET` | `/wb/agent/sessions/{sessionSq}` | 사용자 소유 세션과 메시지 조회 |
| `POST` | `/wb/agent/sessions/{sessionSq}/messages` | 쇼핑 질문 전송과 Agent 응답 생성 |

Agent 메시지 요청:

```json
{
  "text": "다음 캠핑 전에 필요한 물품과 구매 시점을 알려줘",
  "context": {
    "screen": "BRIEFING"
  }
}
```

## 보안 설계

- 비밀번호는 BCrypt 해시로 저장합니다.
- 로그인 입력은 매 요청용 RSA 키로 암호화해 전송합니다.
- 인증 정보는 Access/Refresh JWT의 HttpOnly 쿠키로 관리합니다.
- 위키, 일정, 구매 클릭, Agent 세션 조회는 `userSq`를 항상 조건으로 사용합니다.
- Agent 입력과 저장 데이터에서 이메일, 전화번호, 주민등록번호, 계좌·금융번호, 비밀번호·인증번호를 마스킹합니다.
- 개인 위키의 대화 추출 정보는 사용자 승인 전까지 후보 상태로 유지합니다.
- 상품 이미지 프록시는 HTTPS와 허용 호스트만 통과시키며 원본 크기와 출력 폭을 제한합니다.
- Agent 실행과 라우팅 판단은 추적 ID와 근거 JSON으로 감사할 수 있습니다.
- LLM에는 마스킹된 최소 컨텍스트만 전달하고, DB에 없는 가격·재고·일정을 추측하지 않도록 프롬프트에서 제한합니다.

## 로컬 실행

### 요구 사항

- Java 21+
- Maven 3.9+
- Node.js 20.19+
- npm 10+
- PostgreSQL 17+

### 환경 변수

프로젝트 루트의 `.env`는 로컬 전용이며 Git에 커밋하지 않습니다.

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fub
SPRING_DATASOURCE_USERNAME=fub
SPRING_DATASOURCE_PASSWORD=change-me
DATABASE_URL=postgresql://fub:change-me@localhost:5432/fub
JWT_REAL_KEY=64-bytes-or-longer-secret

NAVER_CLIENT_ID=
NAVER_SECRET=
ELEVENST_SECRET=

OPENAI_API_KEY=
FUB_FRONTEND_BASE_URL=http://localhost:3500

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

`OPENAI_API_KEY`가 없거나 호출에 실패해도 규칙 기반 Agent 응답은 동작합니다. 외부 상품 실데이터에는 네이버 또는 11번가 API 키가 필요합니다.

### 데이터베이스

JPA는 `ddl-auto: none`을 사용합니다. 운영 DB의 회원 테이블을 기준으로 아래 기능 스키마를 순서대로 적용합니다.

```bash
psql "$DATABASE_URL" -f src/main/resources/db/product-price-schema.sql
psql "$DATABASE_URL" -f src/main/resources/db/agent-session-schema.sql
psql "$DATABASE_URL" -f src/main/resources/db/agent-audit-wiki-schema.sql
psql "$DATABASE_URL" -f src/main/resources/db/calendar-agent-schema.sql
```

### 개발 서버

Backend:

```bash
mvn spring-boot:run
```

- API: `http://localhost:30100/delivery`

Frontend:

```bash
cd frontend/foryour
npm ci
npm run dev
```

- Web: `http://localhost:3500/app/main`
- Vite가 `/delivery/**` 요청을 로컬 Backend로 프록시합니다.

### 통합 빌드

```bash
mvn -Preal clean package
java -jar target/delivery.jar
```

`real` 프로필은 Vite 결과물을 Spring 정적 리소스에 포함하고 `target/delivery.jar`를 생성합니다.

## 검증

```bash
mvn -Dmaven.test.skip=false test

cd frontend/foryour
npm run lint
npm run build
```

## 프로젝트 구조

```text
.
├── frontend/foryour/                 # React SPA
│   └── src/
│       ├── pages/                    # 홈, 검색, 상세, 브리핑, 일정, 마이페이지
│       ├── services/                 # Backend API 어댑터
│       └── shared/                   # 인증, API, 공통 UI
├── src/main/java/com/foryour/delivery/
│   ├── client/
│   │   ├── agent/                    # Agent 라우팅, 하네스, OpenAI 연동
│   │   ├── calendar/                 # 일정 저장·준비물 추천
│   │   ├── product/                  # 상품 검색, 가격, 이미지, 구매 클릭
│   │   ├── user/                     # 회원가입·로그인
│   │   └── wiki/                     # 개인 위키와 온보딩
│   ├── config/                       # JWT, Security, 애플리케이션 설정
│   └── domain/                       # Entity, Repository, Enum
├── src/main/resources/db/            # 기능별 PostgreSQL 스키마
├── src/main/resources/static/        # 운영 Frontend 빌드 결과
├── Dockerfile
└── pom.xml
```

## 가격 분석 정책 요약

| 정책 | 값 |
| --- | --- |
| 분석 기간 | 최근 30일 |
| 예측 최소 표본 | 3개 |
| 최저가 근접 기준 | 최저가 대비 3% 이내 |
| 예상 확인일 상한 | 14일 |
| 일정 우선 정책 | 예상 최적일보다 구매 마감일 우선 |
| 가격 예측 방식 | 최근 수집 가격의 단순 선형 추세 |

가격 확인일은 확정 최저가 날짜가 아니라, 수집된 표본을 기준으로 다시 확인할 시점을 제안하는 값입니다.
