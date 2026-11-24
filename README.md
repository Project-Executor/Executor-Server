# Executor(집행자) Server

Unity 퍼즐 게임 **Executor(집행자)** 의 백엔드 API 서버.
회원·프로필(사진 포함), 스테이지 플레이와 순위, 게스트로 플레이용 공개 조회, 도감, 인물, 상점·메일, 친구·메시지, 사용자 제작 맵, 신고, 공지, 관리자 기능을 제공한다.

| 문서 | 내용 |
| --- | --- |
| [docs/api-spec.md](docs/api-spec.md) | API 명세 (요청·응답·오류 코드) |
| [docs/map-data.md](docs/map-data.md) | 맵 JSON 구조와 게임 규칙 (Unity·관리자 맵 에디터가 함께 따르는 기준) |
| [docs/requirements.md](docs/requirements.md) | 요구사항 정의서 |
| Swagger UI | 배포 서버 `/swagger-ui/index.html` |

| 저장소 | 역할 |
| --- | --- |
| **Executor-Server** (여기) | API 서버. `main`에 머지하면 Railway에 자동 배포 |
| [Executor-Admin](https://github.com/Project-Executor/Executor-Admin) | 관리자 웹 (React). `main`에 머지하면 Railway에 배포 (`https://executor-admin-production.up.railway.app`) |
| [Executor-Game](https://github.com/Project-Executor/Executor-Game) | Unity 6 PC 2D 클라이언트 |

- 배포 주소: `https://executor-server-production-032d.up.railway.app`

## 기술 스택

| 분류 | 사용 |
| --- | --- |
| 언어·프레임워크 | Java 17 바이트코드(실행 JDK 21), Spring Boot 4.1, Spring Security 7, Spring Data JPA (Hibernate 7) |
| DB | MySQL 8 (Railway), 구조는 `schema.sql` 기준 + `ddl-auto=validate` |
| 캐시·토큰 | Redis (Railway), Refresh Token 저장 (`refresh:{userId}`, TTL 14일) |
| 인증 | JWT (Access 30분 / Refresh 14일), BCrypt |
| 문서 | springdoc-openapi (Swagger UI) |
| 배포 | Railway + Dockerfile (멀티 스테이지 빌드) |

## 저장소 구조

```
Executor-Server/                  ← 저장소 루트 = Railway Root Directory (/)
├── Dockerfile
├── build.gradle
├── .env.example                  ← 로컬 실행용 환경 변수 양식 (.env는 커밋 금지)
├── .env.test.example             ← 테스트 DB 계정 양식 (.env.test는 커밋 금지)
├── docs/
│   ├── api-spec.md               ← API 명세 (요청·응답·오류 코드)
│   ├── requirements.md           ← 요구사항 정의서
│   ├── map-data.md               ← 스테이지 map_data JSON 구조와 이동 규칙
│   └── maps/                     ← 공식 스테이지 맵 JSON과 생성·검증 스크립트
└── src/main/
    ├── java/com/seyoung5309/executorserver/
    │   ├── domain/               ← 기능별 패키지 (controller / service / repository / entity / dto)
    │   │   ├── auth              회원가입·로그인·토큰 재발급·탈퇴·계정 정지
    │   │   ├── profile           프로필·닉네임·장착·검색·프로필 사진(user_avatars)
    │   │   ├── stage             레이어·작전·스테이지·결과 제출(별 판정)·순위·사용자 맵·게스트 조회(/api/guest)
    │   │   ├── guidebook         도감
    │   │   ├── character         인물 (대화창 이름표 색·일러스트 폴더·주인공, 로그인 없이 조회)
    │   │   ├── item              상점·보유 아이템 (스킨·테마)
    │   │   ├── mail              메일 (보상 수령)
    │   │   ├── friend            친구 요청·목록
    │   │   ├── message           친구 간 메시지
    │   │   ├── report            신고
    │   │   ├── notice            공지사항 (로그인 없이 조회)
    │   │   ├── wordlist          감염된 컴퓨터 타자 해킹 단어 (로그인 없이 조회)
    │   │   └── admin             관리자 (유저·레이어·작전·스테이지·스테이지 배치·도감·인물·아이템·메일·신고·공지·단어 목록)
    │   └── global/               ← 공통 (config, exception, health(/api/health), jpa, response, security)
    └── resources/
        ├── application.properties
        └── db/
            ├── schema.sql        ← 최신 전체 구조 + 기초 데이터 (빈 DB에 한 번 실행)
            ├── reset-dev.sql     ← 개발·테스트 DB 전체 삭제 (DB 이름에 test/dev가 있어야 동작)
            └── migrations/       ← 운영 중인 DB에 적용하는 구조 변경
```

## 로컬 실행

1. `.env.example`을 `.env`로 복사하고 값을 채운다.
   - MySQL·Redis는 Railway 서비스의 **공개 주소**(`xxx.proxy.rlwy.net`)와 포트를 쓴다. 내부 주소는 배포된 서버에서만 통한다.
   - `JWT_SECRET`은 Base64로 인코딩한 256bit 이상의 키.
2. 서버 실행

   ```bash
   ./gradlew bootRun            # 포트를 바꾸려면 PORT=8090 ./gradlew bootRun
   ```
3. `http://localhost:8080/swagger-ui/index.html` 에서 확인한다.

> 로컬 서버도 Railway의 운영 DB를 쓴다. 확인용 데이터는 `zz-` 로 시작하는 이름(이메일은 `zz-...@example.com`)으로 만들고, 확인이 끝나면 지운다.

## 테스트

테스트는 운영 DB가 아니라 **로컬 MySQL의 `executor_test` DB**를 쓴다 (`src/test/resources/application-test.properties`).

1. 로컬 MySQL에 테스트 DB와 계정을 만든다.

   ```sql
   CREATE DATABASE executor_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER 'executor_test'@'localhost' IDENTIFIED BY '비밀번호';
   GRANT ALL PRIVILEGES ON executor_test.* TO 'executor_test'@'localhost';
   ```
2. `.env.test.example`을 `.env.test`로 복사하고 비밀번호를 채운다. **Railway 접속 정보는 넣지 않는다.**
3. 스키마를 넣는다.

   ```bash
   mysql -u executor_test -p executor_test < src/main/resources/db/schema.sql
   ```
4. 테스트 실행

   ```bash
   ./gradlew test
   ```

테스트 DB를 처음부터 다시 만들 때는 `reset-dev.sql`을 실행한 뒤 `schema.sql`을 다시 실행한다.
Redis는 테스트 중 실제로 접속하지 않으므로 로컬에 없어도 된다.

## DB 스크립트 규칙

| 파일 | 용도 | 주의 |
| --- | --- | --- |
| `schema.sql` | 최신 전체 구조와 기초 데이터 | 빈 DB에만 실행한다. 테이블이 있으면 첫 `CREATE TABLE`에서 멈추므로 기존 데이터는 지워지지 않는다. |
| `reset-dev.sql` | 개발·테스트 DB 전체 삭제 | 첫 문장의 안전장치가 DB 이름에 `test`/`dev`가 없으면 오류로 멈춘다. `--force`로 실행하지 않는다. |
| `migrations/YYYY-MM-DD_설명.sql` | 운영 DB 구조 변경 | `schema.sql`을 고칠 때 같은 변경을 이 파일로도 만들고, **main에 머지하기 전에** 운영 DB에 실행한다. |

구조가 바뀐 서버를 테이블 변경 없이 배포하면 `ddl-auto=validate`에서 기동이 실패한다.

적용한 마이그레이션

| 파일 | 내용 | 운영 DB 적용 |
| --- | --- | --- |
| `2026-11-12_user_stage_hints.sql` | 힌트 열람 기록 테이블 (2026-11-20에 힌트 기능과 함께 제거) | 2026-11-12 |
| `2026-11-15_rename_accounts_users.sql` | 테이블 이름 변경 `users` → `accounts`, `profiles` → `users` (제약 조건 이름 포함) | 2026-11-15 |
| `2026-11-15_nickname_only.sql` | `character_name` 삭제, `nickname` NULL 허용 (가입 후 튜토리얼에서 입력) | 적용됨 (2026-11-20 확인) |
| `2026-11-17_account_suspension.sql` | 계정 정지 | 2026-11-17 |
| `2026-11-17_notices.sql` | 공지사항 | 2026-11-17 |
| `2026-11-18_user_stage_stable_cleared.sql` | 별 2(불안정 체크포인트 허용 수 이하) 기록 | 2026-11-18 |
| `2026-11-19_layers.sql` | 레이어(대주제) 추가, 작전을 레이어 안에 | 2026-11-19 |
| `2026-11-19_word_lists.sql` | 타자 해킹 단어 목록 | 2026-11-19 |
| `2026-11-20_remove_hints.sql` | 힌트 기능 제거 (`user_stage_hints` 삭제, 아이템 종류에서 `HINT` 제거) | 적용됨 (2026-11-24 확인) |
| `2026-11-20_mail_comments.sql` | "우편" → "메일" (mails 테이블·열 주석만) | 적용됨 (2026-11-24 확인) |
| `2026-11-21_characters.sql` | 인물 목록 `characters` + 기본 인물 5명 | 적용됨 (2026-11-24 확인) |
| `2026-11-21_cursor_delta_items.sql` | 아이템 종류 `CURSOR`(커서), `DELTA`(델타 꾸미기)와 장착 칸 | 적용됨 (2026-11-24 확인) |
| `2026-11-21_guidebook_details.sql` | 도감 상세 항목 (언어, 코드 예시, 대처, 맵에서) | 적용됨 (2026-11-24 확인) |
| `2026-11-24_user_avatars.sql` | 프로필 사진 `user_avatars` (MEDIUMBLOB, 512KB 이하) | 적용됨 (2026-11-24 확인) |
| `2026-11-24_stage_board_position.sql` | 스테이지 선택 화면 노드 위치 `stages.board_x`, `board_y` | 적용됨 (2026-11-24 확인) |

## 배포 (Railway)

- `main`에 머지하면 Railway가 자동으로 빌드·배포한다. `develop`은 배포되지 않는다.
- 빌드는 루트의 `Dockerfile` (JDK 21로 `bootJar` 빌드 → JRE 21 이미지에서 `executor-server.jar` 실행).
- Railway 서비스 설정
  - Root Directory: 비워 둔다 (저장소 루트)
  - Variables: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`, `REDISHOST`, `REDISPORT`, `REDISPASSWORD`는 `${{MySQL.MYSQLHOST}}`처럼 각 서비스 값을 참조하고, `JWT_SECRET`은 직접 넣는다.
  - `CORS_ALLOWED_ORIGINS`(선택): 브라우저에서 API를 부를 수 있는 다른 주소, 쉼표로 여러 개. 비우면 기본값(관리자 웹 배포 주소, `http://localhost:5173`, `http://localhost:4173`). 관리자 웹 주소가 바뀌면 여기에 넣는다. 게임(Unity)은 브라우저가 아니라 상관없다.
  - 서버 포트는 Railway가 넣는 `PORT`를 따른다.
- 배포 전 확인: 새 마이그레이션이 있으면 운영 DB에 먼저 실행했는지.

## 작업 흐름

- 브랜치: `main`(배포) ← `develop`(작업)
- 기능 하나를 마치면 `develop`에 바로 커밋한다. 배포할 때만 `develop` → `main` PR을 연다 (초기에는 `feature/*` 브랜치와 PR을 썼다).
- 커밋·PR 제목은 `feat:`, `fix:`, `chore:`, `docs:` 접두어와 짧은 명사형 (예: `feat: 타자 해킹 단어 목록 추가`).
- **DB 구조가 바뀌면** `schema.sql`과 `migrations/`를 같이 고치고, 배포 PR을 머지하기 **전에** 운영 DB에 마이그레이션을 실행한다.

## 구현 메모

- **중복 지급·음수 방지**: 보상 지급, 코인 차감, 아이템 차감은 조건부 UPDATE 한 문장으로 처리하고, 영향받은 행 수로 성공 여부를 판단한다. 이미 있으면 건너뛰는 기록은 `INSERT IGNORE`, 수량 누적은 `ON DUPLICATE KEY UPDATE`.
- **동시 요청 순서 처리**: 같은 대상에 대한 요청은 행 잠금(`SELECT ... FOR UPDATE`)으로 하나씩 처리한다. 두 사용자가 얽힌 친구 요청은 두 프로필을 ID 오름차순으로 잠근다. 잠금 순서는 `mails → users → user_items`로 맞춘다.
- **시각**: 모든 시각은 UTC 초 단위다. 서버 시계(`Clock.tick` 1초)와 생성·수정 시각(`@CreatedAtSeconds`, `@UpdatedAtSeconds`)을 DB의 `DATETIME`(초 단위)과 맞춰, 저장 직후 응답한 값과 나중에 조회한 값이 같다.
- **목록 API**: 정렬은 서버가 정하고, 페이지 크기는 1~50으로 제한한다.
- **사용자 맵**: `map_data`는 JSON 객체(최대 256KB)만 받는다. 제작자가 클리어해야 공개할 수 있고, 공개된 맵은 수정할 수 없다. 비공개 맵을 수정하면 플레이 기록이 지워진다.
- **map_data는 해석하지 않는다**: 맵 그림 세트(`theme`), 칸별 그림(`tileArt`), 이야기·대화 등은 게임과 관리자 웹만 읽는다. 형식은 [docs/map-data.md](docs/map-data.md).
- **게스트로 플레이**: `GET /api/guest/operations`, `/api/guest/stages/{id}`는 로그인 없이 공식 스테이지만 조회한다. 기록 없이 모두 열린 상태로 보내고 잠금은 게임이 PC 진행으로 정한다. 결과 제출·순위·코인 등 쓰기 API는 그대로 로그인이 필요하다.
- **프로필 사진**: 외부 저장소 없이 DB(`user_avatars`, MEDIUMBLOB)에 둔다. PNG·JPEG만, 512KB 이하(게임이 256×256으로 잘라 보낸다). `GET /api/profiles/{userId}/avatar`는 그림 바이트를 돌려주고(하루 캐시), 내 프로필의 `avatarVersion`(수정 시각)이 바뀌면 게임이 다시 받는다.
- **스테이지 선택 화면 배치**: `stages.board_x`, `board_y`는 0~1 비율 좌표(노드 가운데). 비어 있으면 게임이 지그재그로 자동 배치한다. 관리자 웹 작전 화면에서 끌어 놓아 `PUT /api/admin/operations/{id}/board`로 저장한다.
- **인증 흐름**: Access Token(30분)은 요청마다 `JwtAuthenticationFilter`가 검사하고, 만료되면 `401 TOKEN_EXPIRED`를 돌려준다. 클라이언트는 Refresh Token(14일, Redis `refresh:{userId}`에 하나만 저장)으로 재발급받고, 재발급할 때마다 Refresh Token도 바뀐다(회전).

## 주요 변경 이력

자세한 내용은 `git log`와 각 배포 PR 본문을 본다.

| 시기 | 내용 |
| --- | --- |
| 11/12 이전 | 상점, 관리자(작전·스테이지·도감·아이템), 친구, 메시지, 메일, 신고, 사용자 맵, `schema.sql` 안전장치, 테스트 DB 분리 |
| 11/15 | 테이블 이름 정리 (`accounts`·`users`), 닉네임은 가입 후 튜토리얼에서 |
| 11/17 | 관리자 유저 관리 (검색·코인·권한·정지), 공지사항 |
| 11/18 | 체크포인트 기준 ★2 (`stars.maxUnstable`, `is_stable_cleared`) |
| 11/19 | 레이어(스토리 대주제), 타자 해킹 단어 목록 |
| 11/20 | 힌트 기능 제거, ★3 조건을 맵마다 선택 (`stars.bonus`, 서버 변경 없음) |
| 11/21 | 서버 연결 확인 `/api/health`, 가입 비밀번호 영문·숫자 규칙, 인물 목록, 아이템 종류 CURSOR·DELTA, 도감 상세 항목, map-data 문서(나레이션 배경, 대화 단계, 맵 그림 세트) |
| 11/24 | 순위 응답에 클리어 인원(`total`)·첫 클리어 시각(`clearedAt`), 프로필 사진, 게스트로 플레이 공개 조회 API, 스테이지 선택 화면 배치, map-data 문서(코퍼레이션·튜토리얼 세트, 워프 후 열쇠 유지, 칸별 그림 `tileArt`) |

## 알려진 문제

- 일부 네트워크에서 `*.up.railway.app` 주소가 차단된다 (DNS가 127.0.0.1을 돌려주거나 TLS 연결이 끊김). 이 경우 VPN(전체 트래픽, UDP)을 켜야 배포 서버에 접속된다. 운영 전에 커스텀 도메인 연결을 검토한다.
