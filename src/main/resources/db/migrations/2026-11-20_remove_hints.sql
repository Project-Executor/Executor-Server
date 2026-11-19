-- =====================================================================
-- 마이그레이션: 힌트 기능 제거
-- =====================================================================
-- 힌트 아이템(items.item_type = 'HINT')과 힌트 열람 기록(user_stage_hints)을 없앤다.
-- 2026-11-20 운영 DB 확인: HINT 아이템 0개, 힌트 열람 기록 0건, 힌트를 가진 사용자 0명.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다.
--      (이전 버전 서버는 힌트 API만 실패하고 나머지는 그대로 동작한다. 힌트를 쓰는 화면이 없으므로 영향 없음)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--
-- 안전장치: HINT 아이템이 하나라도 남아 있으면 첫 문장에서 오류(1242)를 내고 멈춘다
--   (mysql 클라이언트는 오류가 나면 나머지 문장을 실행하지 않는다. --force 옵션을 쓰지 말 것).
--   그 경우 관리자 웹에서 그 아이템을 확인하고 정리한 뒤 다시 실행한다.
-- =====================================================================

SET NAMES utf8mb4;

-- HINT 아이템이 있으면 서브쿼리가 2행을 돌려줘서 "Subquery returns more than 1 row" 오류로 멈춘다
DO (SELECT 1 FROM (SELECT 1 AS n UNION ALL SELECT 2) AS guard
    WHERE EXISTS (SELECT 1 FROM items WHERE item_type = 'HINT'));

DROP TABLE IF EXISTS user_stage_hints;

ALTER TABLE items
    MODIFY item_type ENUM('SKIN', 'THEME') NOT NULL COMMENT 'SKIN: 캐릭터 스킨, THEME: 화면 테마';
