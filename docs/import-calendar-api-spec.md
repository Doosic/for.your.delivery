# 구매목록/구글 캘린더 연동 API 명세 v0.1

## 공통

- Base URL: `/delivery`
- 인증: 로그인 세션 쿠키 기반. 모든 연동 API는 로그인 필요.
- 공통 응답:

```json
{
  "success": true,
  "statusCode": 100,
  "message": "ok",
  "data": {}
}
```

## 1. 연동 소스 목록 조회

- Method: `GET`
- Path: `/wp/import-connections`
- 설명: 사용자의 구매목록/캘린더 연동 상태를 조회한다.

Response `data`:

```json
{
  "connections": [
    {
      "source": "NAVER",
      "label": "네이버",
      "connected": true,
      "lastSyncedAt": "2026-07-21T07:40:00+09:00",
      "autoSync": "DAILY_0600"
    },
    {
      "source": "GOOGLE_CALENDAR",
      "label": "Google Calendar",
      "connected": false,
      "lastSyncedAt": null,
      "autoSync": null
    }
  ]
}
```

## 2. OAuth 연결 시작

- Method: `POST`
- Path: `/wp/import-connections/oauth/start`
- 설명: 외부 서비스 OAuth 동의 화면 URL을 생성한다.

Request:

```json
{
  "source": "GOOGLE_CALENDAR",
  "redirectUri": "http://localhost:3500/imports/connections"
}
```

Response `data`:

```json
{
  "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?...",
  "state": "opaque-state-token"
}
```

## 3. OAuth 콜백 처리

- Method: `POST`
- Path: `/wp/import-connections/oauth/callback`
- 설명: OAuth code를 받아 access/refresh token을 서버에 저장한다.

Request:

```json
{
  "source": "GOOGLE_CALENDAR",
  "code": "oauth-code",
  "state": "opaque-state-token"
}
```

Response `data`:

```json
{
  "source": "GOOGLE_CALENDAR",
  "connected": true,
  "connectedAt": "2026-07-21T17:30:00+09:00"
}
```

## 4. 연동 해제

- Method: `DELETE`
- Path: `/wp/import-connections/{source}`
- 설명: 저장된 외부 서비스 토큰과 자동 동기화 설정을 해제한다.

Response `data`:

```json
{
  "source": "GOOGLE_CALENDAR",
  "connected": false
}
```

## 5. 구매목록 동기화 요청

- Method: `POST`
- Path: `/wp/imports/sync`
- 설명: 연결된 구매 소스에서 주문/구매내역을 가져와 정규화한다.

Request:

```json
{
  "sources": ["NAVER", "COUPANG", "GMAIL"],
  "from": "2026-06-21",
  "to": "2026-07-21"
}
```

Response `data`:

```json
{
  "syncJobSq": 101,
  "status": "DONE",
  "importedCount": 5,
  "newCount": 3
}
```

## 6. 가져온 구매목록 조회

- Method: `GET`
- Path: `/wp/imports/items`
- Query: `status=NEW|COMMITTED|IGNORED`, `page=0`, `size=20`
- 설명: 동기화된 구매목록 후보를 조회한다.

Response `data`:

```json
{
  "items": [
    {
      "importedItemSq": 1,
      "source": "COUPANG",
      "name": "고양이 사료 오리진 1.5kg",
      "quantity": 1,
      "price": 32400,
      "purchasedAt": "2026-07-21",
      "normalizedCategory": "PET_FOOD",
      "status": "NEW"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

## 7. 구매목록 재고 반영

- Method: `POST`
- Path: `/wp/imports/items/commit`
- 설명: 선택한 구매목록을 사용자 재고/소진 예측 데이터에 반영한다.

Request:

```json
{
  "importedItemSqs": [1, 2, 3]
}
```

Response `data`:

```json
{
  "committedCount": 3,
  "inventoryItemSqs": [11, 12, 13]
}
```

## 8. 구글 캘린더 일정 동기화

- Method: `POST`
- Path: `/wp/calendar/sync`
- 설명: Google Calendar에서 다가오는 일정을 가져와 선행구매 후보를 생성한다.

Request:

```json
{
  "calendarIds": ["primary"],
  "from": "2026-07-21",
  "to": "2026-08-20"
}
```

Response `data`:

```json
{
  "syncJobSq": 202,
  "eventCount": 3,
  "suggestionCount": 5
}
```

## 9. 일정 기반 추천 조회

- Method: `GET`
- Path: `/wp/calendar/purchase-suggestions`
- Query: `from=2026-07-21`, `to=2026-08-20`
- 설명: 일정별 필요한 물품과 구매 마감일 추천을 조회한다.

Response `data`:

```json
{
  "suggestions": [
    {
      "calendarEventSq": 1,
      "title": "주말 캠핑",
      "startsAt": "2026-07-25T09:00:00+09:00",
      "location": "가평",
      "items": [
        {
          "name": "모기퇴치제",
          "reason": "야외 숙박 일정",
          "recommendedBuyBy": "2026-07-23",
          "priority": "HIGH"
        }
      ]
    }
  ]
}
```

