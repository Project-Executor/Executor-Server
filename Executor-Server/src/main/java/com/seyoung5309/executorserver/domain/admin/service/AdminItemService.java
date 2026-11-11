package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminItemRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminItemResponse;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 상점 아이템 관리. 삭제는 없고, 판매를 멈출 때는 isActive = false로 바꾼다. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminItemService {

    private static final Long NEW = 0L;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;

    /** 판매 중단된 아이템까지 전체 (아이템 ID 순) */
    @Transactional(readOnly = true)
    public List<AdminItemResponse> getAll() {
        return itemRepository.findAll(Sort.by("id")).stream().map(AdminItemResponse::from).toList();
    }

    /** 판매 중으로 등록한다 (isActive는 무시). @throws BusinessException DUPLICATE_VALUE */
    public AdminItemResponse create(AdminItemRequest request) {
        String name = request.name().strip();
        checkDuplicate(NEW, name);

        Item item = new Item(name, AdminText.blankToNull(request.image()),
                AdminText.blankToNull(request.description()), request.itemType(), request.price());
        saveAndFlush(item);
        return AdminItemResponse.from(item);
    }

    /**
     * 수정한다. 가격을 바꿔도 이미 산 사람에게는 영향이 없다.
     * 가진 사람이 있는 아이템은 종류(itemType)를 바꿀 수 없다 (장착 중인 스킨이 테마로 바뀌는 등의 문제를 막기 위함).
     *
     * @throws BusinessException NOT_FOUND, INVALID_INPUT(isActive 누락, 종류 변경), DUPLICATE_VALUE
     */
    public AdminItemResponse update(Long itemId, AdminItemRequest request) {
        if (request.isActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "isActive를 입력해 주세요.");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아이템을 찾을 수 없습니다."));
        String name = request.name().strip();
        checkDuplicate(itemId, name);
        if (item.getItemType() != request.itemType() && userItemRepository.existsByItemId(itemId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 가진 사용자가 있는 아이템은 종류를 바꿀 수 없습니다.");
        }

        item.update(name, AdminText.blankToNull(request.image()), AdminText.blankToNull(request.description()),
                request.itemType(), request.price(), request.isActive());
        saveAndFlush(item);
        return AdminItemResponse.from(item);
    }

    private void checkDuplicate(Long selfId, String name) {
        if (itemRepository.existsByNameAndIdNot(name, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 아이템 이름입니다.");
        }
    }

    private void saveAndFlush(Item item) {
        try {
            itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 아이템 이름입니다.");
        }
    }
}
