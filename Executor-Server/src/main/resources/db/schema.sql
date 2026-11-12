-- =====================================================================
-- Executor(집행자) - MySQL 8 스키마
-- =====================================================================
-- 대상: Railway MySQL (MySQL 8.0.16 이상, CHECK 제약 사용)
--
-- [실행 방법]
--  - 빈 DB에 처음 한 번만 실행한다. 테이블이 이미 있으면 첫 CREATE TABLE에서 오류가 나고 멈추므로
--    운영 DB에 실수로 실행해도 기존 데이터는 지워지지 않는다.
--  - 개발·테스트 DB를 처음부터 다시 만들려면 먼저 reset-dev.sql을 실행한다
--    (DB 이름에 test 또는 dev가 들어간 DB에서만 동작한다).
--
-- [공통 규칙]
--  - 모든 PK는 BIGINT AUTO_INCREMENT (JPA의 Long과 매핑)
--  - 문자셋 utf8mb4, 시간은 UTC로 저장 (JDBC URL에 serverTimezone=UTC, 앱에서 변환)
--  - PK는 변하지 않으므로 ON UPDATE는 지정하지 않음 (기본값 RESTRICT)
--  - 상태값은 영문 코드 VARCHAR + CHECK (JPA @Enumerated(EnumType.STRING)과 매핑)
--
-- [DB로 강제할 수 없어 서비스 계층에서 검증할 규칙]
--  MySQL은 ON DELETE CASCADE / SET NULL이 걸린 FK 컬럼을 CHECK에 쓸 수 없음 (에러 3823)
--  1. friends: user_id < friend_id 로 정렬해서 한 행만 저장
--  2. friend_requests: sender_id <> receiver_id, 이미 친구면 요청 불가,
--     상대가 보낸 PENDING 요청이 있으면 새 요청 대신 그 요청을 수락 처리
--  3. stages: 공식 스테이지는 operation_id/stage_no NOT NULL + creator_id NULL,
--             사용자 맵은 operation_id/stage_no NULL + creator_id NOT NULL + reward_coin = 0
--  4. stages: 사용자 맵은 제작자가 직접 클리어한 기록(user_stages)이 있어야 is_public = TRUE 가능
--  5. profiles: equipped_*_id는 본인이 보유한 해당 종류(SKIN/THEME) 아이템만
--  6. mails: reward_item_id가 NULL이면 reward_item_quantity = 0
--  7. reports: 버그 제보·기타가 아니면 target_user_id / target_stage_id 중 하나 필수 (둘 다 지정 불가)
--
-- [동시성: 보상 중복 지급 방지 패턴]
--  첫 클리어 보상 : UPDATE user_stages SET first_cleared_at = NOW()
--                  WHERE user_id = ? AND stage_id = ? AND first_cleared_at IS NULL;
--                  -> 영향받은 행이 1일 때만 같은 트랜잭션에서 코인 지급
--  우편 수령      : UPDATE mails SET claimed_at = NOW()
--                  WHERE id = ? AND receiver_id = ? AND claimed_at IS NULL
--                    AND (expires_at IS NULL OR expires_at > NOW());
--  코인 차감      : UPDATE profiles SET coin = coin - ? WHERE user_id = ? AND coin >= ?;
-- =====================================================================

SET NAMES utf8mb4;


-- =====================================================================
-- 1. 계정 / 프로필
-- =====================================================================

-- 계정 (인증 정보만 보관)
CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '계정 ID',
    email       VARCHAR(255)    NOT NULL                COMMENT '이메일 (로그인 ID)',
    password    VARCHAR(255)    NOT NULL                COMMENT 'BCrypt 해시',
    role        ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '권한',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '계정';


