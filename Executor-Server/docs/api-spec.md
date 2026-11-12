# Executor(집행자) API 명세서

| 항목 | 내용 |
| --- | --- |
| Base URL | `https://{railway-domain}/api` (로컬: `http://localhost:8080/api`) |
| 형식 | `application/json; charset=UTF-8`, 필드 이름은 camelCase |
| 시간 | ISO-8601 UTC (예: `2026-11-09T12:00:00Z`) |
| Swagger | `/swagger-ui/index.html` |
| 관련 문서 | [요구사항 정의서](requirements.md), [DB 스키마](../src/main/resources/db/schema.sql) |

---

## 0. 공통 규칙

### 0.1 인증

- 🔓 표시가 있는 API를 빼면 모든 API에 Access Token이 필요하다.
  ```
  Authorization: Bearer {accessToken}
  ```
- 🛡️ 표시가 있는 API는 `ADMIN` 권한이 필요하다.
- Access Token이 만료되면 `401 TOKEN_EXPIRED`가 온다. 이때 클라이언트는 `POST /auth/reissue`를 호출한 뒤 원래 요청을 다시 보낸다.

### 0.2 성공 응답

- 성공하면 본문에 데이터를 그대로 담는다. 본문이 없는 경우에는 `204 No Content`를 보낸다.
- 페이지 단위 조회는 쿼리 파라미터 `page`(0부터 시작, 기본 0)와 `size`(기본 20, 최대 50)를 쓰고, 아래 형식으로 응답한다.
  ```json
  {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 135,
    "totalPages": 7
  }
  ```

### 0.3 오류 응답

```json
{
  "status": 400,
  "code": "INVALID_INPUT",
  "message": "닉네임은 2~20자여야 합니다."
}
```

| HTTP | code | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 요청 값 검증 실패 |
| 401 | `UNAUTHORIZED` | 토큰이 없거나 잘못됨 |
| 401 | `TOKEN_EXPIRED` | Access Token 만료 |
| 401 | `INVALID_REFRESH_TOKEN` | Refresh Token이 만료됐거나 무효화됨 |
| 401 | `INVALID_CREDENTIALS` | 이메일 또는 비밀번호가 틀림 |
| 403 | `FORBIDDEN` | 권한 없음 (다른 사람의 리소스, 관리자 전용 API) |
| 403 | `STAGE_LOCKED` | 아직 해금되지 않은 스테이지 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| 409 | `DUPLICATE_NICKNAME` | 이미 쓰고 있는 닉네임 |
| 409 | `NOT_ENOUGH_COIN` | 코인 부족 |
| 409 | `ALREADY_OWNED` | 이미 가지고 있는 스킨·테마 |
| 409 | `NOT_ENOUGH_ITEM` | 힌트 수량 부족 |
| 409 | `ALREADY_CLAIMED` | 이미 받은 우편 |
| 409 | `MAIL_EXPIRED` | 만료된 우편 |
| 409 | `ALREADY_FRIEND` | 이미 친구 |
| 409 | `REQUEST_ALREADY_EXISTS` | 대기 중인 친구 요청이 이미 있음 |
| 409 | `REQUEST_ALREADY_HANDLED` | 이미 수락·거절된 친구 요청 |
| 409 | `NOT_FRIEND` | 친구가 아닌 사용자에게 메시지 전송 |
| 409 | `NOT_VERIFIED` | 제작자가 클리어하지 않은 맵을 공개하려 함 |
| 409 | `PUBLISHED_STAGE` | 공개된 맵을 수정하려 함 |
| 409 | `OPERATION_HAS_STAGES` | 스테이지가 남아 있는 작전을 삭제하려 함 |
| 409 | `DUPLICATE_VALUE` | 관리자 등록·수정에서 겹칠 수 없는 값이 겹침 (작전 이름·순서, 같은 작전의 스테이지 번호, 도감 이름, 아이템 이름) |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

### 0.4 공통 객체

**UserSummary**: 다른 사용자를 표시할 때 쓴다.
```json
{ "userId": 12, "nickname": "debugger", "equippedSkinId": 3 }
```

---

## 1. API 목록

