-- =====================================================================
-- 마이그레이션: 타자 해킹 단어 목록 (감염된 컴퓨터 computer 오브젝트용)
-- =====================================================================
-- 새 테이블 두 개만 만든다. 기존 테이블·데이터는 건드리지 않는다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다 (이전 버전 서버는 이 테이블을 쓰지 않으므로 먼저 실행해도 된다).
--   2. develop -> main PR을 머지해 새 버전을 배포한다 (ddl-auto=validate라서 테이블이 없으면 시작하지 못한다).
-- 두 번 실행하면 CREATE TABLE에서 멈춘다 (데이터는 그대로).
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE word_lists (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '단어 목록 ID',
    name         VARCHAR(100)   NOT NULL                COMMENT '목록 이름',
    description  VARCHAR(255)       NULL                COMMENT '설명',

    PRIMARY KEY (id),
    UNIQUE KEY uk_word_lists_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '타자 해킹 단어 목록';

CREATE TABLE word_list_words (
    word_list_id BIGINT         NOT NULL COMMENT '단어 목록 ID',
    position     INT            NOT NULL COMMENT '목록 안 순서 (0부터)',
    word         VARCHAR(50)    NOT NULL COMMENT '단어',

    PRIMARY KEY (word_list_id, position),
    CONSTRAINT fk_word_list_words_list
        FOREIGN KEY (word_list_id) REFERENCES word_lists (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '단어 목록의 단어';
