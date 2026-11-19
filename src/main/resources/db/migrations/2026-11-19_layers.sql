-- =====================================================================
-- 마이그레이션: 레이어(스토리 대주제) 추가. 작전(Operation)은 반드시 하나의 레이어 안에 있다.
-- =====================================================================
-- 플레이·해금 순서: 레이어 sort_order -> 작전 sort_order(레이어 안) -> 스테이지 번호 (한 줄로 이어서 해금).
-- 작전 sort_order는 전체에서 겹치지 않던 것을 "같은 레이어 안에서 겹치지 않게"로 바꾼다.
--
-- 기존 데이터:
--   레이어 0 "Tutorial"   <- sort_order 0인 작전 (Tutorial)
--   레이어 1 "Layer 1"    <- 나머지 작전 전부 (sort_order는 그대로). 이름은 관리자 웹에서 바꾼다
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다.
--   2. 바로 develop -> main PR을 머지해 새 버전을 배포한다.
--      (그 사이 이전 버전 서버는 작전 "등록"만 실패한다. layer_id 기본값이 없어서. 조회·플레이는 그대로)
-- 두 번 실행하면 CREATE TABLE에서 멈춘다 (데이터는 그대로).
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE layers (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '레이어 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '레이어 이름',
    description  VARCHAR(255)       NULL                COMMENT '레이어 설명',
    sort_order   INT            NOT NULL                COMMENT '표시·플레이 순서 (튜토리얼 = 0)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_layers_name (name),
    UNIQUE KEY uk_layers_sort (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '레이어';

INSERT INTO layers (name, description, sort_order) VALUES
    ('Tutorial', '홀로그램 시뮬레이션', 0),
    ('Layer 1', NULL, 1);

ALTER TABLE operations
    ADD COLUMN layer_id BIGINT NULL COMMENT '소속 레이어 ID' AFTER id;

UPDATE operations o
    JOIN layers l ON l.sort_order = 0
SET o.layer_id = l.id
WHERE o.sort_order = 0;

UPDATE operations o
    JOIN layers l ON l.sort_order = 1
SET o.layer_id = l.id
WHERE o.layer_id IS NULL;

ALTER TABLE operations
    MODIFY COLUMN layer_id BIGINT NOT NULL COMMENT '소속 레이어 ID',
    MODIFY COLUMN sort_order INT NOT NULL COMMENT '레이어 안에서의 표시 순서 (튜토리얼 = 0)',
    DROP INDEX uk_operations_sort,
    ADD UNIQUE KEY uk_operations_layer_sort (layer_id, sort_order),
    ADD CONSTRAINT fk_operations_layer
        FOREIGN KEY (layer_id) REFERENCES layers (id) ON DELETE RESTRICT;