| 분류 | Method | URL | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| 인증 | POST | `/auth/signup` 🔓 | 회원가입 | AUTH-01~03 |
|  | POST | `/auth/login` 🔓 | 로그인 | AUTH-04 |
|  | POST | `/auth/reissue` 🔓 | 토큰 재발급 | AUTH-05 |
|  | POST | `/auth/logout` | 로그아웃 | AUTH-06 |
|  | GET | `/auth/check-email` 🔓 | 이메일 중복 확인 | AUTH-02 |
|  | DELETE | `/users/me` | 회원 탈퇴 | AUTH-07 |
| 프로필 | GET | `/profiles/check-nickname` 🔓 | 닉네임 중복 확인 | PROFILE-01 |
|  | GET | `/profiles/me` | 내 프로필 | PROFILE-03 |
|  | PATCH | `/profiles/me` | 내 프로필 수정 | PROFILE-02, 04 |
|  | PUT | `/profiles/me/equipment` | 스킨·테마 장착 | PROFILE-07 |
|  | GET | `/profiles/{userId}` | 다른 사용자 프로필 | PROFILE-05 |
|  | GET | `/profiles/search` | 닉네임 검색 | PROFILE-06 |
| 스테이지 | GET | `/operations` | 작전·스테이지 목록과 내 진행도 | STAGE-01 |
|  | GET | `/stages/{stageId}` | 스테이지 상세 | STAGE-02, 03 |
|  | POST | `/stages/{stageId}/results` | 플레이 결과 제출 | STAGE-04~08, GUIDE-01 |
|  | GET | `/stages/{stageId}/rankings` | 최소 턴 순위 | STAGE-09 |
|  | POST | `/stages/{stageId}/hint` | 힌트 사용 | SHOP-07 |
| 사용자 맵 | GET | `/custom-stages` | 공개 맵 목록 | CUSTOM-07 |
|  | GET | `/custom-stages/me` | 내가 만든 맵 목록 | CUSTOM-08 |
|  | POST | `/custom-stages` | 맵 저장 | CUSTOM-01 |
|  | PUT | `/custom-stages/{stageId}` | 맵 수정 | CUSTOM-04, 05 |
|  | PATCH | `/custom-stages/{stageId}/visibility` | 공개·비공개 전환 | CUSTOM-03 |
|  | DELETE | `/custom-stages/{stageId}` | 맵 삭제 | CUSTOM-09 |
| 도감 | GET | `/guidebooks` | 도감 목록 | GUIDE-02 |
|  | GET | `/guidebooks/{guidebookId}` | 도감 상세 | GUIDE-03 |
| 상점 | GET | `/items` | 판매 아이템 목록 | SHOP-01 |
|  | POST | `/items/{itemId}/purchase` | 아이템 구매 | SHOP-02~05 |
|  | GET | `/users/me/items` | 내 보유 아이템 | SHOP-06 |
| 우편 | GET | `/mails` | 우편 목록 | MAIL-01 |
|  | POST | `/mails/{mailId}/claim` | 보상 받기 | MAIL-02, 04 |
|  | POST | `/mails/claim-all` | 보상 모두 받기 | MAIL-03 |
| 친구 | GET | `/friends` | 친구 목록 | FRIEND-07 |
|  | DELETE | `/friends/{userId}` | 친구 삭제 | FRIEND-07 |
|  | POST | `/friend-requests` | 친구 요청 보내기 | FRIEND-01~03 |
|  | GET | `/friend-requests/received` | 받은 요청 목록 | FRIEND-04 |
|  | GET | `/friend-requests/sent` | 보낸 요청 목록 | FRIEND-04 |
|  | POST | `/friend-requests/{requestId}/accept` | 요청 수락 | FRIEND-05 |
|  | POST | `/friend-requests/{requestId}/reject` | 요청 거절 | FRIEND-05 |
|  | DELETE | `/friend-requests/{requestId}` | 보낸 요청 취소 | FRIEND-06 |
| 메시지 | GET | `/messages/rooms` | 대화 상대 목록 | MESSAGE-02 |
|  | GET | `/messages/{partnerId}` | 대화 내용 | MESSAGE-03 |
|  | POST | `/messages` | 메시지 보내기 | MESSAGE-01 |
|  | GET | `/messages/unread-count` | 안 읽은 메시지 수 | MESSAGE-04 |
| 신고 | GET | `/report-categories` | 신고 카테고리 | REPORT-01 |
|  | POST | `/reports` | 신고하기 | REPORT-02 |
|  | GET | `/reports/me` | 내 신고 목록 | REPORT-03 |
| 관리자 🛡️ | | `/admin/**` | 아래 9장 참고 | ADMIN-01~04, MAIL-05, REPORT-04 |

---

## 2. 인증

### POST `/auth/signup` 🔓

회원가입. `users`와 `profiles`를 함께 만든다.

**Request**
```json
{
  "email": "executor@example.com",
  "password": "password123!",
  "nickname": "debugger"
}
```

| 필드 | 규칙 |
| --- | --- |
| email | 이메일 형식, 최대 255자 |
| password | 8~64자 |
| nickname | 2~20자 |

**Response** `201 Created`
```json
{ "userId": 1, "email": "executor@example.com", "nickname": "debugger" }
```

**Error**: `400 INVALID_INPUT`, `409 DUPLICATE_EMAIL`, `409 DUPLICATE_NICKNAME`

---

### POST `/auth/login` 🔓

**Request**
```json
{ "email": "executor@example.com", "password": "password123!" }
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "accessTokenExpiresIn": 1800,
  "user": { "userId": 1, "nickname": "debugger", "role": "USER", "hasCharacterName": false }
}
```
- `hasCharacterName`이 `false`면 클라이언트는 튜토리얼의 이름 입력 단계로 보낸다.

**Error**: `401 INVALID_CREDENTIALS`

---

### POST `/auth/reissue` 🔓

Refresh Token으로 토큰을 다시 발급한다. Refresh Token도 새 것으로 바뀌고, 이전 Refresh Token은 더 이상 쓸 수 없다.

**Request**
```json
{ "refreshToken": "eyJhbGciOi..." }
```

**Response** `200 OK`
```json
{ "accessToken": "eyJhbGciOi...", "refreshToken": "eyJhbGciOi...", "accessTokenExpiresIn": 1800 }
```

**Error**: `401 INVALID_REFRESH_TOKEN`

