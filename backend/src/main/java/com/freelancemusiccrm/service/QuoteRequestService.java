package com.freelancemusiccrm.service;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.freelancemusiccrm.dto.quote.QuoteRequestCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestCreateResponseDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestResponseDto;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.exception.ResourceNotFoundException;
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.QuoteRequestRepository;

@Service
public class QuoteRequestService {

    private static final Logger logger = LoggerFactory.getLogger(QuoteRequestService.class);

    private final QuoteRequestRepository quoteRequestRepository;
    private final OrderCategoryRepository orderCategoryRepository;
    private final DiscordNotifierService discordNotifierService;

    public QuoteRequestService(QuoteRequestRepository quoteRequestRepository,
                               OrderCategoryRepository orderCategoryRepository,
                               DiscordNotifierService discordNotifierService) {
        this.quoteRequestRepository = quoteRequestRepository;
        this.orderCategoryRepository = orderCategoryRepository;
        this.discordNotifierService = discordNotifierService;
    }

    @Transactional
    public QuoteRequestCreateResponseDto create(QuoteRequestCreateDto dto) {
        Long categoryId = Objects.requireNonNull(dto.categoryId(), "categoryId must not be null");
        OrderCategory category = orderCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("依頼区分が見つかりません"));

        QuoteRequest request = new QuoteRequest();
        request.setSubject(escape(dto.subject()));
        request.setClientName(escape(dto.clientName()));
        request.setClientEmail(escapeNullable(dto.clientEmail()));
        request.setCategory(category);
        request.setDesiredDeliveryDate(dto.desiredDeliveryDate());
        request.setFilePathUrl(escapeNullable(dto.filePathUrl()));
        request.setComment(escapeNullable(dto.comment()));
        request.setStatus(QuoteRequestStatus.PENDING);

        QuoteRequest saved = quoteRequestRepository.save(request);
        try {
            discordNotifierService.notifyQuoteRequestCreated(saved);
        } catch (RuntimeException ex) {
            logger.warn("Discord通知呼び出しで例外が発生しましたが、見積依頼保存は継続します: {}", ex.getMessage(), ex);
        }

        return new QuoteRequestCreateResponseDto(
                saved.getId(),
                saved.getStatus(),
                "見積依頼を受け付けました"
        );
    }

    @Transactional(readOnly = true)
    public List<QuoteRequestResponseDto> findAll() {
        return quoteRequestRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuoteRequestResponseDto findById(Long id) {
        QuoteRequest request = quoteRequestRepository.findById(Objects.requireNonNull(id, "id must not be null"))
                .orElseThrow(() -> new ResourceNotFoundException("見積依頼が見つかりません"));
        return toResponseDto(request);
    }

    private QuoteRequestResponseDto toResponseDto(QuoteRequest request) {
        return new QuoteRequestResponseDto(
                request.getId(),
                request.getSubject(),
                request.getClientName(),
                request.getClientEmail(),
                request.getCategory().getId(),
                request.getCategory().getName(),
                request.getDesiredDeliveryDate(),
                request.getFilePathUrl(),
                request.getComment(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    @SuppressWarnings("null")
    private String escape(String value) {
        return Objects.requireNonNull(HtmlUtils.htmlEscape(value), "escaped value must not be null");
    }

    private String escapeNullable(String value) {
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(value);
    }
}