-- 상점 아이템 (profiles가 장착 아이템으로 참조하므로 먼저 생성)
CREATE TABLE items (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '아이템 ID',
    name        VARCHAR(100)    NOT NULL                COMMENT '아이템 이름',
    image       VARCHAR(255)        NULL                COMMENT '아이템 이미지 경로',
    description VARCHAR(255)        NULL                COMMENT '아이템 설명',
    item_type   ENUM('SKIN', 'THEME', 'HINT') NOT NULL  COMMENT 'SKIN: 캐릭터 스킨, THEME: 화면 테마, HINT: 힌트 소모품',
    price       INT             NOT NULL                COMMENT '가격 (코인)',
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE   COMMENT '판매 중 여부 (삭제 대신 FALSE로)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',

    PRIMARY KEY (id),
    UNIQUE KEY uk_items_name (name),
    CONSTRAINT chk_items_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '상점 아이템';


-- 프로필 (users와 1:1)
CREATE TABLE profiles (
    user_id           BIGINT        NOT NULL              COMMENT '사용자 ID',
    nickname          VARCHAR(20)   NOT NULL              COMMENT '닉네임 (친구 검색용, 중복 불가)',
    character_name    VARCHAR(20)       NULL              COMMENT '튜토리얼에서 입력하는 집행자 이름 (중복 허용, 입력 전 NULL)',
    comment           VARCHAR(255)      NULL              COMMENT '자기소개',
    coin              INT           NOT NULL DEFAULT 0    COMMENT '보유 코인',
    equipped_skin_id  BIGINT            NULL              COMMENT '장착한 스킨 (NULL = 기본)',
    equipped_theme_id BIGINT            NULL              COMMENT '장착한 테마 (NULL = 기본)',

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_profiles_nickname (nickname),
    CONSTRAINT chk_profiles_coin CHECK (coin >= 0),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_profiles_skin
        FOREIGN KEY (equipped_skin_id) REFERENCES items (id) ON DELETE SET NULL,
    CONSTRAINT fk_profiles_theme
        FOREIGN KEY (equipped_theme_id) REFERENCES items (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '프로필';


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
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend
        FOREIGN KEY (friend_id) REFERENCES users (id) ON DELETE CASCADE
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
        FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_friend_requests_receiver
        FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
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
        FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '메시지';


-- =====================================================================
-- 3. 작전 / 스테이지
-- =====================================================================

-- 작전 (스테이지 상위 분류)
CREATE TABLE operations (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '작전 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '작전 이름',
    description  VARCHAR(255)       NULL                COMMENT '작전 설명',
    sort_order   INT            NOT NULL                COMMENT '표시 순서 (튜토리얼 = 0)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_operations_name (name),
    UNIQUE KEY uk_operations_sort (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '작전';


-- 스테이지 (공식 스테이지 + 사용자 제작 맵)
--  - creator_id IS NULL  -> 공식 스테이지
--  - 맵 구성(타일, 장애물, 열쇠, NPC, 대화/선택지, 추격 봇 미니게임, 숨겨진 버그/피스, 힌트)은 map_data(JSON)에 저장
--  - 별 조건: 1 = 클리어, 2 = best_moves <= par_moves, 3 = 숨겨진 버그 처치 또는 피스 수집
CREATE TABLE stages (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '스테이지 ID',
    operation_id  BIGINT             NULL                COMMENT '작전 ID (사용자 맵은 NULL)',
    stage_no      INT                NULL                COMMENT '작전 내 순서 (사용자 맵은 NULL)',
    creator_id    BIGINT             NULL                COMMENT '제작자 ID (공식 스테이지는 NULL)',
    stage_type    ENUM('TUTORIAL', 'NORMAL', 'BOSS') NOT NULL DEFAULT 'NORMAL' COMMENT '스테이지 종류',
    title         VARCHAR(100)   NOT NULL                COMMENT '스테이지 이름',
    map_data      JSON           NOT NULL                COMMENT '맵 데이터',
    par_moves     INT            NOT NULL                COMMENT '기준 턴 수 (별 2 조건)',
    reward_coin   INT            NOT NULL DEFAULT 0      COMMENT '첫 클리어 보상 코인 (사용자 맵은 0)',
    is_public     BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '공개 여부 (사용자 맵은 제작자 클리어 후 공개 가능)',
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
    -- 작전을 지울 때 소속 공식 스테이지가 함께 사라지지 않도록 RESTRICT
    CONSTRAINT fk_stages_operation
        FOREIGN KEY (operation_id) REFERENCES operations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_stages_creator
        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '스테이지';


-- 사용자의 스테이지 기록 (첫 플레이 시 INSERT ... ON DUPLICATE KEY UPDATE 로 생성)
--  - 클리어 여부 = first_cleared_at IS NOT NULL   (별 1)
--  - 기준 턴 달성 = best_moves <= stages.par_moves (별 2, 계산으로 구하므로 컬럼 없음)
--  - 숨겨진 요소   = is_hidden_found              (별 3, 한 번 달성하면 유지)
CREATE TABLE user_stages (
    id                BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id           BIGINT     NOT NULL                COMMENT '사용자 ID',
    stage_id          BIGINT     NOT NULL                COMMENT '스테이지 ID',
    best_moves        INT            NULL                COMMENT '클리어 시 최소 행동 횟수 (미클리어 NULL, 친구 기록 비교용)',
    is_hidden_found   BOOLEAN    NOT NULL DEFAULT FALSE  COMMENT '숨겨진 버그 처치 / 피스 수집 달성 여부',
    play_count        INT        NOT NULL DEFAULT 0      COMMENT '도전 횟수',
    first_cleared_at  DATETIME       NULL                COMMENT '첫 클리어 날짜 (NULL -> 값 전환 시 보상 지급)',
    last_played_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '마지막 플레이 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stages (user_id, stage_id),
    KEY idx_user_stages_ranking (stage_id, best_moves),
    CONSTRAINT chk_user_stages_best_moves CHECK (best_moves IS NULL OR best_moves >= 1),
    CONSTRAINT chk_user_stages_play_count CHECK (play_count >= 0),
    CONSTRAINT fk_user_stages_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_stages_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 스테이지 기록';


-- =====================================================================
-- 4. 도감
-- =====================================================================

-- 도감 (이상 개체 / 개발 용어)
-- 해금 시점: 플레이 중 해당 개체를 처음 만났을 때 (map_data에서 guidebook id를 참조)
CREATE TABLE guidebooks (
    id         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '도감 ID',
    stage_id   BIGINT              NULL                COMMENT '처음 등장하는 스테이지',
    category   ENUM('BUG', 'VIRUS', 'TERM') NOT NULL   COMMENT 'BUG: 오류, VIRUS: 바이러스, TERM: 개발 용어',
    title      VARCHAR(255)    NOT NULL                COMMENT '이름 (예: NullPointerException)',
    image      VARCHAR(255)        NULL                COMMENT '도감 이미지 경로',
    comment    TEXT            NOT NULL                COMMENT '설명',

    PRIMARY KEY (id),
    UNIQUE KEY uk_guidebooks_title (title),
    KEY idx_guidebooks_stage (stage_id),
    CONSTRAINT fk_guidebooks_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '도감';


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
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guidebooks_guidebook
        FOREIGN KEY (guidebook_id) REFERENCES guidebooks (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 도감';


-- =====================================================================
-- 5. 상점 / 우편
-- =====================================================================

-- 사용자의 보유 아이템
--  - 구매 시 profiles.coin 차감과 INSERT/수량 증가를 하나의 트랜잭션으로 처리
--  - SKIN/THEME는 quantity = 1 고정, 이미 보유 중이면 구매 불가 (우편 보상이면 코인으로 환산 등 앱에서 처리)
--  - HINT는 ON DUPLICATE KEY UPDATE quantity = quantity + ? 로 증가, 사용 시 감소 (0이 되어도 행 유지)
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
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- 이미 구매한 사용자가 있는 아이템이 사라지지 않도록 RESTRICT (판매 중단은 is_active = FALSE)
    CONSTRAINT fk_user_items_item
        FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자의 보유 아이템';


-- 우편 (시스템 -> 사용자 보상)
CREATE TABLE mails (
    id                    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '우편 ID',
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
        FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mails_item
        FOREIGN KEY (reward_item_id) REFERENCES items (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '우편';


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
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_category
        FOREIGN KEY (category_id) REFERENCES report_categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_target_user
        FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_target_stage
        FOREIGN KEY (target_stage_id) REFERENCES stages (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '신고';


-- =====================================================================
-- 7. 기초 데이터
-- =====================================================================

-- INSERT IGNORE: 이미 있으면 건너뛴다 (UNIQUE name)
INSERT IGNORE INTO report_categories (name) VALUES
    ('부적절한 닉네임'),
    ('불량 맵'),
    ('버그 제보'),
    ('기타');

INSERT IGNORE INTO operations (name, description, sort_order) VALUES
    ('Tutorial', '홀로그램 시뮬레이션', 0);