---

### POST `/auth/logout`

Redis에 저장된 Refresh Token을 지운다.

**Response** `204 No Content`

---

### GET `/auth/check-email?email={email}` 🔓

**Response** `200 OK`
```json
{ "available": true }
```

---

### DELETE `/users/me`

회원 탈퇴. 탈퇴하면 프로필, 기록, 아이템, 친구, 메시지, 우편, 내가 만든 맵이 함께 삭제된다. 신고 기록은 남고 신고자와 대상은 NULL이 된다.

**Request**
```json
{ "password": "password123!" }
```

**Response** `204 No Content`

**Error**: `401 INVALID_CREDENTIALS`

---

## 3. 프로필

### GET `/profiles/check-nickname?nickname={nickname}` 🔓

**Response** `200 OK`
```json
{ "available": false }
```

---

### GET `/profiles/me`

**Response** `200 OK`
```json
{
  "userId": 1,
  "email": "executor@example.com",
  "nickname": "debugger",
  "characterName": "오집행",
  "comment": "버그 잡는 중",
  "coin": 350,
  "equippedSkinId": 3,
  "equippedThemeId": null,
  "role": "USER"
}
```
- `equipped*Id`가 `null`이면 기본 스킨·테마다.

---

### PATCH `/profiles/me`

보낸 필드만 수정한다.

**Request**
```json
{ "nickname": "debugger2", "characterName": "오집행", "comment": "버그 잡는 중" }
```

| 필드 | 규칙 |
| --- | --- |
| nickname | 2~20자, 중복 불가 |
| characterName | 1~20자, 중복 허용 |
| comment | 최대 255자. 빈 문자열을 보내면 지워진다 |

**Response** `200 OK`: `GET /profiles/me`와 같은 형식

**Error**: `400 INVALID_INPUT`, `409 DUPLICATE_NICKNAME`

---

### PUT `/profiles/me/equipment`

장착할 스킨과 테마를 정한다. 값을 `null`로 보내면 장착을 해제하고 기본값으로 돌아간다.

**Request**
```json
{ "skinItemId": 3, "themeItemId": null }
```

**Response** `200 OK`
```json
{ "equippedSkinId": 3, "equippedThemeId": null }
```

**Error**: `403 FORBIDDEN` (가지고 있지 않은 아이템), `400 INVALID_INPUT` (종류가 맞지 않음, 예: 스킨 자리에 테마)

---

### GET `/profiles/{userId}`

**Response** `200 OK`
```json
{
  "userId": 12,
  "nickname": "nullptr",
  "comment": "안녕하세요",
  "equippedSkinId": 5,
  "clearedStageCount": 7,
  "totalStars": 18,
  "isFriend": true
}
```

---

### GET `/profiles/search?nickname={keyword}&page=0&size=20`

닉네임에 검색어가 포함된 사용자를 찾는다. 나 자신은 결과에서 뺀다.

**Response** `200 OK`: `content`는 `UserSummary` 목록

---

## 4. 스테이지 / 플레이

### GET `/operations`

공식 작전과 스테이지 목록, 그리고 내 진행도를 한 번에 조회한다. `sort_order`, `stage_no` 순서로 정렬한다.

**Response** `200 OK`
```json
[
  {
    "operationId": 1,
    "name": "Tutorial",
    "description": "홀로그램 시뮬레이션",
    "sortOrder": 0,
    "stages": [
      {
        "stageId": 1,
        "stageNo": 1,
        "stageType": "TUTORIAL",
        "title": "시뮬레이션",
        "parMoves": 30,
        "rewardCoin": 100,
        "unlocked": true,
        "cleared": true,
        "stars": { "clear": true, "par": false, "hidden": false, "count": 1, "max": 1 },
        "bestMoves": 34,
        "playCount": 2
      }
    ]
  }
]
```
- 잠긴 스테이지는 `unlocked: false`이고, 기록 필드는 `false` 또는 `null`이다.
- 튜토리얼은 `max: 1`, 나머지 스테이지는 `max: 3`이다.

---

### GET `/stages/{stageId}`

스테이지 상세와 맵 데이터를 조회한다. 공식 스테이지와 사용자 맵 모두 이 API로 불러온다.

**Response** `200 OK`
```json
{
  "stageId": 2,
  "operationId": 2,
  "stageNo": 1,
  "stageType": "NORMAL",
  "title": "첫 발령",
  "creator": null,
  "parMoves": 20,
  "rewardCoin": 50,
  "hintCount": 1,
  "mapData": { "...": "map_data JSON에서 hints 필드를 뺀 나머지" },
  "myRecord": {
    "cleared": false,
    "stars": { "clear": false, "par": false, "hidden": false, "count": 0, "max": 3 },
    "bestMoves": null,
    "playCount": 0
  }
}
```
- `creator`: 공식 스테이지는 `null`, 사용자 맵은 `UserSummary`
- `hintCount`: 이 스테이지에서 받을 수 있는 힌트 개수

**Error**: `403 STAGE_LOCKED`, `403 FORBIDDEN` (남의 비공개 맵), `404 NOT_FOUND`

---

### POST `/stages/{stageId}/results`

한 판이 끝날 때마다(클리어, 게임 오버, 포기) 결과를 제출한다.

