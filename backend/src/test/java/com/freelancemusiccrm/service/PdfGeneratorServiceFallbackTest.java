package com.freelancemusiccrm.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;

class PdfGeneratorServiceFallbackTest {

    @Test
    void generatesPdfEvenWhenJapaneseSystemFontsAreUnavailable() throws Exception {
        PdfGeneratorService service = new PdfGeneratorService();
        InvoiceResponseDto invoice = new InvoiceResponseDto(
                1L,
                2L,
                "請求書件名",
                "依頼者名",
                "client@example.com",
                "作曲",
                LocalDate.now(),
                BigDecimal.valueOf(50000),
                LocalDate.now(),
                "作業者名",
                "contact@example.com",
                LocalDateTime.now()
        );

        byte[] pdf = service.generateInvoicePdf(invoice);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(pdf.length, 5), java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF-");

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("請求書");
        }
    }
}
