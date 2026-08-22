package com.freelancemusiccrm;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import com.freelancemusiccrm.dto.category.OrderCategoryUpsertDto;
import com.freelancemusiccrm.dto.quote.QuoteRequestCreateDto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

class DtoValidationPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 3: 必須フィールド欠落時のバリデーションエラー")
    void quoteRequestRequiredFieldsValidation(
            @ForAll boolean includeSubject,
            @ForAll boolean includeClientName,
            @ForAll boolean includeCategory,
            @ForAll boolean includeDesiredDeliveryDate
    ) {
        Assume.that(!(includeSubject && includeClientName && includeCategory && includeDesiredDeliveryDate));

        QuoteRequestCreateDto dto = new QuoteRequestCreateDto(
                includeSubject ? "案件A" : null,
                includeClientName ? "依頼者A" : null,
                null,
                includeCategory ? 1L : null,
                includeDesiredDeliveryDate ? LocalDate.now().plusDays(10) : null,
                null,
                "コメント"
        );

        Set<ConstraintViolation<QuoteRequestCreateDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();

        Set<String> fields = violations.stream().map(v -> v.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
        if (!includeSubject) {
            assertThat(fields).contains("subject");
        }
        if (!includeClientName) {
            assertThat(fields).contains("clientName");
        }
        if (!includeCategory) {
            assertThat(fields).contains("categoryId");
        }
        if (!includeDesiredDeliveryDate) {
            assertThat(fields).contains("desiredDeliveryDate");
        }
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 5: URLフィールドのフォーマットバリデーション")
    void quoteRequestUrlValidation(@ForAll String url) {
        QuoteRequestCreateDto dto = new QuoteRequestCreateDto(
                "件名",
                "依頼者",
                null,
                1L,
                LocalDate.now().plusDays(5),
                url,
                null
        );

        Set<ConstraintViolation<QuoteRequestCreateDto>> violations = validator.validate(dto);
        boolean hasUrlViolation = violations.stream().anyMatch(v -> "filePathUrl".equals(v.getPropertyPath().toString()));

        if (url.matches("^https?://.+$")) {
            assertThat(hasUrlViolation).isFalse();
        } else {
            assertThat(hasUrlViolation).isTrue();
        }
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 6: コメント文字数上限バリデーション")
    void quoteRequestCommentLengthValidation(@ForAll @StringLength(min = 0, max = 1500) String comment) {
        QuoteRequestCreateDto dto = new QuoteRequestCreateDto(
                "件名",
                "依頼者",
                null,
                1L,
                LocalDate.now().plusDays(5),
                null,
                comment
        );

        Set<ConstraintViolation<QuoteRequestCreateDto>> violations = validator.validate(dto);
        boolean hasCommentViolation = violations.stream().anyMatch(v -> "comment".equals(v.getPropertyPath().toString()));

        if (comment.length() <= 1000) {
            assertThat(hasCommentViolation).isFalse();
        } else {
            assertThat(hasCommentViolation).isTrue();
        }
    }

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 17: 区分名文字数バリデーション")
    void orderCategoryNameLengthValidation(@ForAll @IntRange(min = 0, max = 60) int length) {
        String name = "a".repeat(length);
        OrderCategoryUpsertDto dto = new OrderCategoryUpsertDto(name);

        Set<ConstraintViolation<OrderCategoryUpsertDto>> violations = validator.validate(dto);
        boolean hasNameViolation = violations.stream().anyMatch(v -> "name".equals(v.getPropertyPath().toString()));

        if (length >= 1 && length <= 50) {
            assertThat(hasNameViolation).isFalse();
        } else {
            assertThat(hasNameViolation).isTrue();
        }
    }
}
