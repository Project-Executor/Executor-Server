-- =====================================================================
-- 마이그레이션: 방명록 (guestbook_entries)
-- =====================================================================
-- 전시에서 로그인 없이 남기는 방명록. 새 테이블 하나만 만든다. 기존 데이터는 그대로다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 이 테이블을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 테이블이 없으면 시작하지 않는다)
-- =====================================================================

SET NAMES utf8mb4;

-- 방명록 (전시용, 로그인 없이 이름만). 욕설·개인정보는 서버가 * 로 가려서 저장한다
--  - user_id: 로그인하고 남겼으면 (하루 첫 글 코인 보상 판단), device_id: 같은 기기 5초 연속 작성 제한
CREATE TABLE IF NOT EXISTS guestbook_entries (
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
