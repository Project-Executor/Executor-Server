package com.seyoung5309.executorserver.domain.profile.repository;

import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByNickname(String nickname);
}
