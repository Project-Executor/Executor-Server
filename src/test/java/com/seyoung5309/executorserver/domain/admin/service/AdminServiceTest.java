package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.*;
import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.repository.ItemRepository;
import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static com.seyoung5309.executorserver.domain.stage.StageFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Nested
    class 작전 {
        private OperationRepository operationRepository;
        private StageRepository stageRepository;
        private AdminOperationService service;

        @BeforeEach
        void setUp() {
            operationRepository = mock(OperationRepository.class);
            stageRepository = mock(StageRepository.class);
            service = new AdminOperationService(operationRepository, stageRepository);
        }

        @Test
        void 이름_앞뒤_공백을_지우고_빈_설명은_null로_저장한다() {
            AdminOperationResponse response = service.create(new AdminOperationRequest("  1장  ", "  ", 1));

            assertThat(response.name()).isEqualTo("1장");
            assertThat(response.description()).isNull();
            verify(operationRepository).existsByNameAndIdNot("1장", 0L);
        }

        @Test
        void 이름이나_순서가_겹치면_DUPLICATE_VALUE() {
            when(operationRepository.existsBySortOrderAndIdNot(0, 0L)).thenReturn(true);

            assertErrorCode(() -> service.create(new AdminOperationRequest("새 작전", null, 0)), ErrorCode.DUPLICATE_VALUE);
            verify(operationRepository, never()).saveAndFlush(any());
        }

        @Test
        void 수정할_때는_자기_자신과는_겹쳐도_된다() {
            Operation operation = operation(5L, "1장", 1);
            when(operationRepository.findById(5L)).thenReturn(Optional.of(operation));

            service.update(5L, new AdminOperationRequest("1장", "설명", 1));

            verify(operationRepository).existsByNameAndIdNot("1장", 5L);
            verify(operationRepository).existsBySortOrderAndIdNot(1, 5L);
            assertThat(operation.getDescription()).isEqualTo("설명");
        }

        @Test
        void 저장_중_UNIQUE_위반이면_DUPLICATE_VALUE() {
            when(operationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

            assertErrorCode(() -> service.create(new AdminOperationRequest("새 작전", null, 3)), ErrorCode.DUPLICATE_VALUE);
        }

        @Test
        void 스테이지가_있는_작전은_삭제할_수_없다() {
            when(operationRepository.findById(5L)).thenReturn(Optional.of(operation(5L, "1장", 1)));
            when(stageRepository.existsByOperationId(5L)).thenReturn(true);

            assertErrorCode(() -> service.delete(5L), ErrorCode.OPERATION_HAS_STAGES);
            verify(operationRepository, never()).delete(any());
        }

        @Test
        void 없는_작전은_NOT_FOUND() {
            assertErrorCode(() -> service.delete(99L), ErrorCode.NOT_FOUND);
        }
    }

    @Nested
    class 스테이지 {
        private StageRepository stageRepository;
        private OperationRepository operationRepository;
        private AdminStageService service;
        private final Operation op = operation(1L, "1장", 1);

        @BeforeEach
        void setUp() {
            stageRepository = mock(StageRepository.class);
            operationRepository = mock(OperationRepository.class);
            service = new AdminStageService(stageRepository, operationRepository, objectMapper);
            when(operationRepository.findById(1L)).thenReturn(Optional.of(op));
        }

        @Test
        void 등록하면_공개된_공식_스테이지가_되고_힌트_개수가_나온다() {
            AdminStageResponse response = service.create(request("{\"w\":3,\"hints\":[\"a\",\"b\"]}"));

            assertThat(response.hintCount()).isEqualTo(2);
            assertThat(response.mapData().get("w").asInt()).isEqualTo(3);
            assertThat(response.operationId()).isEqualTo(1L);
            verify(stageRepository).saveAndFlush(argThat(s -> s.isPublic() && s.isOfficial()));
        }

        @Test
        void mapData가_객체가_아니거나_hints가_배열이_아니면_INVALID_INPUT() {
            assertErrorCode(() -> service.create(request("[1,2]")), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> service.create(request("\"text\"")), ErrorCode.INVALID_INPUT);
            assertErrorCode(() -> service.create(request("{\"hints\":\"a\"}")), ErrorCode.INVALID_INPUT);
            verify(stageRepository, never()).saveAndFlush(any());
        }

        @Test
        void 같은_작전에_같은_번호가_있으면_DUPLICATE_VALUE() {
            when(stageRepository.existsByOperationIdAndStageNoAndIdNot(1L, 1, 0L)).thenReturn(true);

            assertErrorCode(() -> service.create(request("{}")), ErrorCode.DUPLICATE_VALUE);
        }

        @Test
        void 없는_작전이면_NOT_FOUND() {
            AdminStageRequest request = new AdminStageRequest(99L, 1, StageType.NORMAL, "s", json("{}"), 10, 0);

            assertErrorCode(() -> service.create(request), ErrorCode.NOT_FOUND);
        }

        @Test
        void 사용자_맵은_관리자_API로_수정하거나_삭제할_수_없다() {
            Stage userMap = userMap(30L, user(7L), false);
            when(stageRepository.findById(30L)).thenReturn(Optional.of(userMap));

            assertErrorCode(() -> service.update(30L, request("{}")), ErrorCode.NOT_FOUND);
            assertErrorCode(() -> service.delete(30L), ErrorCode.NOT_FOUND);
            verify(stageRepository, never()).delete(any());
        }

        @Test
        void 수정하면_내용이_바뀌고_중복_검사에서_자기_자신은_제외한다() {
            Stage stage = withMapData(officialStage(20L, op, 1), "{}");
            when(stageRepository.findById(20L)).thenReturn(Optional.of(stage));

            service.update(20L, new AdminStageRequest(1L, 2, StageType.BOSS, " 보스 ", json("{\"hints\":[]}"), 15, 300));

            verify(stageRepository).existsByOperationIdAndStageNoAndIdNot(1L, 2, 20L);
            assertThat(stage.getStageNo()).isEqualTo(2);
            assertThat(stage.getStageType()).isEqualTo(StageType.BOSS);
            assertThat(stage.getTitle()).isEqualTo("보스");
            assertThat(stage.getRewardCoin()).isEqualTo(300);
        }

        private AdminStageRequest request(String mapData) {
            return new AdminStageRequest(1L, 1, StageType.NORMAL, "1-1", json(mapData), 10, 50);
        }
    }

    @Nested
    class 도감 {
        private GuidebookRepository guidebookRepository;
        private StageRepository stageRepository;
        private AdminGuidebookService service;

        @BeforeEach
        void setUp() {
            guidebookRepository = mock(GuidebookRepository.class);
            stageRepository = mock(StageRepository.class);
            service = new AdminGuidebookService(guidebookRepository, stageRepository);
        }

        @Test
        void 처음_등장_스테이지_없이_등록할_수_있다() {
            AdminGuidebookResponse response = service.create(
                    new AdminGuidebookRequest(null, GuidebookCategory.TERM, " API ", "", "설명"));

            assertThat(response.stageId()).isNull();
            assertThat(response.title()).isEqualTo("API");
            assertThat(response.image()).isNull();
            verify(stageRepository, never()).findById(anyLong());
        }

        @Test
        void 없는_스테이지를_가리키면_NOT_FOUND() {
            assertErrorCode(() -> service.create(new AdminGuidebookRequest(99L, GuidebookCategory.BUG, "NPE", null, "설명")),
                    ErrorCode.NOT_FOUND);
        }

        @Test
        void 이름이_겹치면_DUPLICATE_VALUE() {
            when(guidebookRepository.existsByTitleAndIdNot("NPE", 0L)).thenReturn(true);

            assertErrorCode(() -> service.create(new AdminGuidebookRequest(null, GuidebookCategory.BUG, "NPE", null, "설명")),
                    ErrorCode.DUPLICATE_VALUE);
        }

        @Test
        void 없는_도감_삭제는_NOT_FOUND() {
            assertErrorCode(() -> service.delete(99L), ErrorCode.NOT_FOUND);
            verify(guidebookRepository, never()).delete(any(Guidebook.class));
        }
    }

    @Nested
    class 아이템 {
        private ItemRepository itemRepository;
        private UserItemRepository userItemRepository;
        private AdminItemService service;

        @BeforeEach
        void setUp() {
            itemRepository = mock(ItemRepository.class);
            userItemRepository = mock(UserItemRepository.class);
            service = new AdminItemService(itemRepository, userItemRepository);
        }

        @Test
        void 등록하면_isActive와_상관없이_판매_중이다() {
            AdminItemResponse response = service.create(
                    new AdminItemRequest("다크 스킨", null, null, ItemType.SKIN, 300, false));

            assertThat(response.isActive()).isTrue();
        }

        @Test
        void 수정할_때_isActive가_없으면_INVALID_INPUT() {
            assertErrorCode(() -> service.update(3L, new AdminItemRequest("a", null, null, ItemType.SKIN, 1, null)),
                    ErrorCode.INVALID_INPUT);
        }

        @Test
        void 판매를_중단할_수_있다() {
            Item item = new Item("다크 스킨", null, null, ItemType.SKIN, 300);
            ReflectionTestUtils.setField(item, "id", 3L);
            when(itemRepository.findById(3L)).thenReturn(Optional.of(item));

            AdminItemResponse response = service.update(3L,
                    new AdminItemRequest("다크 스킨", null, null, ItemType.SKIN, 500, false));

            assertThat(response.isActive()).isFalse();
            assertThat(response.price()).isEqualTo(500);
        }

        @Test
        void 가진_사람이_있으면_종류를_바꿀_수_없고_없으면_바꿀_수_있다() {
            Item item = new Item("다크 스킨", null, null, ItemType.SKIN, 300);
            ReflectionTestUtils.setField(item, "id", 3L);
            when(itemRepository.findById(3L)).thenReturn(Optional.of(item));
            AdminItemRequest toTheme = new AdminItemRequest("다크 스킨", null, null, ItemType.THEME, 300, true);

            when(userItemRepository.existsByItemId(3L)).thenReturn(true);
            assertErrorCode(() -> service.update(3L, toTheme), ErrorCode.INVALID_INPUT);

            when(userItemRepository.existsByItemId(3L)).thenReturn(false);
            assertThat(service.update(3L, toTheme).itemType()).isEqualTo(ItemType.THEME);
        }

        @Test
        void 없는_아이템은_NOT_FOUND() {
            assertErrorCode(() -> service.update(99L, new AdminItemRequest("a", null, null, ItemType.HINT, 1, true)),
                    ErrorCode.NOT_FOUND);
        }
    }

    private tools.jackson.databind.JsonNode json(String value) {
        return objectMapper.readTree(value);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
