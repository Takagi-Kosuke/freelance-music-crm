package com.freelancemusiccrm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;
import org.springframework.web.util.HtmlUtils;

import com.freelancemusiccrm.dto.quote.QuoteRequestCreateDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestCreateResponseDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestResponseDto;
import com.freelancemusiccrm.entity.OrderCategory;
import com.freelancemusiccrm.entity.QuoteRequest;
import com.freelancemusiccrm.entity.QuoteRequestStatus;
import com.freelancemusiccrm.repository.OrderCategoryRepository;
import com.freelancemusiccrm.repository.QuoteRequestRepository;
import com.freelancemusiccrm.service.DiscordNotifierService;
import com.freelancemusiccrm.service.QuoteRequestService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

@SuppressWarnings("null")
class QuoteRequestServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 4: 有効な見積依頼のラウンドトリップ保存")
    void validQuoteRequestRoundTrips(
            @ForAll @StringLength(min = 1, max = 100) String subject,
            @ForAll @StringLength(min = 1, max = 100) String clientName,
            @ForAll @StringLength(min = 0, max = 1000) String comment,
            @ForAll @IntRange(min = 1, max = 365) int desiredDeliveryDays
    ) {
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

        when(quoteRequestRepository.save(org.mockito.ArgumentMatchers.any(QuoteRequest.class))).thenAnswer((Answer<QuoteRequest>) invocation -> {
            QuoteRequest saved = invocation.getArgument(0);
            saved.setId(123L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        String escapedSubject = HtmlUtils.htmlEscape(subject);
        String escapedClientName = HtmlUtils.htmlEscape(clientName);
        String escapedComment = HtmlUtils.htmlEscape(comment);

        when(quoteRequestRepository.findById(123L)).thenAnswer(invocation -> {
            QuoteRequest request = new QuoteRequest();
            request.setId(123L);
            request.setSubject(escapedSubject);
            request.setClientName(escapedClientName);
            request.setClientEmail("client@example.com");
            request.setCategory(category);
            request.setDesiredDeliveryDate(LocalDate.now().plusDays(desiredDeliveryDays));
            request.setFilePathUrl("https://example.com/work");
            request.setComment(escapedComment);
            request.setStatus(QuoteRequestStatus.PENDING);
            request.setCreatedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            return Optional.of(request);
        });

        QuoteRequestCreateDto createDto = new QuoteRequestCreateDto(
                subject,
                clientName,
                "client@example.com",
                1L,
                LocalDate.now().plusDays(desiredDeliveryDays),
                "https://example.com/work",
                comment
        );

        QuoteRequestCreateResponseDto created = service.create(createDto);
        QuoteRequestResponseDto loaded = service.findById(created.id());

        assertThat(loaded.subject()).isEqualTo(escapedSubject);
        assertThat(loaded.clientName()).isEqualTo(escapedClientName);
        assertThat(loaded.categoryId()).isEqualTo(1L);
        assertThat(loaded.status()).isEqualTo(QuoteRequestStatus.PENDING);
        assertThat(loaded.filePathUrl()).isEqualTo("https://example.com/work");
        assertThat(loaded.comment()).isEqualTo(escapedComment);
        }

        @Property(tries = 100)
        @Tag("Feature: freelance-music-crm, Property 24: SQL インジェクション入力の安全な処理")
        void sqlInjectionPayloadDoesNotBreakProcessing(
            @ForAll @StringLength(min = 1, max = 40) String left,
            @ForAll @StringLength(min = 1, max = 40) String right,
            @ForAll @IntRange(min = 1, max = 365) int desiredDeliveryDays
        ) {
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

        when(quoteRequestRepository.save(org.mockito.ArgumentMatchers.any(QuoteRequest.class))).thenAnswer((Answer<QuoteRequest>) invocation -> {
            QuoteRequest saved = invocation.getArgument(0);
            saved.setId(456L);
            return saved;
        });

        String injection = left + "' OR 1=1 -- " + right;
        QuoteRequestCreateResponseDto created = service.create(new QuoteRequestCreateDto(
            injection,
            "依頼者",
            "client@example.com",
            1L,
            LocalDate.now().plusDays(desiredDeliveryDays),
            "https://example.com/work",
            injection
        ));

        assertThat(created.id()).isEqualTo(456L);
        assertThat(created.status()).isEqualTo(QuoteRequestStatus.PENDING);
        }

        @Property(tries = 100)
        @Tag("Feature: freelance-music-crm, Property 25: XSS 入力のエスケープ処理")
        void xssPayloadIsEscapedBeforePersistence(
            @ForAll @StringLength(min = 1, max = 30) String payloadSeed,
            @ForAll @IntRange(min = 1, max = 365) int desiredDeliveryDays
        ) {
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

        AtomicReference<QuoteRequest> captured = new AtomicReference<>();
        when(quoteRequestRepository.save(org.mockito.ArgumentMatchers.any(QuoteRequest.class))).thenAnswer((Answer<QuoteRequest>) invocation -> {
            QuoteRequest saved = invocation.getArgument(0);
            saved.setId(789L);
            captured.set(saved);
            return saved;
        });

        String xss = "<script>alert('" + payloadSeed + "')</script>";
        QuoteRequestCreateResponseDto created = service.create(new QuoteRequestCreateDto(
            xss,
            xss,
            "client@example.com",
            1L,
            LocalDate.now().plusDays(desiredDeliveryDays),
            "https://example.com/work",
            xss
        ));

        assertThat(created.id()).isEqualTo(789L);
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getSubject()).doesNotContain("<script>");
        assertThat(captured.get().getClientName()).doesNotContain("<script>");
        assertThat(captured.get().getComment()).doesNotContain("<script>");
        assertThat(captured.get().getSubject()).contains("&lt;script&gt;");
    }
}