**Request**
```json
{
  "cleared": true,
  "moves": 18,
  "hiddenFound": true,
  "encounteredGuidebookIds": [3, 7]
}
```

| 필드 | 규칙 |
| --- | --- |
| cleared | 필수 |
| moves | 1~9999. `cleared`가 `true`일 때만 기록에 반영한다 |
| hiddenFound | 기본값 `false`. `cleared`가 `true`일 때만 반영한다 |
| encounteredGuidebookIds | 이번 판에서 만난 이상 개체의 도감 ID. 클리어하지 못해도 해금한다 |

**처리 순서** (하나의 트랜잭션)
1. `user_stages` 행이 없으면 만들고, `play_count`를 1 늘린다.
2. 클리어했다면 `best_moves = LEAST(기존 값, moves)`로 갱신하고 `is_hidden_found`를 누적한다.
3. 클리어했다면 `first_cleared_at IS NULL` 조건으로 UPDATE한다. 바뀐 행이 1개일 때만 `reward_coin`을 지급한다.
4. `encounteredGuidebookIds` 중 아직 없는 항목을 `user_guidebooks`에 추가한다.

**Response** `200 OK`
```json
{
  "cleared": true,
  "thisRun": { "clear": true, "par": true, "hidden": true },
  "stars": { "clear": true, "par": true, "hidden": true, "count": 3, "max": 3 },
  "bestMoves": 18,
  "isNewBest": true,
  "firstClear": true,
  "rewardCoin": 50,
  "totalCoin": 400,
  "unlockedGuidebooks": [
    { "guidebookId": 3, "category": "BUG", "title": "NullPointerException" }
  ],
  "nextStageId": 3
}
```
- `thisRun`은 이번 판의 결과, `stars`는 지금까지 누적된 결과다.
- 첫 클리어가 아니면 `firstClear: false`, `rewardCoin: 0`이다.
- `nextStageId`는 이번 클리어로 열린 다음 공식 스테이지다. 없으면 `null`이다.

**Error**: `400 INVALID_INPUT`, `403 STAGE_LOCKED`, `404 NOT_FOUND`

---

### GET `/stages/{stageId}/rankings?scope={all|friends}&size=20`

최소 턴 순위를 조회한다. 정렬은 `best_moves` 오름차순, 같으면 `first_cleared_at` 오름차순이다. `scope`의 기본값은 `all`이다. `friends`로 하면 나와 친구만 포함한다.

**Response** `200 OK`
```json
{
  "rankings": [
    { "rank": 1, "user": { "userId": 12, "nickname": "nullptr", "equippedSkinId": 5 }, "bestMoves": 15 },
    { "rank": 2, "user": { "userId": 1, "nickname": "debugger", "equippedSkinId": 3 }, "bestMoves": 18 }
  ],
  "me": { "rank": 2, "bestMoves": 18 }
}
```
- 내가 클리어하지 않았으면 `me`는 `null`이다.

---

### POST `/stages/{stageId}/hint`

힌트 아이템 1개를 써서 이 스테이지의 다음 힌트를 받는다.

**Request**
```json
{ "hintIndex": 0 }
```
- `hintIndex`: 0부터 `hintCount - 1`까지

**처리**
- 힌트 아이템 수량 확인과 1개 차감을 조건부 UPDATE 한 문장으로 처리한다. 힌트 아이템이 여러 종류면 아이템 ID가 작은 것부터 쓴다.
- 어떤 힌트를 봤는지는 저장하지 않는다. 같은 힌트를 다시 요청해도 아이템이 1개 빠지므로, 받은 힌트는 클라이언트가 기억해 둔다.
- `hintIndex` 범위 검사와 스테이지 접근 검사가 먼저다. 이 오류들로는 아이템이 빠지지 않는다.

**Response** `200 OK`
```json
{ "hintIndex": 0, "content": "열쇠는 오른쪽 벽 너머에 있다.", "remainingHintItems": 2 }
```
- `content`: `map_data.hints[hintIndex]` 그대로. 문자열이 아니라 객체로 저장했다면 객체로 나간다(키 순서는 보장하지 않음).
- `remainingHintItems`: 쓰고 남은 힌트 아이템 수(여러 종류면 합계)

**Error**: `400 INVALID_INPUT` (범위를 벗어난 hintIndex, 힌트가 없는 스테이지), `403 STAGE_LOCKED`, `403 FORBIDDEN` (남의 비공개 맵), `404 NOT_FOUND`, `409 NOT_ENOUGH_ITEM`

---

## 5. 사용자 제작 맵

플레이(상세 조회, 결과 제출, 순위)는 공식 스테이지와 같은 `/stages/{stageId}` API를 쓴다. 비공개 맵은 제작자만 조회·플레이할 수 있고(남은 `403`), 보상 코인은 항상 0이다.
수정·공개 전환·삭제는 스테이지 행을 잠그고 처리하므로, 수정(기록 삭제)과 공개가 동시에 와도 검증되지 않은 맵이 공개되지 않는다.

### GET `/custom-stages?page=0&size=20`

공개된 사용자 맵을 최신순(만든 시각)으로 조회한다(`size` 1~50).

