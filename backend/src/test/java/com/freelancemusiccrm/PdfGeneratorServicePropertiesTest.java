package com.freelancemusiccrm;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;
import com.freelancemusiccrm.service.PdfGeneratorService;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

class PdfGeneratorServicePropertiesTest {

    @Property(tries = 50)
    @Tag("Feature: freelance-music-crm, Property 20: PDF 生成の有効性（日本語含む）")
    void generatedPdfIsValidAndContainsText(
            @ForAll @LongRange(min = 1, max = 100000) long invoiceId,
            @ForAll @LongRange(min = 1, max = 100000) long taskId,
            @ForAll @StringLength(min = 1, max = 20) String subjectSuffix,
            @ForAll @StringLength(min = 1, max = 20) String clientSuffix,
            @ForAll @IntRange(min = 1, max = 365) int deliveryDays,
            @ForAll @DoubleRange(min = 0.0, max = 1000000.0) double amountRaw
    ) throws Exception {
        PdfGeneratorService service = new PdfGeneratorService();
        InvoiceResponseDto invoice = new InvoiceResponseDto(
                invoiceId,
                taskId,
                "請求書件名" + subjectSuffix,
                "依頼者" + clientSuffix,
                "client@example.com",
                "作曲",
                LocalDate.now().plusDays(deliveryDays),
                BigDecimal.valueOf(amountRaw),
                LocalDate.now(),
                "作業者名",
                "contact@example.com",
                LocalDateTime.now()
        );

        byte[] pdf = service.generateInvoicePdf(invoice);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(pdf.length, 5), java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF-");

        try (var document = Loader.loadPDF(new ByteArrayInputStream(pdf).readAllBytes())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("請求書");
            assertThat(text).contains(invoice.subject());
            assertThat(text).contains(invoice.clientName());
            assertThat(text).contains(invoice.categoryName());
            assertThat(text).contains(invoice.workerName());
        }
    }
}
