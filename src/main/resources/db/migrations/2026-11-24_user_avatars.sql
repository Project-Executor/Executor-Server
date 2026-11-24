-- =====================================================================
-- 마이그레이션: 프로필 사진 (user_avatars)
-- =====================================================================
-- 게임 프로필 창의 '사진 바꾸기'로 올린 그림을 저장한다. 기존 데이터는 그대로다.
--
-- [적용 순서]
--   1. 운영 DB에 이 파일을 실행한다. (이전 버전 서버는 이 테이블을 쓰지 않으므로 그대로 동작한다)
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
--      (새 서버는 ddl-auto=validate라 이 테이블이 없으면 시작하지 않는다)
-- =====================================================================
-- 프로필 사진 (없으면 행이 없고 게임은 기본 실루엣을 쓴다). users와 1:1이지만 그림이 커서 따로 둔다
--  - 프로필을 읽을 때마다 그림까지 읽지 않도록 분리. 게임이 256x256 PNG로 잘라 보낸다
CREATE TABLE user_avatars (
    user_id     BIGINT      NOT NULL COMMENT '사용자 ID',
    image       MEDIUMBLOB  NOT NULL COMMENT 'PNG 또는 JPEG (512KB 이하)',
    content_type VARCHAR(20) NOT NULL COMMENT 'image/png 또는 image/jpeg',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '바꾼 시각 (게임의 그림 캐시 버전)',

    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_avatars_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '프로필 사진';