**Response** `200 OK`: `content` 항목 형식
```json
{
  "stageId": 101,
  "title": "무한 루프 탈출",
  "creator": { "userId": 12, "nickname": "nullptr", "equippedSkinId": 5 },
  "parMoves": 25,
  "clearCount": 14,
  "myCleared": false,
  "createdAt": "2026-11-20T03:12:00Z"
}
```
- `clearCount`: 이 맵을 클리어한 사용자 수(제작자 포함), `myCleared`: 내가 클리어했는지

---

### GET `/custom-stages/me`

내가 만든 맵 목록. 비공개 맵도 포함하고 최근 수정 순으로 조회한다.

**Response** `200 OK`
```json
[
  { "stageId": 101, "title": "무한 루프 탈출", "parMoves": 25, "isPublic": true, "verified": true, "updatedAt": "2026-11-20T03:12:00Z" }
]
```
- `verified`: 제작자가 이 맵을 클리어했는지. `true`여야 공개할 수 있다.

---

### POST `/custom-stages`

**Request**
```json
{ "title": "무한 루프 탈출", "parMoves": 25, "mapData": { "...": "맵 JSON" } }
```

| 필드 | 규칙 |
| --- | --- |
| title | 1~100자 |
| parMoves | 1~9999 |
| mapData | 필수. 문자열이 아니라 JSON 객체 그대로 보낸다. 서버는 객체인지, `hints`가 있으면 배열인지, 크기(UTF-8 최대 256KB)만 검사한다 |

**Response** `201 Created`
```json
{ "stageId": 101, "isPublic": false, "verified": false }
```

---

### PUT `/custom-stages/{stageId}`

비공개 맵만 수정할 수 있다. 수정하면 이 맵의 기록이 모두 지워지므로 다시 클리어해야 공개할 수 있다.

**Request**: `POST /custom-stages`와 같은 형식

**Response** `200 OK`
```json
{ "stageId": 101, "isPublic": false, "verified": false }
```

**Error**: `400 INVALID_INPUT`, `403 FORBIDDEN` (내 맵이 아님), `404 NOT_FOUND` (없거나 공식 스테이지), `409 PUBLISHED_STAGE`

---

### PATCH `/custom-stages/{stageId}/visibility`

**Request**
```json
{ "isPublic": true }
```

비공개로는 언제든 바꿀 수 있다. 비공개로 바꿔도 기록은 남는다.

**Response** `200 OK`
```json
{ "stageId": 101, "isPublic": true, "verified": true }
```

**Error**: `403 FORBIDDEN`, `404 NOT_FOUND`, `409 NOT_VERIFIED` (클리어하지 않은 맵을 공개하려 함)

---

### DELETE `/custom-stages/{stageId}`

맵과 그 맵의 플레이 기록을 삭제한다. 신고 기록은 남고 대상 스테이지는 NULL이 된다.

**Response** `204 No Content`

**Error**: `403 FORBIDDEN`, `404 NOT_FOUND`

---

## 6. 도감

### GET `/guidebooks?category={BUG|VIRUS|TERM}`

`category`를 빼면 전체를 조회한다.

**Response** `200 OK`
```json
{
  "unlockedCount": 5,
  "totalCount": 20,
  "items": [
    { "guidebookId": 3, "category": "BUG", "unlocked": true, "title": "NullPointerException", "image": "/guide/npe.png", "unlockedAt": "2026-11-20T03:12:00Z" },
    { "guidebookId": 4, "category": "BUG", "unlocked": false, "title": null, "image": null, "unlockedAt": null }
  ]
}
```

---

### GET `/guidebooks/{guidebookId}`

**Response** `200 OK`
```json
{
  "guidebookId": 3,
  "category": "BUG",
  "title": "NullPointerException",
  "image": "/guide/npe.png",
  "comment": "존재하지 않는 객체를 참조하려 할 때 발생하는 오류...",
  "stageId": 2,
  "unlockedAt": "2026-11-20T03:12:00Z"
}
```

**Error**: `403 FORBIDDEN` (아직 해금하지 않은 항목)

---

## 7. 상점 / 우편

### GET `/items?type={SKIN|THEME|HINT}`

판매 중(`is_active = true`)인 아이템만 아이템 ID 순으로 조회한다. `type`을 빼면 전체(대소문자 무시). 보유하지 않은 아이템은 `owned: false`, `quantity: 0`.

**Response** `200 OK`
```json
[
  { "itemId": 3, "name": "다크 모드 집행자", "image": "/items/skin_dark.png", "description": "어둠 속의 집행자", "itemType": "SKIN", "price": 300, "owned": true, "quantity": 1 },
  { "itemId": 9, "name": "힌트 로그", "image": "/items/hint.png", "description": "스테이지 힌트 1회", "itemType": "HINT", "price": 30, "owned": true, "quantity": 2 }
]
```

---

### POST `/items/{itemId}/purchase`

**Request**
```json
{ "quantity": 3 }
```
- 스킨과 테마는 `quantity`를 무시하고 1개로 처리한다. 힌트는 1~99개.
- 본문이나 `quantity`를 빼면 1개.
- 이미 가진 스킨·테마는 코인이 모자라도 `ALREADY_OWNED`가 먼저다.

**처리**: 코인 차감(`coin >= 가격` 조건부 UPDATE)과 아이템 지급을 하나의 트랜잭션으로 처리한다.

