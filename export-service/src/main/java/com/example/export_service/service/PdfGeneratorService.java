package com.example.export_service.service;

import com.example.export_service.client.ResumeData;
import com.example.export_service.client.SectionData;
import com.example.export_service.dto.ExportRequest;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class PdfGeneratorService {

    private static final Map<String, TemplateStyle> TEMPLATE_STYLES = Map.ofEntries(
            Map.entry("modern-professional", new TemplateStyle(StandardFonts.HELVETICA, true, 2f, ColorConstants.BLUE)),
            Map.entry("clean-minimal", new TemplateStyle(StandardFonts.HELVETICA, false, 1f, ColorConstants.BLUE)),
            Map.entry("classic-executive", new TemplateStyle(StandardFonts.TIMES_ROMAN, true, 2f, ColorConstants.DARK_GRAY)),
            Map.entry("creative-portfolio", new TemplateStyle(StandardFonts.HELVETICA, true, 2f, ColorConstants.BLUE)),
            Map.entry("default", new TemplateStyle(StandardFonts.HELVETICA, true, 2f, ColorConstants.BLUE))
    );

    private record TemplateStyle(String fontName, boolean uppercaseHeadings, float dividerWeight, Color accentColor) {}

    public Path generatePdf(ExportRequest request, ResumeData resume, List<SectionData> sections, Path outputFile) throws IOException {
        PdfWriter writer = new PdfWriter(outputFile.toFile());
        PdfDocument pdfDocument = new PdfDocument(writer);
        Document document = new Document(pdfDocument, PageSize.A4);

        document.setMargins(36, 36, 36, 36);

        String templateKey = request != null && request.templateKey() != null && !request.templateKey().isBlank()
                ? request.templateKey()
                : (resume != null ? resume.template() : "default");
        TemplateStyle style = getTemplateStyle(templateKey);
        
        if (resume != null) {
            addHeader(document, resume, style);
            addSummary(document, resume.summary(), style);
        }
        
        if (sections != null && !sections.isEmpty()) {
            addSections(document, sections, style);
        } else {
            document.add(new Paragraph("No resume sections available. Please add content in the Resume Builder.")
                    .setFont(PdfFontFactory.createFont(style.fontName()))
                    .setFontSize(11)
                    .setFontColor(ColorConstants.GRAY));
        }

        document.close();
        return outputFile;
    }

    private TemplateStyle getTemplateStyle(String templateKey) {
        return TEMPLATE_STYLES.getOrDefault(normalizeTemplateKey(templateKey), TEMPLATE_STYLES.get("default"));
    }

    private void addHeader(Document document, ResumeData resume, TemplateStyle style) throws IOException {
        String headingText = resume.name() != null && !resume.name().isBlank() ? resume.name() : resume.title();
        if (headingText != null && !headingText.isBlank()) {
            document.add(new Paragraph(headingText)
                    .setFont(PdfFontFactory.createFont(style.fontName()))
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(ColorConstants.BLACK)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        if (resume.title() != null && !resume.title().isBlank() && !resume.title().equals(headingText)) {
            document.add(new Paragraph(resume.title())
                    .setFont(PdfFontFactory.createFont(style.fontName()))
                    .setFontSize(12)
                    .setItalic()
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        SolidLine line = new SolidLine(style.dividerWeight());
        line.setColor(style.accentColor());
        document.add(new LineSeparator(line).setMarginTop(10).setMarginBottom(16));
    }

    private void addSummary(Document document, String summary, TemplateStyle style) throws IOException {
        if (summary == null || summary.isBlank()) {
            return;
        }

        document.add(new Paragraph("Summary")
                .setBold()
                .setFont(PdfFontFactory.createFont(style.fontName()))
                .setFontSize(12)
                .setFontColor(style.accentColor()));
        document.add(new Paragraph(summary)
                .setFont(PdfFontFactory.createFont(style.fontName()))
                .setFontSize(11)
                .setFontColor(ColorConstants.BLACK)
                .setMarginBottom(12));
    }

    private void addSections(Document document, List<SectionData> sections, TemplateStyle style) throws IOException {
        if (sections == null || sections.isEmpty()) {
            return;
        }

        for (SectionData section : sections) {
            String title = section.title() != null && !section.title().isBlank()
                    ? section.title()
                    : (section.type() != null && !section.type().isBlank()
                        ? section.type().replace('_', ' ') : "Section");

            if (style.uppercaseHeadings()) {
                title = title.toUpperCase();
            }

            document.add(new Paragraph(title)
                    .setBold()
                    .setFont(PdfFontFactory.createFont(style.fontName()))
                    .setFontSize(12)
                    .setFontColor(style.accentColor())
                    .setMarginBottom(4));

            renderSectionContent(section.content(), document, style);
            document.add(new Paragraph(" "));
        }
    }

    private void renderSectionContent(String content, Document document, TemplateStyle style) throws IOException {
        if (content == null || content.isBlank()) {
            document.add(new Paragraph("No data available")
                    .setFont(PdfFontFactory.createFont(style.fontName()))
                    .setFontSize(11)
                    .setFontColor(ColorConstants.GRAY));
            return;
        }

        String[] lines = content.split("\\r?\\n");
        boolean addedList = false;
        com.itextpdf.layout.element.List list = new com.itextpdf.layout.element.List()
                .setSymbolIndent(10)
                .setListSymbol("\u2022")
                .setFont(PdfFontFactory.createFont(style.fontName()))
                .setFontSize(11)
                .setFontColor(ColorConstants.BLACK);

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                addedList = true;
                list.add(new com.itextpdf.layout.element.ListItem(line.substring(2).trim()));
            } else {
                if (addedList) {
                    document.add(list);
                    list = new com.itextpdf.layout.element.List()
                            .setSymbolIndent(10)
                            .setListSymbol("\u2022")
                            .setFont(PdfFontFactory.createFont(style.fontName()))
                            .setFontSize(11)
                            .setFontColor(ColorConstants.BLACK);
                    addedList = false;
                }
                document.add(new Paragraph(line)
                        .setFont(PdfFontFactory.createFont(style.fontName()))
                        .setFontSize(11)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginBottom(2));
            }
        }

        if (addedList) {
            document.add(list);
        }
    }

    private String normalizeTemplateKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return "default";
        }
        return templateKey.trim();
    }
}


