-- =====================================================================
-- 마이그레이션: 테이블 이름 변경 users -> accounts, profiles -> users
-- =====================================================================
-- 로그인 계정(이메일·비밀번호·권한)은 accounts, 게임 사용자 정보(닉네임·코인·장착)는 users로 이름을 맞춘다.
-- Java 엔티티도 User -> Account, Profile -> User로 바뀌었다.
--
-- [적용 순서] 이 SQL을 실행하는 순간부터 이전 버전 서버는 profiles 테이블을 찾지 못해 오류가 난다.
--   1. 운영 DB에 이 파일을 실행한다.
--   2. 바로 develop -> main PR을 머지해 새 버전을 배포한다 (빌드·기동 약 1~2분 동안 API 오류).
--   새 버전 서버를 먼저 배포하면 ddl-auto=validate에서 기동이 실패한다 (이전 서버는 그대로 유지됨).
--
-- [안전장치] 두 번 실행하면 첫 문장에서 "accounts가 이미 있음" 오류로 멈춘다.
--   mysql 클라이언트는 오류가 나면 나머지 문장을 실행하지 않는다 (--force 옵션을 쓰지 말 것).
-- 데이터는 그대로 옮겨지고, 다른 테이블의 외래 키(user_id 등)는 MySQL이 새 이름을 자동으로 따라간다.
-- =====================================================================

SET NAMES utf8mb4;

-- 1) 테이블 이름. 한 문장이라 두 이름이 동시에 바뀐다
RENAME TABLE users TO accounts, profiles TO users;

-- 2) accounts: 인덱스 이름만 바꾼다
ALTER TABLE accounts
    RENAME INDEX uk_users_email TO uk_accounts_email,
    COMMENT = '로그인 계정';

-- 3) users (이전 profiles): 외래 키와 CHECK는 이름을 바꿀 수 없어서 지우고 같은 내용으로 다시 만든다
ALTER TABLE users
    DROP FOREIGN KEY fk_profiles_user,
    DROP FOREIGN KEY fk_profiles_skin,
    DROP FOREIGN KEY fk_profiles_theme,
    DROP CHECK chk_profiles_coin;

ALTER TABLE users
    RENAME INDEX uk_profiles_nickname TO uk_users_nickname,
    RENAME INDEX fk_profiles_skin TO fk_users_skin,
    RENAME INDEX fk_profiles_theme TO fk_users_theme,
    ADD CONSTRAINT chk_users_coin CHECK (coin >= 0),
    ADD CONSTRAINT fk_users_account
        FOREIGN KEY (user_id) REFERENCES accounts (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_users_skin
        FOREIGN KEY (equipped_skin_id) REFERENCES items (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_users_theme
        FOREIGN KEY (equipped_theme_id) REFERENCES items (id) ON DELETE SET NULL,
    COMMENT = '사용자';