**Response** `200 OK`
```json
{ "itemId": 9, "quantity": 5, "spentCoin": 90, "remainingCoin": 310 }
```
- `quantity`: 구매한 뒤의 보유 수량

**Error**: `400 INVALID_INPUT` (힌트 수량이 1~99 밖), `404 NOT_FOUND` (없거나 판매가 중단된 아이템), `409 NOT_ENOUGH_COIN`, `409 ALREADY_OWNED`

---

### GET `/users/me/items?type={SKIN|THEME|HINT}`

아이템 ID 순. 다 쓴 힌트(수량 0)는 빠진다. `equipped`는 지금 장착 중인 스킨·테마인지 나타낸다.

**Response** `200 OK`
```json
[
  { "itemId": 3, "name": "다크 모드 집행자", "itemType": "SKIN", "image": "/items/skin_dark.png", "quantity": 1, "equipped": true, "acquiredAt": "2026-11-20T03:12:00Z" }
]
```

---

### GET `/mails?page=0&size=20`

만료되지 않은 우편을 최신순으로 조회한다(`size` 1~50, 기본 20). 받은 우편도 `claimed: true`로 남아 있다.

**Response** `200 OK`: `content` 항목 형식
```json
{
  "mailId": 15,
  "title": "발표회 방문 감사 보상",
  "content": "찾아와 주셔서 감사합니다!",
  "rewardCoin": 100,
  "rewardItem": { "itemId": 3, "name": "다크 모드 집행자", "itemType": "SKIN" },
  "rewardItemQuantity": 1,
  "claimed": false,
  "expiresAt": "2026-12-31T14:59:59Z",
  "createdAt": "2026-11-24T01:00:00Z"
}
```
- 첨부 아이템이 없으면 `rewardItem`은 `null`이다.

---

### POST `/mails/{mailId}/claim`

**처리**: `claimed_at IS NULL`이고 만료되지 않았다는 조건으로 UPDATE한다. 바뀐 행이 1개일 때만 보상을 지급한다. 이미 가진 스킨이나 테마가 첨부되어 있으면 그 아이템의 **현재 가격**만큼 코인으로 바꿔서 준다.
- 잠금 순서는 우편 → 프로필(코인) → 보유 아이템. 상점 구매와 같은 방향이라 같은 사용자의 구매·수령이 동시에 와도 교착 상태가 없다.

**Response** `200 OK`
```json
{
  "mailId": 15,
  "rewardCoin": 100,
  "convertedCoin": 300,
  "rewardItem": null,
  "totalCoin": 750
}
```
- `convertedCoin`: 이미 가진 아이템을 코인으로 바꾼 금액. 바꾼 게 없으면 0
- `rewardItem`: 실제로 지급된 아이템 `{ "itemId", "itemType", "quantity" }`. 첨부가 없거나 코인으로 바꿨다면 `null`
- `totalCoin`: 받은 뒤 내 코인

**Error**: `403 FORBIDDEN` (내 우편이 아님), `404 NOT_FOUND`, `409 ALREADY_CLAIMED`, `409 MAIL_EXPIRED`

---

### POST `/mails/claim-all`

받을 수 있는 우편을 오래된 순으로 모두 받는다. 받을 게 없으면 `claimedCount: 0`. 같은 스킨이 여러 우편에 있으면 첫 우편에서 아이템을 받고 나머지는 코인으로 바뀐다. `rewardItems`는 같은 아이템의 수량을 합친 값(아이템 ID 순).

**Response** `200 OK`
```json
{ "claimedCount": 3, "rewardCoin": 250, "convertedCoin": 0, "rewardItems": [ { "itemId": 9, "quantity": 2 } ], "totalCoin": 1000 }
```

---

## 8. 친구 / 메시지 / 신고

### GET `/friends`

닉네임 순으로 조회한다. `since`는 친구가 된 시각.

**Response** `200 OK`
```json
[
  { "user": { "userId": 12, "nickname": "nullptr", "equippedSkinId": 5 }, "since": "2026-11-20T03:12:00Z" }
]
```

---

### DELETE `/friends/{userId}`

친구 관계와 함께 두 사람 사이의 친구 요청 기록도 지운다. 그래서 나중에 다시 처음부터 요청할 수 있다.

**Response** `204 No Content`

**Error**: `404 NOT_FOUND` (친구가 아님)

---

### POST `/friend-requests`

**Request**
```json
{ "receiverId": 12 }
```

**처리**
- 상대가 나에게 보낸 대기 중 요청이 있으면 그 요청을 수락 처리하고 바로 친구가 된다.
- 내가 보냈다가 거절된 요청이 있으면 그 요청을 `PENDING`으로 되돌린다(같은 `requestId`, `createdAt`은 다시 보낸 시각).
- 같은 두 사람 사이의 요청·수락·거절·취소·삭제는 두 사람의 프로필 행을 ID 순서대로 잠근 뒤 처리한다. A→B, B→A 요청이 동시에 와도 바로 친구가 된다.

**Response** `201 Created`
```json
{ "requestId": 30, "status": "PENDING" }
```
- 바로 친구가 된 경우에는 `"status": "ACCEPTED"`

