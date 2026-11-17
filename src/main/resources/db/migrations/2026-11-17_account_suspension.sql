-- =====================================================================
-- 마이그레이션: 계정 정지 (accounts.suspended_until, accounts.suspend_reason)
-- =====================================================================
-- 관리자가 계정을 기한부 또는 영구로 정지한다. 정지된 계정은 로그인·토큰 재발급이 거부된다.
--   suspended_until NULL                  -> 정지 아님
--   suspended_until 9999-12-31 23:59:59   -> 영구 정지
--   그 밖의 값                             -> 그 시각(UTC)까지 정지. 지나면 자동으로 풀린다 (지우지 않아도 됨)
--
-- [적용 순서] 컬럼을 추가하기만 하므로 이전 버전 서버도 그대로 동작한다.
--   1. 운영 DB에 이 파일을 실행한다.
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
-- 순서를 반대로 하면 새 서버가 시작할 때 컬럼이 없다는 오류(ddl-auto=validate)로 뜨지 않는다.
-- 두 번 실행하면 "Duplicate column name" 오류로 멈춘다 (데이터는 바뀌지 않음).
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE accounts
    ADD COLUMN suspended_until DATETIME NULL
        COMMENT '정지 기한 (UTC). NULL이면 정지 아님, 9999-12-31 23:59:59면 영구 정지' AFTER created_at,
    ADD COLUMN suspend_reason VARCHAR(200) NULL
        COMMENT '정지 사유 (로그인 거부 메시지에 표시)' AFTER suspended_until;
