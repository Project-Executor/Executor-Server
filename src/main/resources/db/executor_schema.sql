-- =====================================================================
-- Executor(집행자) - 데이터베이스 스키마 (MySQL 8)
-- =====================================================================
-- Executor는 노트북 속 세계에 갇힌 주인공이 버그를 없애며 탈출하는 턴제 그리드 퍼즐 게임이다.
-- 게임(Unity)과 관리자 웹(React)이 게임 서버(Spring Boot)의 REST API를 부르고,
-- 서버가 이 스키마의 MySQL에 모든 데이터를 저장한다. (Refresh Token만 Redis에 따로 둔다)
--
-- 대상 DBMS : MySQL 8.0.16 이상 (CHECK 제약 사용), InnoDB, utf8mb4 / utf8mb4_0900_ai_ci
-- 실행 방법 : 빈 데이터베이스에 이 파일을 한 번 실행한다. 맨 끝의 기초 데이터까지 함께 들어간다.
--
-- ---------------------------------------------------------------------
-- 테이블 목록 (24개)
-- ---------------------------------------------------------------------
--  1. 계정 / 프로필      accounts, items, users, user_avatars
--  2. 친구 / 메시지      friends, friend_requests, messages
--  3. 작전 / 스테이지    layers, operations, stages, user_stages
--  4. 도감 / 콘텐츠      guidebooks, word_lists, word_list_words, characters, user_guidebooks
--  5. 상점 / 메일        user_items, mails
--  6. 신고              report_categories, reports
--  7. 공지사항          notices
--  8. 전시용 기능        event_records, guestbook_entries, app_settings
--
-- 주요 관계
--  accounts 1 ─ 1 users ─ 0..1 user_avatars          (로그인 정보 / 게임 정보 / 프로필 사진)
--  layers 1 ─ N operations 1 ─ N stages               (레이어 → 작전 → 스테이지)
--  accounts N ─ M stages     : user_stages            (기록·별)
--  accounts N ─ M items      : user_items             (보유 아이템), users.equipped_*_id (장착)
--  accounts N ─ M guidebooks : user_guidebooks        (도감 해금)
--  accounts N ─ M accounts   : friends, friend_requests, messages
--  stages 1 ─ N event_records                         (전시 이벤트 순위)
--
-- ---------------------------------------------------------------------
-- 공통 규칙
-- ---------------------------------------------------------------------
--  - PK는 BIGINT AUTO_INCREMENT (JPA의 Long과 매핑). 예외:
--      users·user_avatars   : 상위 테이블의 ID를 그대로 PK로 쓴다 (1:1)
--      word_list_words      : (단어 목록 ID, 목록 안 순서) 복합 PK
--      app_settings         : 설정 이름이 PK
--  - 이메일·닉네임처럼 바뀔 수 있는 값은 PK로 쓰지 않고 UNIQUE로 둔다
--  - 시각은 DATETIME에 UTC로 저장한다 (JDBC serverTimezone=UTC). 화면에 보일 때 한국 시간으로 바꾼다
--  - 코드가 값마다 다르게 동작하는 고정 종류는 ENUM (권한, 아이템 종류, 스테이지 종류 등)
--    처리 단계가 바뀌는 상태값은 VARCHAR + CHECK (친구 요청, 신고)
--    관리자가 늘리고 줄이는 분류는 별도 테이블 (신고 카테고리)
--  - 제약 이름: uk_ (UNIQUE), idx_ (인덱스), fk_ (외래 키), chk_ (CHECK)
--  - 삭제 정책
--      CASCADE  : 계정을 지우면 그 사람의 기록·보유 아이템·메일·친구·메시지도 지운다 (탈퇴)
--      SET NULL : 지워도 남아야 하는 기록 (신고의 신고자·대상, 방명록 작성자, 장착 아이템 → 기본값)
--      RESTRICT : 지우면 다른 데이터가 깨지는 것 (작전이 남은 레이어, 누군가 보유한 아이템 → 판매 중지로 대신)
--  - 맵(지형·오브젝트·대화·이야기·별 조건)은 stages.map_data에 JSON으로 통째로 저장한다
--    (게임이 항상 한 판 전체를 읽고, 새 요소가 자주 추가되므로 테이블로 나누지 않음)
--
-- ---------------------------------------------------------------------
-- DB로 강제할 수 없어 서버(서비스 계층)가 검증하는 규칙
-- ---------------------------------------------------------------------
--  MySQL은 ON DELETE CASCADE / SET NULL이 걸린 FK 컬럼을 CHECK에 쓸 수 없다 (에러 3823)
--  1. friends: user_id < friend_id 로 정렬해서 한 관계를 한 행으로만 저장
--  2. friend_requests: 자기 자신에게 요청 불가, 이미 친구면 요청 불가,
--     상대가 보낸 PENDING 요청이 있으면 새 요청 대신 그 요청을 수락 처리
--  3. stages: 공식 스테이지는 operation_id·stage_no NOT NULL + creator_id NULL,
--             사용자 맵은 operation_id·stage_no NULL + creator_id NOT NULL + reward_coin = 0
--  4. stages: 사용자 맵은 제작자가 직접 클리어한 기록(user_stages)이 있어야 is_public = TRUE 가능
--  5. users: equipped_*_id는 본인이 보유한 해당 종류(SKIN/THEME/CURSOR/DELTA) 아이템만
--  6. mails: reward_item_id가 NULL이면 reward_item_quantity = 0
--  7. reports: 버그 제보·기타가 아니면 target_user_id / target_stage_id 중 하나 필수 (둘 다 지정 불가)
--  8. map_data 안의 참조(단어 목록 ID, 도감 ID)는 FK로 검사할 수 없어 게임·관리자 웹이 검사한다
--
-- ---------------------------------------------------------------------
-- 동시 요청에서 정합성을 지키는 방법
-- ---------------------------------------------------------------------
--  첫 클리어 보상 : UPDATE user_stages SET first_cleared_at = ?
--                  WHERE user_id = ? AND stage_id = ? AND first_cleared_at IS NULL;
--                  -> 영향받은 행이 1일 때만 같은 트랜잭션에서 코인 지급 (두 번 지급 방지)
--  메일 수령      : UPDATE mails SET claimed_at = ?
--                  WHERE id = ? AND receiver_id = ? AND claimed_at IS NULL
--                    AND (expires_at IS NULL OR expires_at > ?);
--  코인 차감      : UPDATE users SET coin = coin - ? WHERE user_id = ? AND coin >= ?;   (+ CHECK coin >= 0)
--  중복 방지      : 아이템 구매·이벤트 이름·닉네임은 UNIQUE 제약이 최종 방어선 (위반 시 서버가 오류 응답으로 변환)
-- =====================================================================

