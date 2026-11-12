package com.seyoung5309.executorserver.domain.mail.service;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.mail.dto.ClaimAllResponse;
import com.seyoung5309.executorserver.domain.mail.dto.ClaimResponse;
import com.seyoung5309.executorserver.domain.mail.dto.MailResponse;
import com.seyoung5309.executorserver.domain.mail.entity.Mail;
import com.seyoung5309.executorserver.domain.mail.repository.MailRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 우편 조회와 수령.
 * <p>
 * 수령은 "아직 받지 않았고 만료되지 않음" 조건부 UPDATE가 1행을 바꿨을 때만 보상을 준다. 그래서 동시에 여러 번 받아도 한 번만 지급된다.
 * 잠금 순서는 우편 -> 프로필(코인) -> 보유 아이템으로, 상점 구매(프로필 -> 보유 아이템)와 같은 방향이라 교착 상태가 생기지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MailService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final MailRepository mailRepository;
    private final ProfileRepository profileRepository;
    private final UserItemRepository userItemRepository;
    private final Clock clock;

    /** 만료되지 않은 내 우편 (최신순). 받은 우편도 claimed = true로 남아 있다 */
    @Transactional(readOnly = true)
    public PageResponse<MailResponse> getMails(Long userId, int page, int size) {
        return PageResponse.of(
                mailRepository.findVisible(userId, clock.instant(), PageRequest.of(page, size, LATEST_FIRST)),
                MailResponse::from);
    }

    /**
     * 우편 한 통의 보상을 받는다.
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN(내 우편이 아님), ALREADY_CLAIMED, MAIL_EXPIRED
     */
    public ClaimResponse claim(Long userId, Long mailId) {
        Mail mail = mailRepository.findWithItemById(mailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "우편을 찾을 수 없습니다."));
        if (!mail.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내 우편이 아닙니다.");
        }
        Instant now = clock.instant();
        if (mailRepository.markClaimed(mailId, userId, now) == 0) {
            throw whyNotClaimable(mail, now);
        }

        Grant grant = grant(userId, mail, now);
        return new ClaimResponse(mailId, mail.getRewardCoin(), grant.convertedCoin(), grant.item(), currentCoin(userId));
    }

    /**
     * 받을 수 있는 우편을 모두 받는다 (오래된 우편부터). 받을 게 없으면 claimedCount = 0.
     * 같은 스킨이 여러 우편에 있으면 첫 우편에서 아이템을 받고, 나머지는 코인으로 바뀐다.
     */
    public ClaimAllResponse claimAll(Long userId) {
        Instant now = clock.instant();
        List<Mail> mails = mailRepository.findClaimable(userId, now);

        int claimedCount = 0;
        int rewardCoin = 0;
        int convertedCoin = 0;
        Map<Long, Integer> items = new TreeMap<>();
        for (Mail mail : mails) {
            if (mailRepository.markClaimed(mail.getId(), userId, now) == 0) {
                continue; // 그 사이 다른 요청이 받음
            }
            Grant grant = grant(userId, mail, now);
            claimedCount++;
            rewardCoin += mail.getRewardCoin();
            convertedCoin += grant.convertedCoin();
            if (grant.item() != null) {
                items.merge(grant.item().itemId(), grant.item().quantity(), Integer::sum);
            }
        }

        List<ClaimAllResponse.ItemQuantity> rewardItems = items.entrySet().stream()
                .map(entry -> new ClaimAllResponse.ItemQuantity(entry.getKey(), entry.getValue()))
                .toList();
        return new ClaimAllResponse(claimedCount, rewardCoin, convertedCoin, rewardItems, currentCoin(userId));
    }

    /**
     * 보상을 지급한다. 코인을 먼저 더해서(프로필 행 잠금) 상점 구매와 잠금 순서를 맞춘다.
     * 이미 가진 스킨·테마는 아이템 가격 × 수량만큼 코인으로 바꾼다.
     */
    private Grant grant(Long userId, Mail mail, Instant now) {
        profileRepository.addCoin(userId, mail.getRewardCoin());

        Item item = mail.getRewardItem();
        if (item == null || mail.getRewardItemQuantity() <= 0) {
            return new Grant(0, null);
        }
        int quantity = mail.getRewardItemQuantity();
        if (item.getItemType() == ItemType.HINT) {
            userItemRepository.addQuantity(userId, item.getId(), quantity, now);
            return new Grant(0, new ClaimResponse.GrantedItem(item.getId(), item.getItemType(), quantity));
        }
        if (userItemRepository.insertIfAbsent(userId, item.getId(), now) == 1) {
            return new Grant(0, new ClaimResponse.GrantedItem(item.getId(), item.getItemType(), 1));
        }
        int converted = Math.toIntExact(Math.min((long) item.getPrice() * quantity, Integer.MAX_VALUE));
        profileRepository.addCoin(userId, converted);
        return new Grant(converted, null);
    }

    private BusinessException whyNotClaimable(Mail mail, Instant now) {
        if (mail.getClaimedAt() == null && mail.getExpiresAt() != null && !mail.getExpiresAt().isAfter(now)) {
            return new BusinessException(ErrorCode.MAIL_EXPIRED);
        }
        return new BusinessException(ErrorCode.ALREADY_CLAIMED); // 이미 받았거나, 방금 다른 요청이 받음
    }

    private int currentCoin(Long userId) {
        return profileRepository.findCoinByUserId(userId).orElse(0);
    }

    private record Grant(int convertedCoin, ClaimResponse.GrantedItem item) {
    }
}
