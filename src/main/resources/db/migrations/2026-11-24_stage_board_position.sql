-- =====================================================================
-- 마이그레이션: 스테이지 선택 화면의 노드 위치 (stages.board_x, board_y)
-- =====================================================================
-- 관리자 웹 작전 화면에서 스테이지 노드를 끌어다 놓은 위치를 저장한다.
-- 값은 게임 스테이지 선택 기판 안의 비율 좌표(0~1, 노드 가운데, 왼쪽 위 기준)이고,
-- NULL이면 게임이 기본 지그재그 배치를 쓴다. 기존 데이터는 그대로다(모두 NULL = 지금 배치).
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 이 열을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 열이 없으면 시작하지 않는다)
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE stages
    ADD COLUMN board_x DOUBLE NULL COMMENT '스테이지 선택 기판 가로 위치 (0~1, NULL이면 자동 배치)' AFTER is_public,
    ADD COLUMN board_y DOUBLE NULL COMMENT '스테이지 선택 기판 세로 위치 (0~1, NULL이면 자동 배치)' AFTER board_x,
    ADD CONSTRAINT chk_stages_board CHECK (
        (board_x IS NULL AND board_y IS NULL)
        OR (board_x BETWEEN 0 AND 1 AND board_y BETWEEN 0 AND 1));
