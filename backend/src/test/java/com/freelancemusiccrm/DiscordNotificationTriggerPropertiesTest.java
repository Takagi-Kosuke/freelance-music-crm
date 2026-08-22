package com.freelancemusiccrm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

import com.freelancemusiccrm.dto.quote.QuoteRequestCreateDto;
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
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.OrderRepository;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.repository.QuoteResponseRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.service.DiscordNotifierService;
import com.freelancemusiccrm.service.OrderService;
import com.freelancemusiccrm.service.QuoteRequestService;
import com.freelancemusiccrm.service.TaskService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

@SuppressWarnings("null")
class DiscordNotificationTriggerPropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 22: Discord 通知のトリガー確認")
    void quoteRequestCreationTriggersNotifier(@ForAll @LongRange(min = 1, max = 365) long deliveryDays) {
        QuoteRequestRepository quoteRequestRepository = mock(QuoteRequestRepository.class);
        OrderCategoryRepository orderCategoryRepository = mock(OrderCategoryRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);

        QuoteRequestService service = new QuoteRequestService(
                quoteRequestRepository,
                orderCategoryRepository,
                discordNotifierService
        );

        OrderCategory category = new OrderCategory();
        category.setId(1L);
        category.setName("作曲");
        when(orderCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(quoteRequestRepository.save(any(QuoteRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new QuoteRequestCreateDto(
                "件名",
                "依頼者",
                "client@example.com",
                1L,
                LocalDate.now().plusDays(deliveryDays),
                "https://example.com/file",
                "comment"
        ));

        verify(discordNotifierService, times(1)).notifyQuoteRequestCreated(any(QuoteRequest.class));
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 22: Discord 通知のトリガー確認")
    void orderApprovalTriggersNotifier(@ForAll @LongRange(min = 1, max = 100000) long quoteRequestId) {
        QuoteResponseRepository quoteResponseRepository = mock(QuoteResponseRepository.class);
        QuoteRequestRepository quoteRequestRepository = mock(QuoteRequestRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        OrderService service = new OrderService(
                quoteResponseRepository,
                quoteRequestRepository,
                orderRepository,
                taskRepository,
                discordNotifierService
        );

        String token = "token-" + quoteRequestId;
        QuoteResponse quoteResponse = buildActiveQuoteResponse(quoteRequestId);
        when(quoteResponseRepository.findByApprovalToken(token)).thenReturn(Optional.of(quoteResponse));
        when(orderRepository.findByQuoteResponseId(anyLong())).thenReturn(Optional.empty());
        when(quoteRequestRepository.save(any(QuoteRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteResponseRepository.save(any(QuoteResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicLong idGenerator = new AtomicLong(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer((Answer<Order>) invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(idGenerator.getAndIncrement());
            return saved;
        });
        when(taskRepository.save(any(Task.class))).thenAnswer((Answer<Task>) invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(idGenerator.getAndIncrement());
            return saved;
        });

        service.approveByToken(token);

        verify(discordNotifierService, times(1)).notifyOrderCreated(any(Order.class));
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 22: Discord 通知のトリガー確認")
    void taskCompletionTriggersNotifier(@ForAll @LongRange(min = 1, max = 100000) long taskId) {
        TaskRepository taskRepository = mock(TaskRepository.class);
        DiscordNotifierService discordNotifierService = mock(DiscordNotifierService.class);
        TaskService service = new TaskService(taskRepository, discordNotifierService);

        Task task = buildTask(taskId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(taskId, new TaskStatusUpdateDto(TaskStatus.COMPLETED));

            verify(discordNotifierService, times(1)).notifyTaskCompleted(anyString());
    }

    private QuoteResponse buildActiveQuoteResponse(long quoteRequestId) {
        OrderCategory category = new OrderCategory();
        category.setId(1L);
        category.setName("作曲");

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(quoteRequestId);
        quoteRequest.setSubject("件名");
        quoteRequest.setClientName("依頼者");
        quoteRequest.setClientEmail("client@example.com");
        quoteRequest.setCategory(category);
        quoteRequest.setDesiredDeliveryDate(LocalDate.now().plusDays(10));
        quoteRequest.setComment("comment");
        quoteRequest.setStatus(QuoteRequestStatus.RESPONDED);

        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setId(quoteRequestId + 1000);
        quoteResponse.setQuoteRequest(quoteRequest);
        quoteResponse.setAmount(BigDecimal.valueOf(12000));
        quoteResponse.setResponseDeliveryDate(LocalDate.now().plusDays(14));
        quoteResponse.setResponseComment("response");
        quoteResponse.setApprovalToken("token-" + quoteRequestId);
        quoteResponse.setTokenStatus(TokenStatus.ACTIVE);
        quoteResponse.setCreatedAt(LocalDateTime.now());
        return quoteResponse;
    }

    private Task buildTask(long taskId) {
        OrderCategory category = new OrderCategory();
        category.setId(1L);
        category.setName("作曲");

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(10L);
        quoteRequest.setSubject("件名");
        quoteRequest.setClientName("依頼者");
        quoteRequest.setClientEmail("client@example.com");
        quoteRequest.setCategory(category);
        quoteRequest.setDesiredDeliveryDate(LocalDate.now().plusDays(7));

        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setId(20L);
        quoteResponse.setQuoteRequest(quoteRequest);
        quoteResponse.setAmount(BigDecimal.valueOf(10000));
        quoteResponse.setResponseDeliveryDate(LocalDate.now().plusDays(10));
        quoteResponse.setApprovalToken("token");
        quoteResponse.setTokenStatus(TokenStatus.USED);

        Order order = new Order();
        order.setId(30L);
        order.setQuoteResponse(quoteResponse);
        order.setSubject(quoteRequest.getSubject());
        order.setClientName(quoteRequest.getClientName());
        order.setClientEmail(quoteRequest.getClientEmail());
        order.setCategory(category);
        order.setDesiredDeliveryDate(quoteRequest.getDesiredDeliveryDate());
        order.setComment("comment");
        order.setStatus(OrderStatus.RECEIVED);

        Task task = new Task();
        task.setId(taskId);
        task.setOrder(order);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStatusUpdatedAt(LocalDateTime.now().minusDays(1));
        return task;
    }
}
