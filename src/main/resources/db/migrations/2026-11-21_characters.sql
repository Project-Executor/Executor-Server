-- =====================================================================
-- 마이그레이션: 인물 목록 (대화창 이름표 색·일러스트 폴더·주인공 여부)
-- =====================================================================
-- 새 테이블 하나를 만들고 기본 인물 5명을 넣는다. 기존 테이블·데이터는 건드리지 않는다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다 (이전 버전 서버는 이 테이블을 쓰지 않으므로 먼저 실행해도 된다).
--   2. develop -> main PR을 머지해 새 버전을 배포한다 (ddl-auto=validate라서 테이블이 없으면 시작하지 못한다).
-- 두 번 실행하면 CREATE TABLE에서 멈춘다 (데이터는 그대로).
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE characters (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '인물 ID',
    character_key  VARCHAR(50)   NOT NULL                COMMENT '일러스트 폴더 ID (Resources/Portraits/{key})',
    name           VARCHAR(50)   NOT NULL                COMMENT '대사의 speaker와 같은 이름',
    tag_color      VARCHAR(7)    NOT NULL                COMMENT '이름표 바탕색 #RRGGBB',
    protagonist    BOOLEAN       NOT NULL DEFAULT FALSE  COMMENT '주인공(대화창 왼쪽)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_characters_key (character_key),
    UNIQUE KEY uk_characters_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '대화 인물';

INSERT INTO characters (character_key, name, tag_color, protagonist) VALUES
    ('player',     '집행자', '#D4D6DE', TRUE),
    ('cassia',     '카시아', '#FF4FA3', FALSE),
    ('delta',      '델타',   '#FF9A3C', FALSE),
    ('researcher', '연구원', '#C3A6FF', FALSE),
    ('supervisor', '감독관', '#C3A6FF', FALSE);
