-- =====================================================================
-- 마이그레이션: 서버 설정 (app_settings) - 방명록 켜기/끄기
-- =====================================================================
-- 새 테이블 하나와 기본값 한 줄. 기존 데이터는 그대로다.
-- 방명록은 처음에 꺼져 있다 ('false'). 전시할 때 관리자 웹 방명록 메뉴에서 켠다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 이 테이블을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 테이블이 없으면 시작하지 않는다)
-- =====================================================================

SET NAMES utf8mb4;

-- 서버 설정 (관리자 웹에서 켜고 끄는 값). 키 하나에 값 하나
--  - guestbook.enabled: 방명록을 게임에 보일지 ('true' / 'false'). 전시할 때만 켠다
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key    VARCHAR(50)   NOT NULL                COMMENT '설정 이름',
    setting_value  VARCHAR(255)  NOT NULL                COMMENT '값',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '바꾼 시각',

    PRIMARY KEY (setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '서버 설정';

INSERT IGNORE INTO app_settings (setting_key, setting_value) VALUES ('guestbook.enabled', 'false');
