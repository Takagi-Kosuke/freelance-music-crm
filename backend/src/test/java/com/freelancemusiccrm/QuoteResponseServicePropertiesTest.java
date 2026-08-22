package com.freelancemusiccrm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

import com.freelancemusiccrm.dto.quote.QuoteResponseCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteResponseDto;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.entity.QuoteResponse;
import com.freelancemusiccrm.exception.ConflictException;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.repository.QuoteResponseRepository;
import com.freelancemusiccrm.service.QuoteResponseService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

class QuoteResponseServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 7: 見積回答データのラウンドトリップ保存")
    void quoteResponseRoundTrip(
            @ForAll @LongRange(min = 1, max = 100000) long quoteRequestId,
            @ForAll @DoubleRange(min = 0.0, max = 1_000_000.0) double amountRaw,
            @ForAll @IntRange(min = 1, max = 365) int deliveryDays,
            @ForAll @StringLength(min = 0, max = 1000) String responseComment
    ) {
        QuoteRequestRepository quoteRequestRepository = mock(QuoteRequestRepository.class);
        QuoteResponseRepository quoteResponseRepository = mock(QuoteResponseRepository.class);
        QuoteResponseService service = new QuoteResponseService(quoteResponseRepository, quoteRequestRepository);

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(quoteRequestId);
        quoteRequest.setStatus(QuoteRequestStatus.PENDING);

        when(quoteRequestRepository.findById(quoteRequestId)).thenReturn(Optional.of(quoteRequest));
        when(quoteResponseRepository.findByQuoteRequestId(quoteRequestId)).thenReturn(Optional.empty());
        when(quoteResponseRepository.findByApprovalToken(anyString())).thenReturn(Optional.empty());

        AtomicLong idSequence = new AtomicLong(1L);
        when(quoteResponseRepository.save(any(QuoteResponse.class))).thenAnswer((Answer<QuoteResponse>) invocation -> {
            QuoteResponse saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(idSequence.getAndIncrement());
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        BigDecimal amount = BigDecimal.valueOf(amountRaw).setScale(2, RoundingMode.DOWN);
        QuoteResponseCreateDto dto = new QuoteResponseCreateDto(
                quoteRequestId,
                amount,
                LocalDate.now().plusDays(deliveryDays),
                responseComment
        );

        QuoteResponseDto created = service.create(dto);

        assertThat(created.quoteRequestId()).isEqualTo(dto.quoteRequestId());
        assertThat(created.amount()).isEqualTo(dto.amount());
        assertThat(created.responseDeliveryDate()).isEqualTo(dto.responseDeliveryDate());
        assertThat(created.responseComment()).isEqualTo(dto.responseComment());
        assertThat(created.approvalToken()).isNotBlank();
        assertThat(created.createdAt()).isNotNull();
        assertThat(quoteRequest.getStatus()).isEqualTo(QuoteRequestStatus.RESPONDED);
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 8: QuoteRequest 1 件に対する QuoteResponse の一意性")
    void quoteResponseUniqueness(
            @ForAll @LongRange(min = 1, max = 100000) long quoteRequestId,
            @ForAll @DoubleRange(min = 0.0, max = 1_000_000.0) double amountRaw
    ) {
        QuoteRequestRepository quoteRequestRepository = mock(QuoteRequestRepository.class);
        QuoteResponseRepository quoteResponseRepository = mock(QuoteResponseRepository.class);
        QuoteResponseService service = new QuoteResponseService(quoteResponseRepository, quoteRequestRepository);

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setId(quoteRequestId);

        QuoteResponse existing = new QuoteResponse();
        existing.setId(10L);
        existing.setQuoteRequest(quoteRequest);

        when(quoteRequestRepository.findById(quoteRequestId)).thenReturn(Optional.of(quoteRequest));
        when(quoteResponseRepository.findByQuoteRequestId(quoteRequestId)).thenReturn(Optional.of(existing));

        QuoteResponseCreateDto dto = new QuoteResponseCreateDto(
                quoteRequestId,
                BigDecimal.valueOf(amountRaw).setScale(2, RoundingMode.DOWN),
                LocalDate.now().plusDays(10),
                "dup"
        );

        ConflictException exception = assertThrows(ConflictException.class, () -> service.create(dto));
        assertThat(exception.getMessage()).contains("既に回答");
        verify(quoteResponseRepository, never()).save(any(QuoteResponse.class));
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 9: 承認トークンの一意性")
    void approvalTokenUniqueness(
            @ForAll @IntRange(min = 2, max = 20) int requestCount
    ) {
        QuoteRequestRepository quoteRequestRepository = mock(QuoteRequestRepository.class);
        QuoteResponseRepository quoteResponseRepository = mock(QuoteResponseRepository.class);
        QuoteResponseService service = new QuoteResponseService(quoteResponseRepository, quoteRequestRepository);

        when(quoteRequestRepository.findById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            QuoteRequest quoteRequest = new QuoteRequest();
            quoteRequest.setId(id);
            return Optional.of(quoteRequest);
        });
        when(quoteResponseRepository.findByQuoteRequestId(anyLong())).thenReturn(Optional.empty());
        when(quoteResponseRepository.findByApprovalToken(anyString())).thenReturn(Optional.empty());

        AtomicLong idSequence = new AtomicLong(1L);
        when(quoteResponseRepository.save(any(QuoteResponse.class))).thenAnswer((Answer<QuoteResponse>) invocation -> {
            QuoteResponse saved = invocation.getArgument(0);
            saved.setId(idSequence.getAndIncrement());
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < requestCount; i++) {
            QuoteResponseCreateDto dto = new QuoteResponseCreateDto(
                    (long) i + 1,
                    BigDecimal.valueOf(1000 + i),
                    LocalDate.now().plusDays(30),
                    "token-check"
            );
            QuoteResponseDto created = service.create(dto);
            tokens.add(created.approvalToken());
        }

        assertThat(tokens).hasSize(requestCount);
    }
}
