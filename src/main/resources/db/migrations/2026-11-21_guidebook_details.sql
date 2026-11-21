-- =====================================================================
-- 마이그레이션: 도감 상세 항목 추가 (언어, 코드 예시, 대처, 맵에서)
-- =====================================================================
-- 도감 상세 창(시안 PopGuide)에 보일 항목. 모두 빈칸 허용이라 기존 도감은 그대로 동작한다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 새 열을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 열들이 없으면 시작하지 않는다)
-- =====================================================================

ALTER TABLE guidebooks
    ADD COLUMN language VARCHAR(50)  NULL COMMENT '언어 (예: Java)' AFTER comment,
    ADD COLUMN code     TEXT         NULL COMMENT '코드 예시' AFTER language,
    ADD COLUMN remedy   VARCHAR(255) NULL COMMENT '대처 방법' AFTER code,
    ADD COLUMN map_note VARCHAR(255) NULL COMMENT '맵에서의 모습' AFTER remedy;
