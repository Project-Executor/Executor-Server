-- =====================================================================
-- 마이그레이션: 상점 아이템 종류 추가 (커서, 델타 꾸미기)와 장착 칸
-- =====================================================================
-- items.item_type에 CURSOR(메인 터미널 커서 모양), DELTA(델타 얼굴 꾸미기)를 더하고,
-- users에 장착 칸 equipped_cursor_id, equipped_delta_id를 만든다. 기존 데이터는 그대로다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 새 종류·열을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 열들이 없으면 시작하지 않는다)
-- =====================================================================

ALTER TABLE items
    MODIFY COLUMN item_type ENUM('SKIN', 'THEME', 'CURSOR', 'DELTA') NOT NULL
        COMMENT 'SKIN: 캐릭터 스킨, THEME: 화면 테마, CURSOR: 터미널 커서, DELTA: 델타 꾸미기';

ALTER TABLE users
    ADD COLUMN equipped_cursor_id BIGINT NULL COMMENT '장착한 커서 (NULL = 기본)' AFTER equipped_theme_id,
    ADD COLUMN equipped_delta_id  BIGINT NULL COMMENT '장착한 델타 꾸미기 (NULL = 없음)' AFTER equipped_cursor_id,
    ADD CONSTRAINT fk_users_cursor FOREIGN KEY (equipped_cursor_id) REFERENCES items (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_delta FOREIGN KEY (equipped_delta_id) REFERENCES items (id) ON DELETE SET NULL;
