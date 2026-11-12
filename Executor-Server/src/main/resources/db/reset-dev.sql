-- =====================================================================
-- Executor(집행자) - 개발·테스트 DB 초기화 (모든 테이블 삭제)
-- =====================================================================
-- !! 운영 DB(Railway)에서 실행하지 말 것. 모든 데이터가 사라진다. !!
--
-- 안전장치: 현재 DB 이름에 'test' 또는 'dev'가 없으면 첫 문장에서 오류(1242)를 내고 멈춘다.
--   mysql 클라이언트는 오류가 나면 나머지 문장을 실행하지 않는다 (--force 옵션을 쓰지 말 것).
--   Railway DB 이름은 'railway'이므로 여기서 멈춘다.
--
-- 사용 예 (로컬 테스트 DB를 처음부터 다시 만들 때):
--   mysql -u root -p executor_test < src/main/resources/db/reset-dev.sql
--   mysql -u root -p executor_test < src/main/resources/db/schema.sql
-- =====================================================================

-- DB 이름이 test/dev를 포함하지 않으면 서브쿼리가 2행을 돌려줘서 "Subquery returns more than 1 row" 오류로 멈춘다
DO (SELECT 1 FROM (SELECT 1 AS n UNION ALL SELECT 2) AS guard
    WHERE DATABASE() IS NULL OR (DATABASE() NOT LIKE '%test%' AND DATABASE() NOT LIKE '%dev%'));

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS report_categories;
DROP TABLE IF EXISTS mails;
DROP TABLE IF EXISTS user_items;
DROP TABLE IF EXISTS user_guidebooks;
DROP TABLE IF EXISTS guidebooks;
DROP TABLE IF EXISTS user_stages;
DROP TABLE IF EXISTS stages;
DROP TABLE IF EXISTS operations;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS friend_requests;
DROP TABLE IF EXISTS friends;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;
