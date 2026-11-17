-- =====================================================================
-- 마이그레이션: 공지사항 테이블 (notices)
-- =====================================================================
-- 관리자 웹에서 공지를 작성하고, 게임은 GET /api/notices로 읽는다.
--   보이는 공지: is_visible = TRUE, published_at <= 지금, (ends_at이 NULL이거나 지금보다 뒤)
--   정렬: 상단 고정(is_pinned) 먼저, 그다음 게시 시각 최신순
--
-- [적용 순서] 새 테이블만 만들므로 이전 버전 서버도 그대로 동작한다.
--   1. 운영 DB에 이 파일을 실행한다.
--   2. develop -> main PR을 머지해 새 버전을 배포한다.
-- 순서를 반대로 하면 새 서버가 시작할 때 테이블이 없다는 오류(ddl-auto=validate)로 뜨지 않는다.
-- 두 번 실행해도 IF NOT EXISTS라 아무것도 바뀌지 않는다.
-- =====================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS notices (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '공지 ID',
    category      ENUM('NOTICE', 'UPDATE', 'EVENT', 'MAINTENANCE') NOT NULL DEFAULT 'NOTICE' COMMENT '분류 (공지·업데이트·이벤트·점검)',
    title         VARCHAR(100)   NOT NULL                COMMENT '제목',
    content       TEXT           NOT NULL                COMMENT '내용 (5000자 이하)',
    is_pinned     BOOLEAN        NOT NULL DEFAULT FALSE  COMMENT '상단 고정',
    is_visible    BOOLEAN        NOT NULL DEFAULT TRUE   COMMENT 'FALSE면 숨김 (게임에 안 보임)',
    published_at  DATETIME       NOT NULL                COMMENT '게시 시각 (미래면 예약, 그때부터 보임)',
    ends_at       DATETIME           NULL                COMMENT '게시 종료 시각 (NULL이면 계속)',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일',
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    PRIMARY KEY (id),
    KEY idx_notices_list (is_visible, is_pinned, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '공지사항';