**Error**: `400 INVALID_INPUT` (자기 자신), `404 NOT_FOUND`, `409 ALREADY_FRIEND`, `409 REQUEST_ALREADY_EXISTS`

---

### GET `/friend-requests/received` · GET `/friend-requests/sent`

대기 중(`PENDING`)인 요청만 최근 순으로 조회한다.

**Response** `200 OK`
```json
[
  { "requestId": 30, "user": { "userId": 12, "nickname": "nullptr", "equippedSkinId": 5 }, "createdAt": "2026-11-20T03:12:00Z" }
]
```
- `received`에서 `user`는 보낸 사람, `sent`에서 `user`는 받는 사람이다.

---

### POST `/friend-requests/{requestId}/accept` · POST `/friend-requests/{requestId}/reject`

나에게 온 대기 중 요청만 처리할 수 있다. 수락하면 같은 트랜잭션에서 `friends`에 행을 추가한다.

**Response** `200 OK`
```json
{ "requestId": 30, "status": "ACCEPTED" }
```

**Error**: `403 FORBIDDEN` (나에게 온 요청이 아님), `404 NOT_FOUND`, `409 REQUEST_ALREADY_HANDLED` (이미 처리됐거나 취소된 요청)

---

### DELETE `/friend-requests/{requestId}`

내가 보낸 대기 중 요청을 취소한다. 요청 행을 삭제한다.

**Response** `204 No Content`

**Error**: `403 FORBIDDEN` (내가 보낸 요청이 아님), `404 NOT_FOUND`, `409 REQUEST_ALREADY_HANDLED` (이미 수락·거절된 요청)

---

### GET `/messages/rooms`

마지막 메시지가 최근인 순으로 조회한다. 친구를 끊은 상대와의 대화도 남아 있다.

**Response** `200 OK`
```json
[
  {
    "partner": { "userId": 12, "nickname": "nullptr", "equippedSkinId": 5 },
    "lastMessage": "보스 스테이지 몇 턴에 깼어?",
    "lastMessageAt": "2026-11-20T03:12:00Z",
    "unreadCount": 2
  }
]
```

---

### GET `/messages/{partnerId}?page=0&size=30`

최신순으로 조회한다(`sort` 파라미터는 무시). `size`는 1~50, 기본 30.
조회하면 상대가 나에게 보낸 메시지를 **먼저** 읽음 처리하므로 응답의 `isRead`에도 바로 반영된다. 친구를 끊은 뒤에도 기존 대화는 볼 수 있다.

**Response** `200 OK`: `content` 항목 형식
```json
{ "messageId": 88, "senderId": 12, "content": "보스 스테이지 몇 턴에 깼어?", "isRead": true, "createdAt": "2026-11-20T03:12:00Z" }
```
- `isRead`: 받는 사람이 읽었는지. 내가 보낸 메시지면 상대가 읽었는지를 뜻한다.

---

### POST `/messages`

**Request**
```json
{ "receiverId": 12, "content": "18턴!" }
```
- `content`: 1~500자

**Response** `201 Created`
```json
{ "messageId": 89, "createdAt": "2026-11-20T03:13:00Z" }
```

**Error**: `400 INVALID_INPUT` (자기 자신, 빈 내용, 500자 초과), `409 NOT_FRIEND` (친구가 아님. 없는 사용자도 같은 오류로 응답해 존재 여부를 드러내지 않는다)

---

### GET `/messages/unread-count`

**Response** `200 OK`
```json
{ "count": 3 }
```

---

### GET `/report-categories`

**Response** `200 OK`
```json
[
  { "categoryId": 1, "name": "부적절한 닉네임", "targetRequired": true },
  { "categoryId": 2, "name": "불량 맵", "targetRequired": true },
  { "categoryId": 3, "name": "버그 제보", "targetRequired": false },
  { "categoryId": 4, "name": "기타", "targetRequired": false }
]
```
- `targetRequired`: 신고 대상(사용자 또는 스테이지)을 꼭 지정해야 하는지. `report_categories`에 이를 나타내는 컬럼이 없어서 서버가 카테고리 **이름**(`버그 제보`, `기타`)으로 판단한다. 카테고리 이름을 바꾸면 서버 코드도 함께 바꿔야 한다.

---

### POST `/reports`

**Request**
```json
{
  "categoryId": 2,
  "targetUserId": null,
  "targetStageId": 101,
  "title": "클리어 불가능한 맵",
  "comment": "문이 벽으로 막혀 있습니다."
}
```
- 버그 제보와 기타를 빼면 `targetUserId`와 `targetStageId` 중 하나는 꼭 있어야 한다. **둘 다 지정할 수는 없다.**
- 자기 자신은 신고할 수 없다. 남의 비공개 맵은 볼 수 없는 맵이므로 없는 스테이지(`404`)로 처리한다.
- `title`은 1~255자, `comment`는 1~2000자.

**Response** `201 Created`
```json
{ "reportId": 7, "status": "RECEIVED", "handledAt": null }
```

**Error**: `400 INVALID_INPUT` (대상 규칙, 자기 자신, 길이), `404 NOT_FOUND` (카테고리·대상 사용자·대상 스테이지)

---

### GET `/reports/me?page=0&size=20`

최신순으로 조회한다(`size` 1~50).

