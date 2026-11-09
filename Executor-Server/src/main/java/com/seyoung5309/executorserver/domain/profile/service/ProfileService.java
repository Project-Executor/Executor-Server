package com.seyoung5309.executorserver.domain.profile.service;

import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;

    public boolean isNicknameAvailable(String nickname) {
        return !profileRepository.existsByNickname(nickname.trim());
    }
}
