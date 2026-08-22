package com.freelancemusiccrm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.freelancemusiccrm.dto.order.OrderActionResponseDto;
import com.freelancemusiccrm.entity.Order;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TokenStatus;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.OrderRepository;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.repository.QuoteResponseRepository;
import com.freelancemusiccrm.repository.TaskRepository;
import com.freelancemusiccrm.service.DiscordNotifierService;
import com.freelancemusiccrm.service.OrderService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

class OrderServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 10: 承認操作による Order・Task の一括生成とデータ完全性")
    void approveCreatesOrderAndTask(
            @ForAll @LongRange(min = 1, max = 100000) long quoteRequestId,
            @ForAll @StringLength(min = 1, max = 100) String subject,
            @ForAll @StringLength(min = 1, max = 100) String clientName,
            @ForAll @StringLength(min = 0, max = 100) String comment,
            @ForAll @IntRange(min = 1, max = 365) int deliveryDays,
            @ForAll @DoubleRange(min = 0.0, max = 1000000.0) double amountRaw
    ) {
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
        QuoteResponse quoteResponse = buildActiveQuoteResponse(quoteRequestId, subject, clientName, comment, deliveryDays, amountRaw);
        when(quoteResponseRepository.findByApprovalToken(token)).thenReturn(Optional.of(quoteResponse));
        when(orderRepository.findByQuoteResponseId(quoteResponse.getId())).thenReturn(Optional.empty());
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

        OrderActionResponseDto result = service.approveByToken(token);

        assertThat(result.orderId()).isNotNull();
        assertThat(result.taskId()).isNotNull();
        assertThat(result.message()).isEqualTo("正式依頼を承認しました");

        assertThat(quoteResponse.getQuoteRequest().getStatus()).isEqualTo(QuoteRequestStatus.APPROVED);
        assertThat(quoteResponse.getTokenStatus()).isEqualTo(TokenStatus.USED);

        verify(taskRepository).save(any(Task.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 11: 辞退操作で Task が生成されない")
    void declineDoesNotCreateTask(
            @ForAll @LongRange(min = 1, max = 100000) long quoteRequestId
    ) {
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

        String token = "decline-token-" + quoteRequestId;
        QuoteResponse quoteResponse = buildActiveQuoteResponse(quoteRequestId, "件名", "依頼者", "", 30, 1000.0);
        when(quoteResponseRepository.findByApprovalToken(token)).thenReturn(Optional.of(quoteResponse));
        when(quoteRequestRepository.save(any(QuoteRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteResponseRepository.save(any(QuoteResponse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderActionResponseDto result = service.declineByToken(token);

        assertThat(result.orderId()).isNull();
        assertThat(result.taskId()).isNull();
        assertThat(quoteResponse.getQuoteRequest().getStatus()).isEqualTo(QuoteRequestStatus.DECLINED);
        assertThat(quoteResponse.getTokenStatus()).isEqualTo(TokenStatus.USED);

        verify(orderRepository, never()).save(any(Order.class));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 12: Order の重複生成防止")
    void duplicateApproveIsRejected(@ForAll @LongRange(min = 1, max = 100000) long quoteRequestId) {
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

        String token = "duplicate-token-" + quoteRequestId;
        QuoteResponse quoteResponse = buildActiveQuoteResponse(quoteRequestId, "件名", "依頼者", "", 30, 1000.0);
        when(quoteResponseRepository.findByApprovalToken(token)).thenReturn(Optional.of(quoteResponse));

        Order existing = new Order();
        existing.setId(999L);
        when(orderRepository.findByQuoteResponseId(anyLong())).thenReturn(Optional.of(existing));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.approveByToken(token));
        assertThat(exception.getMessage()).contains("見積回答が見つかりません");

        verify(orderRepository, never()).save(any(Order.class));
        verify(taskRepository, never()).save(any(Task.class));
    }

    private QuoteResponse buildActiveQuoteResponse(long quoteRequestId,
                                                   String subject,
                                                   String clientName,
                                                   String comment,
                                                   int deliveryDays,
                                                   double amountRaw) {
        OrderCategory category = new OrderCategory();
        category.setId(1L);
        category.setName("作曲");

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(quoteRequestId);
        quoteRequest.setSubject(subject);
        quoteRequest.setClientName(clientName);
        quoteRequest.setClientEmail("client@example.com");
        quoteRequest.setCategory(category);
        quoteRequest.setDesiredDeliveryDate(LocalDate.now().plusDays(deliveryDays));
        quoteRequest.setComment(comment);
        quoteRequest.setStatus(QuoteRequestStatus.RESPONDED);

        QuoteResponse quoteResponse = new QuoteResponse();
        quoteResponse.setId(quoteRequestId + 1000);
        quoteResponse.setQuoteRequest(quoteRequest);
        quoteResponse.setAmount(BigDecimal.valueOf(amountRaw));
        quoteResponse.setResponseDeliveryDate(LocalDate.now().plusDays(deliveryDays));
        quoteResponse.setResponseComment("コメント");
        quoteResponse.setApprovalToken("token-" + quoteRequestId);
        quoteResponse.setTokenStatus(TokenStatus.ACTIVE);
        quoteResponse.setCreatedAt(LocalDateTime.now());
        return quoteResponse;
    }
}
