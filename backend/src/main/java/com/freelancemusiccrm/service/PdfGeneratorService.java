package com.freelancemusiccrm.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.freelancemusiccrm.dto.invoice.InvoiceResponseDto;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final float MARGIN_X = 48f;
    private static final float PAGE_TOP = 790f;
    private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - (MARGIN_X * 2);
    private static final Color PRIMARY_ACCENT = new Color(20, 92, 158);
    private static final Color TEXT_COLOR = new Color(31, 41, 55);
    private static final Color BORDER_COLOR = new Color(209, 213, 219);
    private static final Color HEADER_BG_COLOR = new Color(245, 240, 233);

    public byte[] generateInvoicePdf(InvoiceResponseDto invoice) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont font = loadJapaneseFont(document);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = PAGE_TOP;

                y = drawTitle(stream, font, y, "請求書");
                y = drawMeta(stream, font, y, invoice);
                y = drawCustomerAndIssuer(stream, font, y, invoice);
                drawInvoiceBody(stream, font, y, invoice);
                drawFooter(stream, font);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("PDFの生成に失敗しました", ex);
        }
    }

    private float drawTitle(PDPageContentStream stream, PDFont font, float y, String title) throws IOException {
        drawText(stream, font, 24, MARGIN_X, y, title);
        stream.setStrokingColor(PRIMARY_ACCENT);
        stream.setLineWidth(1.6f);
        stream.moveTo(MARGIN_X, y - 8f);
        stream.lineTo(MARGIN_X + 110f, y - 8f);
        stream.stroke();
        return y - 38f;
    }

    private float drawMeta(PDPageContentStream stream, PDFont font, float y, InvoiceResponseDto invoice) throws IOException {
        drawText(stream, font, 11, MARGIN_X, y, "請求書ID: " + invoice.id());
        drawText(stream, font, 11, MARGIN_X + 180f, y, "タスクID: " + invoice.taskId());
        drawText(stream, font, 11, MARGIN_X + 330f, y, "発行日: " + invoice.issueDate().format(DATE_FORMATTER));
        return y - 28f;
    }

    private float drawCustomerAndIssuer(PDPageContentStream stream, PDFont font, float y, InvoiceResponseDto invoice) throws IOException {
        float boxHeight = 92f;
        float boxWidth = (CONTENT_WIDTH - 14f) / 2f;

        drawBox(stream, MARGIN_X, y - boxHeight, boxWidth, boxHeight);
        drawBox(stream, MARGIN_X + boxWidth + 14f, y - boxHeight, boxWidth, boxHeight);

        float leftX = MARGIN_X + 10f;
        float rightX = MARGIN_X + boxWidth + 24f;
        float lineY = y - 16f;

        drawText(stream, font, 10, leftX, lineY, "請求先");
        drawText(stream, font, 12, leftX, lineY - 18f, invoice.clientName() + " 様");
        drawText(stream, font, 10, leftX, lineY - 36f, "メール: " + nullSafe(invoice.clientEmail()));

        drawText(stream, font, 10, rightX, lineY, "発行元");
        drawText(stream, font, 12, rightX, lineY - 18f, invoice.workerName());
        drawText(stream, font, 10, rightX, lineY - 36f, "連絡先: " + nullSafe(invoice.workerContact()));

        return y - boxHeight - 24f;
    }

    private float drawInvoiceBody(PDPageContentStream stream, PDFont font, float y, InvoiceResponseDto invoice) throws IOException {
        float[] colWidths = new float[] { 180f, 90f, 80f, 120f };
        float rowHeight = 28f;

        drawHeaderRow(stream, font, y, colWidths, List.of("件名", "区分", "納品日", "金額"));
        y -= rowHeight;

        List<String> values = new ArrayList<>();
        values.add(invoice.subject());
        values.add(invoice.categoryName());
        values.add(invoice.deliveryDate().format(DATE_FORMATTER));
        values.add("¥" + invoice.amount().toPlainString());
        drawDataRow(stream, font, y, colWidths, values);
        y -= rowHeight + 16f;

        drawText(stream, font, 11, MARGIN_X + 250f, y, "合計請求金額");
        drawBox(stream, MARGIN_X + 340f, y - 18f, 160f, 26f);
        drawText(stream, font, 14, MARGIN_X + 350f, y - 1f, "¥" + invoice.amount().toPlainString());

        return y - 48f;
    }

    private void drawFooter(PDPageContentStream stream, PDFont font) throws IOException {
        drawText(stream, font, 9, MARGIN_X, 76f, "備考: 本請求書の内容をご確認のうえ、所定の期日までにお支払いをお願いいたします。");
    }

    private void drawHeaderRow(PDPageContentStream stream, PDFont font, float y, float[] colWidths, List<String> labels) throws IOException {
        float x = MARGIN_X;
        float rowHeight = 28f;
        for (int i = 0; i < colWidths.length; i++) {
            float width = colWidths[i];
            stream.setNonStrokingColor(HEADER_BG_COLOR);
            stream.addRect(x, y - rowHeight, width, rowHeight);
            stream.fill();
            drawBox(stream, x, y - rowHeight, width, rowHeight);
            drawText(stream, font, 10, x + 8f, y - 17f, labels.get(i));
            x += width;
        }
    }

    private void drawDataRow(PDPageContentStream stream, PDFont font, float y, float[] colWidths, List<String> values) throws IOException {
        float x = MARGIN_X;
        float rowHeight = 28f;
        for (int i = 0; i < colWidths.length; i++) {
            float width = colWidths[i];
            drawBox(stream, x, y - rowHeight, width, rowHeight);
            drawText(stream, font, 10, x + 8f, y - 17f, values.get(i));
            x += width;
        }
    }

    private void drawBox(PDPageContentStream stream, float x, float y, float width, float height) throws IOException {
        stream.setStrokingColor(BORDER_COLOR);
        stream.setLineWidth(1f);
        stream.addRect(x, y, width, height);
        stream.stroke();
    }

    private void drawText(PDPageContentStream stream, PDFont font, int fontSize, float x, float y, String text) throws IOException {
        String safeText = sanitizeTextForPdf(font, text);

        stream.beginText();
        try {
            stream.setNonStrokingColor(TEXT_COLOR);
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(x, y);
            stream.showText(safeText);
        } finally {
            stream.endText();
        }
    }

    private String sanitizeTextForPdf(PDFont font, String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            if (Character.isISOControl(codePoint)) {
                return;
            }

            String candidate = new String(Character.toChars(codePoint));
            try {
                font.encode(candidate);
                builder.append(candidate);
            } catch (IOException | IllegalArgumentException ex) {
                builder.append('?');
            }
        });

        if (builder.isEmpty()) {
            return "-";
        }

        return builder.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }

    private PDFont loadJapaneseFont(PDDocument document) throws IOException {
        try {
            PDFont bundledFont = loadBundledJapaneseFont(document);
            if (bundledFont != null) {
                return bundledFont;
            }

            PDFont systemFont = loadSystemJapaneseFont(document);
            if (systemFont != null) {
                return systemFont;
            }
        } catch (RuntimeException ex) {
            // Some Linux/CI images can throw runtime font-loading exceptions even when the resource exists.
            // Keep the PDF generation resilient instead of failing the entire invoice export.
        }

        // 最終フォールバック。日本語の描画が完全ではないものの、500 を避けて PDF を返す。
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDFont loadSystemJapaneseFont(PDDocument document) {
        List<Path> candidatePaths = new ArrayList<>();

        String configuredFontPath = System.getenv("PDF_FONT_PATH");
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            candidatePaths.add(Path.of(configuredFontPath));
        }

        candidatePaths.addAll(List.of(
                Path.of("C:/Windows/Fonts"),
                Path.of("C:/Windows/Fonts/yu gothic"),
                Path.of("C:/Windows/System32/Fonts"),
                Path.of("/usr/share/fonts"),
                Path.of("/usr/share/fonts/truetype"),
                Path.of("/usr/share/fonts/opentype"),
                Path.of("/usr/local/share/fonts"),
                Path.of("/System/Library/Fonts"),
                Path.of("/System/Library/Fonts/Supplemental")
        ));

        List<Path> discoveredFonts = new ArrayList<>();
        for (Path root : candidatePaths) {
            if (!Files.exists(root)) {
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::looksLikeJapaneseFont)
                        .forEach(discoveredFonts::add);
            } catch (IOException ignored) {
                // 次の候補へ進める
            }
        }

        for (Path path : discoveredFonts) {
            try {
                String fileName = path.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".ttc")) {
                    try (TrueTypeCollection collection = new TrueTypeCollection(path.toFile())) {
                        String candidateName = inferredFontName(path);
                        TrueTypeFont font = collection.getFontByName(candidateName);
                        if (font != null) {
                            return PDType0Font.load(document, font, true);
                        }
                    }
                }
                if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                    return PDType0Font.load(document, path.toFile());
                }
            } catch (IOException | RuntimeException ignored) {
                // 次の候補を試す
            }
        }

        return null;
    }

    private PDFont loadBundledJapaneseFont(PDDocument document) throws IOException {
        String[] candidates = {
                "fonts/NotoSansCJKjp-Regular.otf",
                "fonts/NotoSansCJKjp-Regular.ttf",
                "fonts/NotoSansCJKjp-Regular.otc",
                "fonts/NotoSansCJK-Regular.otf",
                "fonts/NotoSansCJK-Regular.ttf",
                "fonts/NotoSansCJK-Regular.otc"
        };

        for (String resourcePath : candidates) {
            try {
                PDFont loadedFont = tryLoadBundledFont(document, resourcePath);
                if (loadedFont != null) {
                    return loadedFont;
                }
            } catch (IOException ignored) {
                // 次の候補へ進める
            }
        }

        return null;
    }

    private PDFont tryLoadBundledFont(PDDocument document, String resourcePath) throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    return PDType0Font.load(document, inputStream);
                }
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
                    if (inputStream != null) {
                        return PDType0Font.load(document, inputStream);
                    }
                }
            }
        } catch (RuntimeException ex) {
            return null;
        }

        return null;
    }

    private boolean looksLikeJapaneseFont(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.contains("noto")
                || fileName.contains("jp")
                || fileName.contains("japan")
                || fileName.contains("gothic")
                || fileName.contains("hiragino")
                || fileName.contains("meiryo")
                || fileName.contains("msgothic")
                || fileName.contains("msyh")
                || fileName.contains("yu")
                || fileName.contains("korean")
                || fileName.contains("cjk")
                || fileName.contains("mincho")
                || fileName.contains("biz-ud")
                || fileName.contains("sourcehan")
                || fileName.contains("wqy")
                || fileName.contains("ipag")
                || fileName.contains("aoyagi");
    }

    private String inferredFontName(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase().contains("meiryo")) {
            return "Meiryo";
        }
        if (name.toLowerCase().contains("gothic")) {
            return "MS Gothic";
        }
        if (name.toLowerCase().contains("hiragino")) {
            return "HiraginoSans-W3";
        }
        return "Noto Sans CJK JP";
    }
}
