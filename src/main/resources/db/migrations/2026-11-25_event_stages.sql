-- =====================================================================
-- 마이그레이션: 이벤트 스테이지 (전시용)
-- =====================================================================
-- stages.stage_type에 EVENT_TURNS(턴수 이벤트), EVENT_TYPING(타자 이벤트)를 더하고,
-- 로그인 없이 이름을 적어 올리는 기록 테이블 event_records를 만든다. 기존 데이터는 그대로다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 새 종류·테이블을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 테이블이 없으면 시작하지 않는다)
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE stages
    MODIFY COLUMN stage_type ENUM('TUTORIAL', 'NORMAL', 'BOSS', 'EVENT_TURNS', 'EVENT_TYPING') NOT NULL DEFAULT 'NORMAL'
        COMMENT '스테이지 종류 (EVENT_*: 전시용 이벤트, 이름을 적어 event_records에 순위)';

-- 이벤트 스테이지 기록 (전시용, 로그인 없이 이름만). 같은 스테이지에서 이름은 겹칠 수 없다 (정렬 규칙상 대소문자 무시)
--  - score: 턴수 이벤트(EVENT_TURNS)는 턴(적을수록 위), 타자 이벤트(EVENT_TYPING)는 친 단어 수(많을수록 위)
CREATE TABLE IF NOT EXISTS event_records (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    stage_id    BIGINT       NOT NULL                COMMENT '이벤트 스테이지 ID',
    name        VARCHAR(20)  NOT NULL                COMMENT '순위에 보일 이름 (1~12자)',
    score       INT          NOT NULL                COMMENT '턴수 이벤트: 턴, 타자 이벤트: 친 단어 수',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '올린 시각 (같은 점수면 먼저 올린 기록이 위)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_event_records_name (stage_id, name),
    KEY idx_event_records_rank (stage_id, score, created_at),
    CONSTRAINT chk_event_records_score CHECK (score >= 0),
    CONSTRAINT fk_event_records_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '이벤트 스테이지 기록';
