-- =====================================================================
-- 마이그레이션: 별 2 기준 변경 (user_stages.is_stable_cleared)
-- =====================================================================
-- 별 2가 "최소 기록 <= 기준 턴"에서 "불안정 체크포인트가 허용 개수 이하로 클리어"로 바뀐다 (docs/map-data.md 3.3, 3.4).
-- 판정에 필요한 불안정 수는 결과 제출 때만 알 수 있어서, 달성 여부를 기록 행에 저장한다 (한 번 달성하면 유지).
--
-- 기존 기록은 예전 기준(최소 기록 <= 기준 턴, 튜토리얼 제외)으로 채워서 이미 받은 별 2가 사라지지 않게 한다.
--
-- [적용 순서] 컬럼을 추가하기만 하므로 이전 버전 서버도 그대로 동작한다.
--   1. 운영 DB에 이 파일을 실행한다.
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
-- 두 번 실행하면 "Duplicate column name" 오류로 멈춘다 (UPDATE는 실행되지 않음, 데이터는 그대로).
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE user_stages
    ADD COLUMN is_stable_cleared BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '별 2: 불안정 체크포인트가 허용 개수(map_data.stars.maxUnstable) 이하로 클리어한 적 있음' AFTER is_hidden_found;

UPDATE user_stages us
    JOIN stages s ON s.id = us.stage_id
SET us.is_stable_cleared = TRUE
WHERE us.first_cleared_at IS NOT NULL
  AND us.best_moves <= s.par_moves
  AND s.stage_type <> 'TUTORIAL';