**Response** `200 OK`: `content` 항목 형식
```json
{ "reportId": 7, "category": "불량 맵", "title": "클리어 불가능한 맵", "status": "IN_PROGRESS", "createdAt": "2026-11-20T03:12:00Z", "handledAt": null }
```

---

## 9. 관리자 API 🛡️

모든 관리자 API는 `ADMIN` 권한이 필요하다. 요청 본문은 DB 컬럼을 camelCase로 바꾼 형식을 따른다.

- 등록은 `201 Created`와 등록된 내용을, 수정은 `200 OK`와 수정된 내용을, 삭제는 `204 No Content`를 돌려준다.
- 수정(PUT)은 모든 값을 보낸다. 문자열은 앞뒤 공백을 지우고, 선택 항목(설명, 이미지)이 빈 문자열이면 `null`로 저장한다.
- 겹칠 수 없는 값이 겹치면 `409 DUPLICATE_VALUE`.

| Method | URL | 설명 | 요청 본문 / 비고 |
| --- | --- | --- | --- |
| POST | `/admin/operations` | 작전 등록 | `name`, `description`, `sortOrder` |
| PUT | `/admin/operations/{id}` | 작전 수정 | 등록과 같음 |
| DELETE | `/admin/operations/{id}` | 작전 삭제 | 스테이지가 있으면 `409 OPERATION_HAS_STAGES` |
| GET | `/admin/stages/{id}` | 공식 스테이지 상세 | 사용자용과 달리 `mapData`에 `hints`가 그대로 들어 있다 |
| POST | `/admin/stages` | 공식 스테이지 등록 | `operationId`, `stageNo`, `stageType`, `title`, `mapData`, `parMoves`, `rewardCoin` (`isPublic`은 항상 true). `mapData`는 문자열이 아니라 JSON 객체 그대로 보내고, `hints`가 있으면 배열이어야 한다 |
| PUT | `/admin/stages/{id}` | 공식 스테이지 수정 | 등록과 같음. 기존 기록은 유지한다 |
| DELETE | `/admin/stages/{id}` | 공식 스테이지 삭제 | 기록도 함께 삭제되고, 도감의 `stageId`는 NULL이 된다 |
| POST | `/admin/guidebooks` | 도감 등록 | `stageId`, `category`, `title`, `image`, `comment` |
| PUT | `/admin/guidebooks/{id}` | 도감 수정 | 등록과 같음 |
| DELETE | `/admin/guidebooks/{id}` | 도감 삭제 | 사용자의 해금 기록도 함께 삭제 |
| GET | `/admin/items` | 아이템 전체 목록 | 판매 중단된 아이템까지 모두 |
| POST | `/admin/items` | 아이템 등록 | `name`, `image`, `description`, `itemType`, `price`. 항상 판매 중으로 등록 |
| PUT | `/admin/items/{id}` | 아이템 수정 | 등록 항목 + `isActive`(필수). 가진 사용자가 있는 아이템은 `itemType`을 바꿀 수 없다(400). 삭제 API는 두지 않는다 |
| POST | `/admin/mails` | 우편 발송 | 아래 참고 |
| GET | `/admin/reports?status=RECEIVED&page=0` | 신고 목록 | 최신순. `status`를 빼면 전체(대소문자 무시). 항목: `reportId`, `category {categoryId, name}`, `reporter`·`targetUser`(UserSummary, 탈퇴했으면 null), `targetStage {stageId, title, official}`(삭제됐으면 null), `title`, `comment`, `status`, `createdAt`, `handledAt` |
| PATCH | `/admin/reports/{id}` | 신고 상태 변경 | `{ "status": "DONE" }`. 앞으로만 진행(`RECEIVED` → `IN_PROGRESS` → `DONE`, `RECEIVED` → `DONE` 가능), 되돌리면 `400`. 같은 상태면 그대로. `DONE`이면 `handledAt`을 기록. 응답 `{ reportId, status, handledAt }` |

### POST `/admin/mails`

**Request**
```json
{
  "receiverIds": [1, 12],
  "toAll": false,
  "title": "발표회 방문 감사 보상",
  "content": "찾아와 주셔서 감사합니다!",
  "rewardCoin": 100,
  "rewardItemId": 3,
  "rewardItemQuantity": 1,
  "expiresAt": "2026-12-31T14:59:59Z"
}
```
- `toAll`이 `true`면 `receiverIds`를 무시하고 모든 사용자에게 한 통씩 보낸다. 아니면 `receiverIds`(최대 1000명, 중복은 한 번만)가 필요하고, 없는 사용자가 있으면 `404`(메시지에 그 ID 목록)로 아무에게도 보내지 않는다.
- `rewardItemId`가 `null`이면 `rewardItemQuantity`는 0이어야 한다. 아이템이 있으면 스킨·테마는 1개, 힌트는 1~99개.
- `rewardCoin`(0~1,000,000)과 `rewardItemQuantity`는 생략하면 0. `title`은 1~100자, `content`는 2000자 이하.
- `expiresAt`은 `null`(무기한)이거나 지금보다 뒤여야 한다.
- 받는 사람 수와 상관없이 `INSERT ... SELECT` 한 문장으로 넣는다.

**Response** `201 Created`
```json
{ "sentCount": 2 }
```
