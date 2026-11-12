-- =====================================================================
-- 마이그레이션: 힌트 열람 기록 테이블 추가 (PR #14 feature/hint-history)
-- =====================================================================
-- 운영 DB(Railway) 적용: 2026-11-12 완료
--
-- 한 번 본 힌트는 다시 볼 때 힌트 아이템을 쓰지 않도록, 본 힌트를 (사용자, 스테이지, 힌트 번호)로 기록한다.
-- 이 테이블이 없는 DB에 이 버전 이상의 서버를 배포하면 ddl-auto=validate에서 기동이 실패한다.
--
-- 테이블을 추가하기만 하므로 기존 데이터에는 영향이 없다. IF NOT EXISTS라 여러 번 실행해도 된다.
-- 새 DB는 schema.sql에 이미 들어 있으므로 이 파일을 따로 실행하지 않아도 된다.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_stage_hints (
    id          BIGINT     NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id     BIGINT     NOT NULL                COMMENT '사용자 ID',
    stage_id    BIGINT     NOT NULL                COMMENT '스테이지 ID',
    hint_index  INT        NOT NULL                COMMENT 'map_data.hints 배열 번호 (0부터)',
    viewed_at   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '처음 본 날짜',

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stage_hints (user_id, stage_id, hint_index),
    KEY idx_user_stage_hints_stage (stage_id),
    CONSTRAINT chk_user_stage_hints_index CHECK (hint_index >= 0),
    CONSTRAINT fk_user_stage_hints_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_stage_hints_stage
        FOREIGN KEY (stage_id) REFERENCES stages (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '사용자가 본 힌트';
