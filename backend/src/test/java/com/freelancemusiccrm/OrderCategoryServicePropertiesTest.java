package com.freelancemusiccrm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

import com.freelancemusiccrm.dto.category.OrderCategoryResponseDto;
import com.freelancemusiccrm.dto.category.OrderCategoryUpsertDto;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.exception.UnprocessableEntityException;
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.service.OrderCategoryService;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class OrderCategoryServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 16: 依頼区分の CRUD ラウンドトリップ")
    void categoryCrudRoundTrip(
            @ForAll("categoryNames") String createName,
            @ForAll("categoryNames") String updateName
    ) {
        OrderCategoryRepository orderCategoryRepository = mock(OrderCategoryRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        OrderCategoryService service = new OrderCategoryService(orderCategoryRepository, taskRepository);

        List<OrderCategory> storage = new ArrayList<>();
        AtomicLong idSeq = new AtomicLong(1);

        when(orderCategoryRepository.save(any(OrderCategory.class))).thenAnswer((Answer<OrderCategory>) invocation -> {
            OrderCategory arg = invocation.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(idSeq.getAndIncrement());
                storage.add(arg);
                return arg;
            }

            Optional<OrderCategory> existing = storage.stream().filter(c -> c.getId().equals(arg.getId())).findFirst();
            existing.ifPresent(c -> c.setName(arg.getName()));
            return arg;
        });

        when(orderCategoryRepository.findById(anyLong())).thenAnswer((Answer<Optional<OrderCategory>>) invocation ->
                storage.stream().filter(c -> c.getId().equals(invocation.getArgument(0))).findFirst());

        when(orderCategoryRepository.findAllByOrderByIdAsc()).thenAnswer((Answer<List<OrderCategory>>) invocation ->
                storage.stream().sorted((a, b) -> Long.compare(a.getId(), b.getId())).toList());

        OrderCategoryResponseDto created = service.create(new OrderCategoryUpsertDto(createName));
        service.update(created.id(), new OrderCategoryUpsertDto(updateName));

        List<OrderCategoryResponseDto> all = service.findAll();

        assertThat(all.stream().anyMatch(c -> c.id().equals(created.id()) && c.name().equals(updateName))).isTrue();
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 18: 使用中区分の削除拒否")
    void rejectDeleteWhenCategoryInUse(@ForAll @net.jqwik.api.constraints.LongRange(min = 1, max = 100000) long categoryId) {
        OrderCategoryRepository orderCategoryRepository = mock(OrderCategoryRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        OrderCategoryService service = new OrderCategoryService(orderCategoryRepository, taskRepository);

        when(orderCategoryRepository.existsById(categoryId)).thenReturn(true);
        when(taskRepository.existsByOrderCategoryId(categoryId)).thenReturn(true);

        UnprocessableEntityException ex = assertThrows(UnprocessableEntityException.class, () -> service.delete(categoryId));

        assertThat(ex.getMessage()).contains("使用中の区分は削除できません");
        verify(orderCategoryRepository, never()).deleteById(anyLong());
    }

    @Provide
    Arbitrary<String> categoryNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20);
    }
}
