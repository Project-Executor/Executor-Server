-- =====================================================================
-- 마이그레이션: 집행자 이름(character_name) 삭제, 닉네임으로 통일
-- =====================================================================
-- 회원가입은 이메일·비밀번호만 받고, 닉네임은 첫 로그인 후 튜토리얼에서 정한다.
-- 그래서 users.nickname은 가입 직후 NULL일 수 있다 (UNIQUE는 NULL끼리 겹쳐도 된다).
-- 대사의 {characterName}은 {nickname}으로 바뀌었다 (docs/map-data.md).
--
-- [적용 순서] 이 SQL을 먼저 실행하면 이전 버전 서버는 character_name 컬럼을 찾지 못해 오류가 난다.
--   1. 운영 DB에 이 파일을 실행한다.
--   2. 바로 develop -> main PR을 머지해 새 버전을 배포한다.
-- 두 번 실행하면 첫 문장에서 "character_name이 없음" 오류로 멈춘다.
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE users
    DROP COLUMN character_name,
    MODIFY COLUMN nickname VARCHAR(20) NULL COMMENT '닉네임 (중복 불가, 가입 후 튜토리얼에서 입력, 입력 전 NULL)';