SET NAMES utf8mb4;


-- =====================================================================
-- 1. 계정 / 프로필
-- =====================================================================

-- 로그인 계정 (인증 정보만 보관. 게임 정보는 users)
--  - 이메일은 소문자로 정리해서 저장, 비밀번호는 BCrypt 해시
CREATE TABLE accounts (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '계정 ID',
    email       VARCHAR(255)    NOT NULL                COMMENT '이메일 (로그인 ID)',
    password    VARCHAR(255)    NOT NULL                COMMENT 'BCrypt 해시',
    role        ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '권한',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    suspended_until DATETIME    NULL                    COMMENT '정지 기한 (UTC). NULL이면 정지 아님, 9999-12-31 23:59:59면 영구 정지',
    suspend_reason  VARCHAR(200) NULL                   COMMENT '정지 사유 (로그인 거부 메시지에 표시)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '로그인 계정';


-- 상점 아이템 (users가 장착 아이템으로 참조하므로 먼저 생성)
CREATE TABLE items (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '아이템 ID',
    name        VARCHAR(100)    NOT NULL                COMMENT '아이템 이름',
    image       VARCHAR(255)        NULL                COMMENT '그림 키 (스킨·테마: 그림 이름, 커서: 모양:색 예 bar-fast:#4AFF8C, 델타: 꾸미기 이름 예 antenna)',
    description VARCHAR(255)        NULL                COMMENT '아이템 설명',
    item_type   ENUM('SKIN', 'THEME', 'CURSOR', 'DELTA') NOT NULL COMMENT 'SKIN: 캐릭터 스킨, THEME: 화면 테마, CURSOR: 터미널 커서, DELTA: 델타 꾸미기',
    price       INT             NOT NULL                COMMENT '가격 (코인)',
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE   COMMENT '판매 중 여부 (삭제 대신 FALSE로)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',

    PRIMARY KEY (id),
    UNIQUE KEY uk_items_name (name),
    CONSTRAINT chk_items_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '상점 아이템';


-- 사용자 (게임 정보: 닉네임·코인·장착 아이템). accounts와 1:1이고 PK가 accounts.id와 같다
CREATE TABLE users (
    user_id           BIGINT        NOT NULL              COMMENT '사용자 ID',
    nickname          VARCHAR(20)       NULL              COMMENT '닉네임 2~20자 (중복 불가, 가입 후 튜토리얼에서 입력, 입력 전 NULL)',
    comment           VARCHAR(255)      NULL              COMMENT '자기소개',
    coin              INT           NOT NULL DEFAULT 0    COMMENT '보유 코인',
    equipped_skin_id  BIGINT            NULL              COMMENT '장착한 스킨 (NULL = 기본)',
    equipped_theme_id BIGINT            NULL              COMMENT '장착한 테마 (NULL = 기본)',
    equipped_cursor_id BIGINT           NULL              COMMENT '장착한 커서 (NULL = 기본)',
    equipped_delta_id BIGINT            NULL              COMMENT '장착한 델타 꾸미기 (NULL = 없음)',

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_nickname (nickname),
    CONSTRAINT chk_users_coin CHECK (coin >= 0),
    CONSTRAINT fk_users_account
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_users_skin
        FOREIGN KEY (equipped_skin_id) REFERENCES items (id) ON DELETE SET NULL,
    CONSTRAINT fk_users_theme
        FOREIGN KEY (equipped_theme_id) REFERENCES items (id) ON DELETE SET NULL,
    CONSTRAINT fk_users_cursor
        FOREIGN KEY (equipped_cursor_id) REFERENCES items (id) ON DELETE SET NULL,
    CONSTRAINT fk_users_delta
        FOREIGN KEY (equipped_delta_id) REFERENCES items (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자';

-- 프로필 사진 (없으면 행이 없고 게임은 기본 실루엣을 쓴다). users와 1:1이지만 그림이 커서 따로 둔다
--  - 프로필·순위·친구 목록을 읽을 때마다 그림까지 읽지 않도록 분리. 게임이 256x256으로 잘라 보낸다
CREATE TABLE user_avatars (
    user_id     BIGINT      NOT NULL COMMENT '사용자 ID',
    image       MEDIUMBLOB  NOT NULL COMMENT 'PNG 또는 JPEG (512KB 이하)',
    content_type VARCHAR(20) NOT NULL COMMENT 'image/png 또는 image/jpeg',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '바꾼 시각 (게임의 그림 캐시 버전)',

    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_avatars_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '프로필 사진';


-- =====================================================================
-- 2. 친구 / 메시지
-- =====================================================================

-- 친구 관계 (양방향이므로 user_id < friend_id 로 한 행만 저장, 조회 시 두 컬럼 모두 검색)
CREATE TABLE friends (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id     BIGINT      NOT NULL                COMMENT '두 사용자 중 작은 ID',
    friend_id   BIGINT      NOT NULL                COMMENT '두 사용자 중 큰 ID',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '친구가 된 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_friends_pair (user_id, friend_id),
    KEY idx_friends_friend (friend_id),
    CONSTRAINT fk_friends_user
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend
        FOREIGN KEY (friend_id) REFERENCES accounts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '친구';


-- 친구 요청 (ACCEPTED가 되면 같은 트랜잭션에서 friends에 한 행 추가)
-- 거절 후 다시 요청하면 새 행을 만들지 않고 같은 행을 PENDING으로 되돌림
CREATE TABLE friend_requests (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    sender_id     BIGINT        NOT NULL                COMMENT '요청을 보낸 사용자',
    receiver_id   BIGINT        NOT NULL                COMMENT '요청을 받은 사용자',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACCEPTED / REJECTED',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '요청한 날짜',
    responded_at  DATETIME          NULL                COMMENT '수락/거절한 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_friend_requests_pair (sender_id, receiver_id),
    KEY idx_friend_requests_receiver (receiver_id, status),
    CONSTRAINT chk_friend_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT fk_friend_requests_sender
        FOREIGN KEY (sender_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_requests_receiver
        FOREIGN KEY (receiver_id) REFERENCES accounts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '친구 요청';


-- 메시지 (사용자 ↔ 사용자 대화. 시스템 → 사용자 보상은 mails)
CREATE TABLE messages (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '메시지 ID',
    sender_id    BIGINT         NOT NULL                COMMENT '보낸 사용자',
    receiver_id  BIGINT         NOT NULL                COMMENT '받는 사용자',
    content      VARCHAR(500)   NOT NULL                COMMENT '내용',
    is_read      BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '읽음 여부',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '보낸 날짜',

    PRIMARY KEY (id),
    KEY idx_messages_receiver (receiver_id, is_read, created_at),
    KEY idx_messages_sender (sender_id, created_at),
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id) REFERENCES accounts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '메시지';


-- =====================================================================
-- 3. 작전 / 스테이지
-- =====================================================================

-- 레이어 (스토리 대주제, 작전을 묶는다)
--  - sort_order가 음수(-1, -2, ...)면 비공개 레이어: 게임 목록·플레이·해금 순서에서 빠지고 관리자 웹에만 보인다
CREATE TABLE layers (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '레이어 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '레이어 이름',
    description  VARCHAR(255)       NULL                COMMENT '레이어 설명',
    sort_order   INT            NOT NULL                COMMENT '표시·플레이 순서 (튜토리얼 = 0, 음수면 비공개)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_layers_name (name),
    UNIQUE KEY uk_layers_sort (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '레이어';

-- 작전 (스테이지 상위 분류, 스토리 소주제). 플레이 순서는 레이어 순서 -> 작전 순서 -> 스테이지 번호
CREATE TABLE operations (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '작전 ID',
    layer_id     BIGINT         NOT NULL                COMMENT '소속 레이어 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '작전 이름',
    description  VARCHAR(255)       NULL                COMMENT '작전 설명',
    sort_order   INT            NOT NULL                COMMENT '레이어 안에서의 표시 순서 (튜토리얼 = 0)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_operations_name (name),
    UNIQUE KEY uk_operations_layer_sort (layer_id, sort_order),
    CONSTRAINT fk_operations_layer
        FOREIGN KEY (layer_id) REFERENCES layers (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '작전';


-- 스테이지 (공식 스테이지 + 사용자 제작 맵을 한 테이블에)
--  - creator_id IS NULL -> 공식 스테이지 (작전 소속), NOT NULL -> 사용자 제작 맵
--  - map_data(JSON): 지형(벽·바닥), 시작 위치, 오브젝트(출구·상자·방화벽·열쇠·문·인물·체크포인트·데이터 파편·
--    이상 개체·가짜 벽·감염된 컴퓨터), 인물 대화(선택지), 이야기, 안내 문구, 별 조건, 카메라, 칸별 그림
--  - 별: ★1 클리어, ★2·★3은 맵마다 정한다 (기본 ★2 모든 구간을 턴 안에, ★3 데이터 파편 모두 수집)
--  - 이벤트 스테이지(EVENT_*)는 별·코인 없이 event_records에 이름과 점수로 순위를 남긴다
CREATE TABLE stages (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '스테이지 ID',
    operation_id  BIGINT             NULL                COMMENT '작전 ID (사용자 맵은 NULL)',
    stage_no      INT                NULL                COMMENT '작전 내 순서 (사용자 맵은 NULL)',
    creator_id    BIGINT             NULL                COMMENT '제작자 ID (공식 스테이지는 NULL)',
    stage_type    ENUM('TUTORIAL', 'NORMAL', 'BOSS', 'EVENT_TURNS', 'EVENT_TYPING') NOT NULL DEFAULT 'NORMAL' COMMENT '스테이지 종류 (EVENT_*: 전시용 이벤트, 이름을 적어 event_records에 순위)',
    title         VARCHAR(100)   NOT NULL                COMMENT '스테이지 이름',
    map_data      JSON           NOT NULL                COMMENT '맵 데이터 (JSON)',
    par_moves     INT            NOT NULL                COMMENT '기준 턴 수 (첫 구간에 쓸 수 있는 턴, 이후 구간은 체크포인트마다 map_data에)',
    reward_coin   INT            NOT NULL DEFAULT 0      COMMENT '첫 클리어 보상 코인 (사용자 맵은 0)',
    is_public     BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '공개 여부 (사용자 맵은 제작자 클리어 후 공개 가능)',
    board_x       DOUBLE             NULL                COMMENT '스테이지 선택 기판 가로 위치 (0~1, NULL이면 자동 배치)',
    board_y       DOUBLE             NULL                COMMENT '스테이지 선택 기판 세로 위치 (0~1, NULL이면 자동 배치)',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    PRIMARY KEY (id),
    -- MySQL UNIQUE는 NULL 중복을 허용하므로 사용자 맵(NULL, NULL)끼리는 충돌하지 않음
    UNIQUE KEY uk_stages_operation_no (operation_id, stage_no),
    KEY idx_stages_creator (creator_id),
    KEY idx_stages_public (is_public, created_at),
    CONSTRAINT chk_stages_stage_no CHECK (stage_no IS NULL OR stage_no >= 1),
    CONSTRAINT chk_stages_par_moves CHECK (par_moves >= 1),
    CONSTRAINT chk_stages_reward_coin CHECK (reward_coin >= 0),
    -- 위치는 둘 다 있거나 둘 다 없다
    CONSTRAINT chk_stages_board CHECK ((board_x IS NULL AND board_y IS NULL) OR (board_x BETWEEN 0 AND 1 AND board_y BETWEEN 0 AND 1)),
    -- 작전을 지울 때 소속 공식 스테이지가 함께 사라지지 않도록 RESTRICT
    CONSTRAINT fk_stages_operation
        FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_stages_creator
        FOREIGN KEY (creator_id) REFERENCES accounts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '스테이지';


-- 사용자의 스테이지 기록 (결과를 낼 때마다 INSERT ... ON DUPLICATE KEY UPDATE 로 만들거나 도전 횟수 증가)
--  - ★1 클리어      = first_cleared_at IS NOT NULL (NULL -> 값으로 바뀌는 순간에만 첫 클리어 보상)
--  - ★2 조건 달성   = is_stable_cleared
--  - ★3 조건 달성   = is_hidden_found
--  - 별은 누적: 한 번 달성하면 FALSE로 돌아가지 않는다 (판마다 다른 별을 따도 모두 인정)
--  - 순위 = 클리어한 기록을 best_moves -> first_cleared_at 순으로 정렬
CREATE TABLE user_stages (
    id                BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id           BIGINT     NOT NULL                COMMENT '사용자 ID',
    stage_id          BIGINT     NOT NULL                COMMENT '스테이지 ID',
    best_moves        INT            NULL                COMMENT '클리어한 판의 최소 턴 수 (미클리어 NULL, 순위·친구 기록 비교)',
    is_hidden_found   BOOLEAN    NOT NULL DEFAULT FALSE  COMMENT '★3 조건 달성 (기본: 데이터 파편 모두 수집, 맵마다 다름)',
    is_stable_cleared BOOLEAN    NOT NULL DEFAULT FALSE  COMMENT '★2 조건 달성 (기본: 모든 구간을 턴 안에 통과, 맵마다 다름)',
    play_count        INT        NOT NULL DEFAULT 0      COMMENT '도전 횟수',
    first_cleared_at  DATETIME       NULL                COMMENT '첫 클리어 날짜 (★1, NULL -> 값 전환 시 보상 지급)',
    last_played_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '마지막 플레이 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stages (user_id, stage_id),
    KEY idx_user_stages_ranking (stage_id, best_moves),
    CONSTRAINT chk_user_stages_best_moves CHECK (best_moves IS NULL OR best_moves >= 1),
    CONSTRAINT chk_user_stages_play_count CHECK (play_count >= 0),
    CONSTRAINT fk_user_stages_user
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_stages_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 스테이지 기록';


-- =====================================================================
-- 4. 도감 / 콘텐츠 (단어 목록, 대화 인물)
-- =====================================================================

-- 도감 (오류·바이러스·개발 용어)
--  - 해금 시점: 플레이 중 맵의 이상 개체(anomaly, map_data에서 guidebookId로 참조)를 처음 만났을 때
CREATE TABLE guidebooks (
    id         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '도감 ID',
    stage_id   BIGINT              NULL                COMMENT '처음 등장하는 스테이지',
    category   ENUM('BUG', 'VIRUS', 'TERM') NOT NULL   COMMENT 'BUG: 오류, VIRUS: 바이러스, TERM: 개발 용어',
    title      VARCHAR(255)    NOT NULL                COMMENT '이름 (예: NullPointerException)',
    image      VARCHAR(255)        NULL                COMMENT '도감 이미지 경로',
    comment    TEXT            NOT NULL                COMMENT '설명',
    language   VARCHAR(50)         NULL                COMMENT '언어 (예: Java)',
    code       TEXT                NULL                COMMENT '코드 예시',
    remedy     VARCHAR(255)        NULL                COMMENT '대처 방법',
    map_note   VARCHAR(255)        NULL                COMMENT '맵에서의 모습',

    PRIMARY KEY (id),
    UNIQUE KEY uk_guidebooks_title (title),
    KEY idx_guidebooks_stage (stage_id),
    CONSTRAINT fk_guidebooks_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '도감';


-- 타자 해킹 단어 목록 (맵의 감염된 컴퓨터 computer.wordListId가 참조)
CREATE TABLE word_lists (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '단어 목록 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '목록 이름',
    description  VARCHAR(255)       NULL                COMMENT '설명',

    PRIMARY KEY (id),
    UNIQUE KEY uk_word_lists_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '타자 해킹 단어 목록';

CREATE TABLE word_list_words (
    word_list_id BIGINT         NOT NULL COMMENT '단어 목록 ID',
    position     INT            NOT NULL COMMENT '목록 안 순서 (0부터)',
    word         VARCHAR(50)    NOT NULL COMMENT '단어',

    PRIMARY KEY (word_list_id, position),
    CONSTRAINT fk_word_list_words_list
        FOREIGN KEY (word_list_id) REFERENCES word_lists (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '단어 목록의 단어';


-- 대화 인물 (대사의 speaker = name, portrait의 인물 ID = character_key)
CREATE TABLE characters (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '인물 ID',
    character_key  VARCHAR(50)   NOT NULL                COMMENT '일러스트 폴더 ID (Resources/Portraits/{key})',
    name           VARCHAR(50)   NOT NULL                COMMENT '대사의 speaker와 같은 이름',
    tag_color      VARCHAR(7)    NOT NULL                COMMENT '이름표 바탕색 #RRGGBB',
    protagonist    BOOLEAN       NOT NULL DEFAULT FALSE  COMMENT '주인공(대화창 왼쪽)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_characters_key (character_key),
    UNIQUE KEY uk_characters_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '대화 인물';


-- 사용자의 도감 해금 기록
CREATE TABLE user_guidebooks (
    id            BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id       BIGINT     NOT NULL                COMMENT '사용자 ID',
    guidebook_id  BIGINT     NOT NULL                COMMENT '도감 ID',
    unlocked_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '해금 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_guidebooks (user_id, guidebook_id),
    KEY idx_user_guidebooks_guidebook (guidebook_id),
    CONSTRAINT fk_user_guidebooks_user
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guidebooks_guidebook
        FOREIGN KEY (guidebook_id) REFERENCES guidebooks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 도감';


-- =====================================================================
-- 5. 상점 / 메일
-- =====================================================================

-- 사용자의 보유 아이템
--  - 구매는 users.coin 조건부 차감과 INSERT를 하나의 트랜잭션으로 처리
--  - 같은 아이템은 한 행 (UNIQUE). 이미 보유한 아이템은 구매 불가, 메일 보상으로 받으면 가격만큼 코인으로 바꿔 준다
CREATE TABLE user_items (
    id           BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id      BIGINT     NOT NULL                COMMENT '사용자 ID',
    item_id      BIGINT     NOT NULL                COMMENT '아이템 ID',
    quantity     INT        NOT NULL DEFAULT 1      COMMENT '보유 수량',
    acquired_at  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '처음 획득한 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_items (user_id, item_id),
    KEY idx_user_items_item (item_id),
    CONSTRAINT chk_user_items_quantity CHECK (quantity >= 0),
    CONSTRAINT fk_user_items_user
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    -- 이미 구매한 사용자가 있는 아이템이 사라지지 않도록 RESTRICT (판매 중단은 is_active = FALSE)
    CONSTRAINT fk_user_items_item
        FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 보유 아이템';


-- 메일 (관리자·시스템 -> 사용자. 코인·아이템을 첨부할 수 있다)
CREATE TABLE mails (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '메일 ID',
    receiver_id           BIGINT         NOT NULL                COMMENT '받는 사용자',
    title                 VARCHAR(100)   NOT NULL                COMMENT '제목',
    content               TEXT               NULL                COMMENT '내용',
    reward_coin           INT            NOT NULL DEFAULT 0      COMMENT '첨부 코인',
    reward_item_id        BIGINT             NULL                COMMENT '첨부 아이템',
    reward_item_quantity  INT            NOT NULL DEFAULT 0      COMMENT '첨부 아이템 수량',
    claimed_at            DATETIME           NULL                COMMENT '보상 수령 날짜 (NULL = 미수령)',
    expires_at            DATETIME           NULL                COMMENT '만료 날짜 (NULL = 무기한)',
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발송 날짜',

    PRIMARY KEY (id),
    KEY idx_mails_receiver (receiver_id, claimed_at, created_at),
    KEY idx_mails_item (reward_item_id),
    CONSTRAINT chk_mails_reward_coin CHECK (reward_coin >= 0),
    CONSTRAINT chk_mails_reward_qty CHECK (reward_item_quantity >= 0),
    CONSTRAINT fk_mails_receiver
        FOREIGN KEY (receiver_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_mails_item
        FOREIGN KEY (reward_item_id) REFERENCES items (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '메일';


-- =====================================================================
-- 6. 신고
-- =====================================================================

CREATE TABLE report_categories (
    id    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    name  VARCHAR(100)   NOT NULL                COMMENT '카테고리 이름',

    PRIMARY KEY (id),
    UNIQUE KEY uk_report_categories_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '신고 카테고리';


-- 신고 (신고자/대상이 탈퇴하거나 스테이지가 삭제되어도 처리 이력은 남도록 SET NULL)
CREATE TABLE reports (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '신고 ID',
    user_id          BIGINT             NULL                COMMENT '신고자 (탈퇴 시 NULL)',
    category_id      BIGINT         NOT NULL                COMMENT '카테고리',
    target_user_id   BIGINT             NULL                COMMENT '신고 대상 사용자',
    target_stage_id  BIGINT             NULL                COMMENT '신고 대상 스테이지',
    title            VARCHAR(255)   NOT NULL                COMMENT '제목',
    comment          TEXT           NOT NULL                COMMENT '내용',
    status           VARCHAR(20)    NOT NULL DEFAULT 'RECEIVED' COMMENT 'RECEIVED / IN_PROGRESS / DONE',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 날짜',
    handled_at       DATETIME           NULL                COMMENT '처리 완료 날짜',

    PRIMARY KEY (id),
    KEY idx_reports_status (status, created_at),
    KEY idx_reports_user (user_id),
    KEY idx_reports_category (category_id),
    KEY idx_reports_target_user (target_user_id),
    KEY idx_reports_target_stage (target_stage_id),
    CONSTRAINT chk_reports_status CHECK (status IN ('RECEIVED', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT fk_reports_user
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_category
        FOREIGN KEY (category_id) REFERENCES report_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_target_user
        FOREIGN KEY (target_user_id) REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_target_stage
        FOREIGN KEY (target_stage_id) REFERENCES stages (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '신고';


-- =====================================================================
-- 7. 공지사항
-- =====================================================================

-- 공지 (관리자가 작성, 게임에서 목록·상세 조회)
CREATE TABLE notices (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '공지 ID',
    category      ENUM('NOTICE', 'UPDATE', 'EVENT', 'MAINTENANCE') NOT NULL DEFAULT 'NOTICE' COMMENT '분류 (공지·업데이트·이벤트·점검)',
    title         VARCHAR(100)   NOT NULL                COMMENT '제목',
    content       TEXT           NOT NULL                COMMENT '내용 (5000자 이하)',
    is_pinned     BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '상단 고정',
    is_visible    BOOLEAN        NOT NULL DEFAULT TRUE   COMMENT 'FALSE면 숨김 (게임에 안 보임)',
    published_at  DATETIME       NOT NULL                COMMENT '게시 시각 (미래면 예약, 그때부터 보임)',
    ends_at       DATETIME           NULL                COMMENT '게시 종료 시각 (NULL이면 계속)',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    PRIMARY KEY (id),
    KEY idx_notices_list (is_visible, is_pinned, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공지사항';


-- =====================================================================
-- 8. 전시용 기능 (로그인 없이 참여하는 이벤트 순위 / 방명록 / 서버 설정)
-- =====================================================================

-- 이벤트 스테이지 기록 (전시용, 로그인 없이 이름만 적어 올린다)
--  - 같은 스테이지에서 이름은 겹칠 수 없다 (정렬 규칙상 대소문자 무시)
--  - score: 턴수 이벤트(EVENT_TURNS)는 턴(적을수록 위), 타자 이벤트(EVENT_TYPING)는 제한 시간 안에 친 단어 수(많을수록 위)
--  - 같은 점수는 같은 순위이고 이름순으로 보여 준다
CREATE TABLE event_records (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    stage_id    BIGINT       NOT NULL                COMMENT '이벤트 스테이지 ID',
    name        VARCHAR(20)  NOT NULL                COMMENT '순위에 보일 이름 (1~12자)',
    score       INT          NOT NULL                COMMENT '턴수 이벤트: 턴, 타자 이벤트: 친 단어 수',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '올린 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_event_records_name (stage_id, name),
    KEY idx_event_records_rank (stage_id, score, created_at),
    CONSTRAINT chk_event_records_score CHECK (score >= 0),
    CONSTRAINT fk_event_records_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '이벤트 스테이지 기록';


-- 방명록 (전시용, 로그인 없이 이름만). 욕설·전화번호·이메일은 서버가 * 로 가려서 저장한다
--  - user_id: 로그인하고 남겼으면 (그날 첫 글에 코인 10개 보상 판단). 탈퇴해도 글은 남는다 (SET NULL)
--  - device_id: 같은 PC는 5초에 한 번만 쓸 수 있게 하는 데 쓴다
--  - 게임에 보일지는 app_settings의 guestbook.enabled로 관리자가 켜고 끈다
CREATE TABLE guestbook_entries (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'ID',
    name        VARCHAR(20)   NOT NULL                COMMENT '이름 (1~10자)',
    message     VARCHAR(200)  NOT NULL                COMMENT '남긴 말 (1~100자)',
    user_id     BIGINT            NULL                COMMENT '로그인하고 남겼으면 사용자 ID',
    device_id   VARCHAR(64)       NULL                COMMENT '게임이 PC마다 만든 임의 값 (연속 작성 제한)',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '남긴 시각',

    PRIMARY KEY (id),
    KEY idx_guestbook_created (created_at),
    KEY idx_guestbook_device (device_id, created_at),
    KEY idx_guestbook_user (user_id, created_at),
    CONSTRAINT fk_guestbook_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '방명록';


-- 서버 설정 (관리자 웹에서 켜고 끄는 값). 키 하나에 값 하나
--  - guestbook.enabled: 방명록을 게임에 보일지 ('true' / 'false', 기본 'false'). 전시할 때만 켠다
--  - 설정이 늘어도 행만 추가하면 되도록 키-값으로 둔다. 값이 'true'가 아니면 꺼진 것으로 본다
CREATE TABLE app_settings (
    setting_key    VARCHAR(50)   NOT NULL                COMMENT '설정 이름',
    setting_value  VARCHAR(255)  NOT NULL                COMMENT '값',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '바꾼 시각',

    PRIMARY KEY (setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '서버 설정';


-- =====================================================================
-- 9. 기초 데이터
-- =====================================================================

-- INSERT IGNORE: 이미 있으면 건너뛴다 (UNIQUE 키 기준). 여러 번 실행해도 안전하다

-- 신고 카테고리
INSERT IGNORE INTO report_categories (name) VALUES
    ('부적절한 닉네임'),
    ('불량 맵'),
    ('버그 제보'),
    ('기타');

-- 서버 설정 기본값 (방명록은 꺼진 상태로 시작)
INSERT IGNORE INTO app_settings (setting_key, setting_value) VALUES ('guestbook.enabled', 'false');

-- 튜토리얼 레이어와 작전 (스테이지는 관리자 웹에서 등록)
INSERT IGNORE INTO layers (name, description, sort_order) VALUES
    ('Tutorial', '홀로그램 시뮬레이션', 0);

INSERT IGNORE INTO operations (layer_id, name, description, sort_order)
SELECT id, 'Tutorial', '홀로그램 시뮬레이션', 0 FROM layers WHERE sort_order = 0;
