# Executor(집행자) Server

Unity 퍼즐 게임 **Executor(집행자)** 의 백엔드 API 서버.
회원·프로필, 스테이지 플레이와 순위, 도감, 상점·우편, 친구·메시지, 사용자 제작 맵, 신고, 관리자 기능을 제공한다.

- 배포 주소: `https://executor-server-production-032d.up.railway.app`
- API 문서(Swagger UI): `/swagger-ui/index.html`
- 상세 명세: [docs/api-spec.md](docs/api-spec.md), 요구사항: [docs/requirements.md](docs/requirements.md)

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
│   └── requirements.md           ← 요구사항 정의서
└── src/main/
    ├── java/com/seyoung5309/executorserver/
    │   ├── domain/               ← 기능별 패키지 (controller / service / repository / entity / dto)
    │   │   ├── auth              회원가입·로그인·토큰 재발급·탈퇴
    │   │   ├── profile           프로필·닉네임·장착·검색
    │   │   ├── stage             작전·스테이지·결과 제출·순위·힌트·사용자 맵
    │   │   ├── guidebook         도감
    │   │   ├── item              상점·보유 아이템
    │   │   ├── mail              우편 (보상 수령)
    │   │   ├── friend            친구 요청·목록
    │   │   ├── message           친구 간 메시지
    │   │   ├── report            신고
    │   │   └── admin             관리자 (작전·스테이지·도감·아이템·우편·신고 관리)
    │   └── global/               ← 공통 (config, exception, jpa, response, security)
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
| `2026-11-12_user_stage_hints.sql` | 힌트 열람 기록 테이블 (한 번 본 힌트는 무료) | 2026-11-12 |

## 배포 (Railway)

- `main`에 머지하면 Railway가 자동으로 빌드·배포한다. `develop`은 배포되지 않는다.
- 빌드는 루트의 `Dockerfile` (JDK 21로 `bootJar` 빌드 → JRE 21 이미지에서 `executor-server.jar` 실행).
- Railway 서비스 설정
  - Root Directory: 비워 둔다 (저장소 루트)
  - Variables: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`, `REDISHOST`, `REDISPORT`, `REDISPASSWORD`는 `${{MySQL.MYSQLHOST}}`처럼 각 서비스 값을 참조하고, `JWT_SECRET`은 직접 넣는다.
  - 서버 포트는 Railway가 넣는 `PORT`를 따른다.
- 배포 전 확인: 새 마이그레이션이 있으면 운영 DB에 먼저 실행했는지.

## 작업 흐름

- 브랜치: `main`(배포) ← `develop`(통합) ← `feature/*`, `fix/*`, `chore/*`
- 작업 브랜치는 `develop`에서 만들고 PR의 base도 `develop`이다. 배포할 때만 `develop` → `main` PR을 연다.
- 커밋·PR 제목은 `feat:`, `fix:`, `chore:` 접두어와 짧은 명사형(예: `feat: 힌트 열람 기록 구현`).

## 구현 메모

- **중복 지급·음수 방지**: 보상 지급, 코인 차감, 아이템 차감은 조건부 UPDATE 한 문장으로 처리하고, 영향받은 행 수로 성공 여부를 판단한다. 이미 있으면 건너뛰는 기록은 `INSERT IGNORE`, 수량 누적은 `ON DUPLICATE KEY UPDATE`.
- **동시 요청 순서 처리**: 같은 대상에 대한 요청은 행 잠금(`SELECT ... FOR UPDATE`)으로 하나씩 처리한다. 두 사용자가 얽힌 친구 요청은 두 프로필을 ID 오름차순으로 잠근다. 잠금 순서는 `mails → profiles → user_stage_hints → user_items`로 맞춘다.
- **시각**: 모든 시각은 UTC 초 단위다. 서버 시계(`Clock.tick` 1초)와 생성·수정 시각(`@CreatedAtSeconds`, `@UpdatedAtSeconds`)을 DB의 `DATETIME`(초 단위)과 맞춰, 저장 직후 응답한 값과 나중에 조회한 값이 같다.
- **목록 API**: 정렬은 서버가 정하고, 페이지 크기는 1~50으로 제한한다.
- **사용자 맵**: `map_data`는 JSON 객체(최대 256KB)만 받는다. 제작자가 클리어해야 공개할 수 있고, 공개된 맵은 수정할 수 없다. 비공개 맵을 수정하면 플레이 기록과 힌트 열람 기록이 지워진다.

## 작업 이력

| PR | 브랜치 | 내용 |
| --- | --- | --- |
| #1 | feature/shop | 상점: 아이템 목록·구매·보유 아이템 |
| #2 | feature/stage-hint | 힌트 사용: 힌트 아이템 차감 후 스테이지 힌트 반환 |
| #3 | feature/admin | 관리자: 작전·공식 스테이지·도감·아이템 관리 |
| #5 | feature/friend | 친구: 요청·수락·거절·취소·삭제 |
| #6 | feature/message | 메시지: 친구에게 보내기, 대화 목록·조회, 읽음 처리 |
| #7 | feature/mail | 우편: 목록·받기·모두 받기, 관리자 발송 |
| #8 | feature/report | 신고: 신고·내 신고, 관리자 목록·상태 변경 |
| #9 | feature/user-map | 사용자 맵: 저장·수정·공개 전환·삭제, 공개 목록 |
| #10 | chore/schema-safety | `schema.sql`에서 DROP 분리, `reset-dev.sql` 안전장치 |
| #11 | fix/test-context | 테스트를 로컬 `executor_test` DB로 분리 (`.env.test`) |
| #12 | chore/cleanup | Redis repository 비활성화, 생성·수정 시각 초 단위 통일 |
| #14 | feature/hint-history | 힌트 열람 기록: 한 번 본 힌트는 무료 |

## 알려진 문제

- 일부 네트워크에서 `*.up.railway.app` 주소가 차단된다 (DNS가 127.0.0.1을 돌려주거나 TLS 연결이 끊김). 이 경우 VPN(전체 트래픽, UDP)을 켜야 배포 서버에 접속된다. 운영 전에 커스텀 도메인 연결을 검토한다.
