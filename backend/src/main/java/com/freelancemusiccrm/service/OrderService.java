package com.freelancemusiccrm.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.order.OrderActionResponseDto;
import com.freelancemusiccrm.entity.Order;
import com.freelancemusiccrm.entity.OrderStatus;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.entity.Task;
import com.freelancemusiccrm.entity.TaskStatus;
import com.freelancemusiccrm.entity.TokenStatus;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.OrderRepository;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.repository.QuoteResponseRepository;
import com.freelancemusiccrm.repository.TaskRepository;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final QuoteResponseRepository quoteResponseRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final DiscordNotifierService discordNotifierService;

    public OrderService(QuoteResponseRepository quoteResponseRepository,
                        QuoteRequestRepository quoteRequestRepository,
                        OrderRepository orderRepository,
                        TaskRepository taskRepository,
                        DiscordNotifierService discordNotifierService) {
        this.quoteResponseRepository = quoteResponseRepository;
        this.quoteRequestRepository = quoteRequestRepository;
        this.orderRepository = orderRepository;
        this.taskRepository = taskRepository;
        this.discordNotifierService = discordNotifierService;
    }

    @Transactional
    public OrderActionResponseDto approveByToken(String token) {
        QuoteResponse quoteResponse = findActiveQuoteResponseByToken(token);
        QuoteRequest quoteRequest = quoteResponse.getQuoteRequest();

        orderRepository.findByQuoteResponseId(quoteResponse.getId())
                .ifPresent(existing -> {
                    throw new ResourceNotFoundException("見積回答が見つかりません");
                });

        Order order = new Order();
        order.setQuoteResponse(quoteResponse);
        order.setSubject(quoteRequest.getSubject());
        order.setClientName(quoteRequest.getClientName());
        order.setClientEmail(quoteRequest.getClientEmail());
        order.setCategory(quoteRequest.getCategory());
        order.setDesiredDeliveryDate(quoteRequest.getDesiredDeliveryDate());
        order.setFilePathUrl(quoteRequest.getFilePathUrl());
        order.setComment(quoteRequest.getComment());
        order.setStatus(OrderStatus.RECEIVED);

        Order savedOrder = orderRepository.save(order);

        Task task = new Task();
        task.setOrder(savedOrder);
        task.setStatus(TaskStatus.NOT_STARTED);
        task.setStatusUpdatedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        quoteRequest.setStatus(QuoteRequestStatus.APPROVED);
        quoteRequestRepository.save(quoteRequest);

        quoteResponse.setTokenStatus(TokenStatus.USED);
        quoteResponseRepository.save(quoteResponse);

        try {
            discordNotifierService.notifyOrderCreated(savedOrder);
        } catch (RuntimeException ex) {
            logger.warn("Discord通知呼び出しで例外が発生しましたが、正式依頼処理は継続します: {}", ex.getMessage(), ex);
        }

        return new OrderActionResponseDto(
                savedOrder.getId(),
                savedTask.getId(),
                "正式依頼を承認しました"
        );
    }

    @Transactional
    public OrderActionResponseDto declineByToken(String token) {
        QuoteResponse quoteResponse = findActiveQuoteResponseByToken(token);
        QuoteRequest quoteRequest = quoteResponse.getQuoteRequest();

        quoteRequest.setStatus(QuoteRequestStatus.DECLINED);
        quoteRequestRepository.save(quoteRequest);

        quoteResponse.setTokenStatus(TokenStatus.USED);
        quoteResponseRepository.save(quoteResponse);

        return new OrderActionResponseDto(
                null,
                null,
                "依頼を辞退しました"
        );
    }

    private QuoteResponse findActiveQuoteResponseByToken(String token) {
        QuoteResponse quoteResponse = quoteResponseRepository.findByApprovalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("見積回答が見つかりません"));

        if (quoteResponse.getTokenStatus() != TokenStatus.ACTIVE) {
            throw new ResourceNotFoundException("見積回答が見つかりません");
        }

        return quoteResponse;
    }
}
