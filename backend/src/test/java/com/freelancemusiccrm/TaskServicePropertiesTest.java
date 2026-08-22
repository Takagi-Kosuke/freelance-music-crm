package com.freelancemusiccrm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

import com.freelancemusiccrm.dto.task.TaskResponseDto;
import com.freelancemusiccrm.dto.task.TaskStatusUpdateDto;
import com.freelancemusiccrm.entity.Order;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.OrderStatus;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TaskStatus;
import com.freelancemusiccrm.entity.TokenStatus;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.service.DiscordNotifierService;
import com.freelancemusiccrm.service.TaskService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

class TaskServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 13: Task ステータス変更の正確性と更新日時記録")
    void updateTaskStatus(
            @ForAll @LongRange(min = 1, max = 100000) long taskId,
            @ForAll TaskStatus status
    ) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        Task task = buildTask(taskId, 1L, 1L, LocalDate.now().plusDays(10));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer((Answer<Task>) invocation -> invocation.getArgument(0));

        TaskStatusUpdateDto request = new TaskStatusUpdateDto(status);
        TaskResponseDto result = service.updateStatus(taskId, request);

        assertThat(result.status()).isEqualTo(status);
        assertThat(result.statusUpdatedAt()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(status);
        assertThat(task.getStatusUpdatedAt()).isNotNull();
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 14: 依頼区分フィルタリングの正確性")
    void filterTasksByCategory(
            @ForAll @LongRange(min = 1, max = 100000) long categoryId,
            @ForAll @IntRange(min = 1, max = 20) int count
    ) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        List<Task> tasks = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> buildTask(i + 1L, i + 100L, categoryId, LocalDate.now().plusDays(10)))
                .toList();

        when(taskRepository.findByOrderCategoryId(categoryId)).thenReturn(tasks);

        List<TaskResponseDto> result = service.findAll(categoryId);

        assertThat(result).hasSize(count);
        assertThat(result.stream().allMatch(t -> t.categoryId().equals(categoryId))).isTrue();
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 15: カレンダー期間クエリの正確性")
    void calendarRangeQuery(
            @ForAll @IntRange(min = 1, max = 30) int offsetStart,
            @ForAll @IntRange(min = 31, max = 60) int offsetEnd,
            @ForAll @IntRange(min = 1, max = 20) int count
    ) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        LocalDate start = LocalDate.now().plusDays(offsetStart);
        LocalDate end = LocalDate.now().plusDays(offsetEnd);

        List<Task> tasks = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    LocalDate due = start.plusDays(i % Math.max(1, end.toEpochDay() - start.toEpochDay() == 0 ? 1 : (int) (end.toEpochDay() - start.toEpochDay())));
                    return buildTask(i + 1L, i + 100L, 1L, due);
                })
                .toList();

        when(taskRepository.findByOrderDesiredDeliveryDateBetween(start, end)).thenReturn(tasks);

        List<TaskResponseDto> result = service.findCalendarTasks(start, end);

        assertThat(result).hasSize(count);
        assertThat(result.stream().allMatch(t -> !t.desiredDeliveryDate().isBefore(start) && !t.desiredDeliveryDate().isAfter(end))).isTrue();
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 13: Task ステータス変更の正確性と更新日時記録")
    void updateMissingTaskThrows(@ForAll @LongRange(min = 1, max = 100000) long taskId) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updateStatus(taskId, new TaskStatusUpdateDto(TaskStatus.IN_PROGRESS)));
        assertThat(ex.getMessage()).contains("タスクが見つかりません");
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property: Task フォルダパス更新の正確性")
    void updateTaskFolderPath(
            @ForAll @LongRange(min = 1, max = 100000) long taskId,
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String segment
    ) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        Task task = buildTask(taskId, 1L, 1L, LocalDate.now().plusDays(10));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer((Answer<Task>) invocation -> invocation.getArgument(0));

        String folderPath = "E:/workspace/" + segment;
        TaskResponseDto result = service.updateFolderPath(taskId, new com.freelancemusiccrm.dto.task.TaskFolderPathUpdateDto(folderPath));

        assertThat(result.folderPath()).isEqualTo(folderPath);
        assertThat(task.getFolderPath()).isEqualTo(folderPath);
    }

    private Task buildTask(Long taskId, Long orderId, Long categoryId, LocalDate desiredDate) {
        OrderCategory category = new OrderCategory();
        category.setId(categoryId);
        category.setName("カテゴリ" + categoryId);

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(orderId + 1000);
        quoteRequest.setSubject("件名");
        quoteRequest.setClientName("依頼者");
        quoteRequest.setClientEmail("client@example.com");
        quoteRequest.setCategory(category);
        quoteRequest.setDesiredDeliveryDate(desiredDate);
        quoteRequest.setStatus(QuoteRequestStatus.RESPONDED);

        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setId(orderId + 2000);
        quoteResponse.setQuoteRequest(quoteRequest);
        quoteResponse.setAmount(BigDecimal.valueOf(1000));
        quoteResponse.setResponseDeliveryDate(desiredDate);
        quoteResponse.setApprovalToken("token-" + orderId);
        quoteResponse.setTokenStatus(TokenStatus.USED);
        quoteResponse.setCreatedAt(LocalDateTime.now());

        Order order = new Order();
        order.setId(orderId);
        order.setQuoteResponse(quoteResponse);
        order.setSubject("件名");
        order.setClientName("依頼者");
        order.setClientEmail("client@example.com");
        order.setCategory(category);
        order.setDesiredDeliveryDate(desiredDate);
        order.setStatus(OrderStatus.RECEIVED);
        order.setCreatedAt(LocalDateTime.now());

        Task task = new Task();
        task.setId(taskId);
        task.setOrder(order);
        task.setStatus(TaskStatus.NOT_STARTED);
        task.setStatusUpdatedAt(LocalDateTime.now());
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }
}
