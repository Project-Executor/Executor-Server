package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminMailRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminMailResponse;
import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.mail.repository.MailRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 관리자 우편 발송. 받는 사람 수와 상관없이 INSERT ... SELECT 한 문장으로 넣는다. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminMailService {

    private final MailRepository mailRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * @throws BusinessException INVALID_INPUT(받는 사람 없음, 첨부 수량, 만료 시각), NOT_FOUND(없는 사용자 또는 아이템)
     */
    public AdminMailResponse send(AdminMailRequest request) {
        Instant now = clock.instant();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "expiresAt은 지금보다 뒤여야 합니다.");
        }
        validateRewardItem(request.rewardItemId(), request.rewardItemQuantityOrZero());

        String title = request.title().strip();
        String content = AdminText.blankToNull(request.content());

        if (Boolean.TRUE.equals(request.toAll())) {
            return new AdminMailResponse(mailRepository.sendToAll(title, content, request.rewardCoinOrZero(),
                    request.rewardItemId(), request.rewardItemQuantityOrZero(), request.expiresAt(), now));
        }

        Set<Long> receiverIds = request.receiverIds() == null ? Set.of() : new LinkedHashSet<>(request.receiverIds());
        if (receiverIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "receiverIds를 입력하거나 toAll을 true로 보내 주세요.");
        }
        Set<Long> existing = userRepository.findAllById(receiverIds).stream().map(User::getId).collect(Collectors.toSet());
        List<Long> missing = receiverIds.stream().filter(id -> !existing.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "없는 사용자입니다: " + missing);
        }

        return new AdminMailResponse(mailRepository.sendTo(receiverIds, title, content, request.rewardCoinOrZero(),
                request.rewardItemId(), request.rewardItemQuantityOrZero(), request.expiresAt(), now));
    }

    /** 첨부가 없으면 수량 0, 있으면 스킨·테마는 1개, 힌트는 1~99개 (99 상한은 요청 검증에서) */
    private void validateRewardItem(Long itemId, int quantity) {
        if (itemId == null) {
            if (quantity != 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "첨부 아이템이 없으면 rewardItemQuantity는 0이어야 합니다.");
            }
            return;
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아이템을 찾을 수 없습니다."));
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "첨부 아이템이 있으면 rewardItemQuantity는 1 이상이어야 합니다.");
        }
        if (item.getItemType() != ItemType.HINT && quantity != 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "스킨과 테마는 1개만 첨부할 수 있습니다.");
        }
    }
}
