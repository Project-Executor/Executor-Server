package com.seyoung5309.executorserver.domain.guestbook.service;

import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookEntryResponse;
import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookLatestResponse;
import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookPostResponse;
import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookRequest;
import com.seyoung5309.executorserver.domain.guestbook.entity.GuestbookEntry;
import com.seyoung5309.executorserver.domain.guestbook.repository.GuestbookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.domain.setting.service.AppSettingService;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 방명록 (전시용, 로그인 없이 남긴다).
 * <ul>
 *   <li>이름 1~10자, 내용 1~100자. 욕설·개인정보는 저장하기 전에 * 로 가린다</li>
 *   <li>같은 기기(deviceId)는 5초에 한 번</li>
 *   <li>로그인한 사용자는 그날(한국 시간) 첫 글에 코인 10개</li>
 *   <li>관리자가 끄면(app_settings guestbook.enabled) 게임에서 숨고, 글을 남길 수 없다</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GuestbookService {

    public static final int NAME_MAX = 10;
    public static final int MESSAGE_MAX = 100;
    public static final int REWARD_COIN = 10;
    public static final Duration DEVICE_COOLDOWN = Duration.ofSeconds(5);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GuestbookRepository guestbookRepository;
    private final UserRepository userRepository;
    private final AppSettingService appSettingService;
    private final Clock clock;

    /** 최신 limit개 (오래된 것부터) + 오늘·전체 글 수. 꺼져 있으면 enabled=false와 빈 목록 */
    @Transactional(readOnly = true)
    public GuestbookLatestResponse latest(int limit) {
        if (!isEnabled()) {
            return new GuestbookLatestResponse(false, List.of(), 0, 0);
        }
        List<GuestbookEntryResponse> entries = new ArrayList<>(guestbookRepository.findLatest(PageRequest.of(0, limit)).stream()
                .map(GuestbookEntryResponse::from).toList());
        Collections.reverse(entries);
        return new GuestbookLatestResponse(true, entries, guestbookRepository.countByCreatedAtGreaterThanEqual(startOfToday()),
                guestbookRepository.count());
    }

    /**
     * 글을 남긴다.
     *
     * @param userId 로그인했으면 사용자 ID, 아니면 null
     * @throws BusinessException GUESTBOOK_CLOSED(관리자가 꺼 둠), INVALID_INPUT(길이·제어 문자), MESSAGE_SPAM(같은 기기 5초 안에 다시)
     */
    @Transactional
    public GuestbookPostResponse post(Long userId, GuestbookRequest request) {
        if (!isEnabled()) {
            throw new BusinessException(ErrorCode.GUESTBOOK_CLOSED);
        }
        String name = clean(request.name(), NAME_MAX, "이름");
        String message = clean(request.message(), MESSAGE_MAX, "남길 말");
        String deviceId = request.deviceId() == null || request.deviceId().isBlank() ? null : request.deviceId().strip();
        Instant now = clock.instant();
        if (deviceId != null && guestbookRepository.existsByDeviceIdAndCreatedAtGreaterThanEqual(deviceId, now.minus(DEVICE_COOLDOWN))) {
            throw new BusinessException(ErrorCode.MESSAGE_SPAM, "방명록은 5초에 한 번 남길 수 있어요. 잠시 후 다시 남겨 주세요.");
        }

        // 보상 판단은 저장 전에 (오늘 이미 남겼는지)
        boolean reward = userId != null && !guestbookRepository.existsByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday());

        GuestbookEntry entry = guestbookRepository.save(new GuestbookEntry(GuestbookFilter.mask(name), GuestbookFilter.mask(message),
                userId, deviceId, now));
        Integer totalCoin = null;
        int rewardCoin = 0;
        if (reward && userRepository.addCoin(userId, REWARD_COIN) == 1) {
            rewardCoin = REWARD_COIN;
            totalCoin = userRepository.findCoinByUserId(userId).orElse(null);
        }
        return new GuestbookPostResponse(GuestbookEntryResponse.from(entry), rewardCoin, totalCoin);
    }

    /** 관리자 목록 (최신순, 이름·내용 검색) */
    @Transactional(readOnly = true)
    public PageResponse<GuestbookEntryResponse> search(String q, int page, int size) {
        String keyword = q == null || q.isBlank() ? null : q.strip();
        return PageResponse.of(guestbookRepository.search(keyword, PageRequest.of(page, size)), GuestbookEntryResponse::from);
    }

    /** 관리자 삭제. @throws BusinessException NOT_FOUND */
    @Transactional
    public void delete(Long id) {
        GuestbookEntry entry = guestbookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "방명록 글을 찾을 수 없습니다."));
        guestbookRepository.delete(entry);
    }

    /** 방명록이 켜져 있는지 (관리자 설정) */
    public boolean isEnabled() {
        return appSettingService.isOn(AppSettingService.GUESTBOOK_ENABLED);
    }

    /** 관리자: 방명록 켜기/끄기 */
    @Transactional
    public void setEnabled(boolean enabled) {
        appSettingService.set(AppSettingService.GUESTBOOK_ENABLED, enabled);
    }

    private Instant startOfToday() {
        return LocalDate.now(clock.withZone(KST)).atStartOfDay(KST).toInstant();
    }

    /** 앞뒤 공백을 지우고 1~max자, 줄바꿈 같은 제어 문자는 공백으로 */
    static String clean(String raw, int max, String label) {
        String s = raw == null ? "" : raw.strip();
        s = s.codePoints().map(c -> Character.isISOControl(c) ? ' ' : c)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString().strip();
        int length = s.codePointCount(0, s.length());
        if (length < 1 || length > max) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, label + "은(는) 1~" + max + "자여야 합니다.");
        }
        return s;
    }
}
