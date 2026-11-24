package com.seyoung5309.executorserver.domain.profile.repository;

import com.seyoung5309.executorserver.domain.profile.entity.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {

    /** 사진을 바꾼 시각만 (그림은 읽지 않는다). 사진이 없으면 empty */
    @Query("select a.updatedAt from UserAvatar a where a.userId = :userId")
    Optional<Instant> findUpdatedAt(@Param("userId") Long userId);
}
