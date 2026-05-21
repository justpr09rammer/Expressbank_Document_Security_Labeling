package org.example.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.Iterator;

/**
 * Extracts plain text from Word (.docx/.doc) and Excel (.xlsx/.xls) files
 * using Apache POI.
 */
public class DocumentParser {

    public record ParseResult(String text, int wordCount, long fileSizeBytes, String fileType) {}

    /**
     * Parse a document and return all extracted text.
     *
     * @param filePath absolute path to the file
     * @return ParseResult containing text and metadata
     * @throws IOException if the file cannot be read or is unsupported
     */
    public ParseResult parse(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        long fileSize = Files.size(path);
        String name   = path.getFileName().toString().toLowerCase();

        String text;
        String fileType;

        if (name.endsWith(".docx")) {
            text     = parseDocx(filePath);
            fileType = "docx";
        } else if (name.endsWith(".doc")) {
            text     = parseDoc(filePath);
            fileType = "doc";
        } else if (name.endsWith(".xlsx")) {
            text     = parseXlsx(filePath);
            fileType = "xlsx";
        } else if (name.endsWith(".xls")) {
            text     = parseXls(filePath);
            fileType = "xls";
        } else if (name.endsWith(".txt") || name.endsWith(".csv")) {
            text     = Files.readString(path);
            fileType = name.endsWith(".csv") ? "csv" : "txt";
        } else {
            throw new UnsupportedOperationException(
                "Unsupported file type. Supported: .docx, .doc, .xlsx, .xls, .txt, .csv");
        }

        int wordCount = countWords(text);
        return new ParseResult(text, wordCount, fileSize, fileType);
    }

    // ── Word (.docx) ──────────────────────────────────────────────────────

    private String parseDocx(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {

            // Body paragraphs
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }

            // Tables
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.isBlank()) {
                            sb.append(cellText).append(" ");
                        }
                    }
                    sb.append("\n");
                }
            }

            // Headers and footers
            for (XWPFHeader header : doc.getHeaderList()) {
                sb.append(header.getText()).append("\n");
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                sb.append(footer.getText()).append("\n");
            }

            // Document properties (core properties)
            if (doc.getCoreProperties() != null) {
                appendIfNotNull(sb, "Title: ",    doc.getCoreProperties().getTitle());
                appendIfNotNull(sb, "Subject: ",  doc.getCoreProperties().getSubject());
                appendIfNotNull(sb, "Keywords: ", doc.getCoreProperties().getKeywords());
                appendIfNotNull(sb, "Description: ", doc.getCoreProperties().getDescription());
            }
        }
        return sb.toString();
    }

    // ── Word (.doc legacy) ───────────────────────────────────────────────

    private String parseDoc(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return String.join("\n", extractor.getParagraphText());
        }
    }

    // ── Excel (.xlsx) ────────────────────────────────────────────────────

    private String parseXlsx(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            return extractWorkbookText(wb);
        }
    }

    // ── Excel (.xls legacy) ──────────────────────────────────────────────

    private String parseXls(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             HSSFWorkbook wb = new HSSFWorkbook(fis)) {
            return extractWorkbookText(wb);
        }
    }

    private String extractWorkbookText(Workbook wb) {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");

            Iterator<Row> rowIt = sheet.iterator();
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                Iterator<Cell> cellIt = row.cellIterator();
                while (cellIt.hasNext()) {
                    Cell cell = cellIt.next();
                    String val = formatter.formatCellValue(cell);
                    if (val != null && !val.isBlank()) {
                        sb.append(val).append("\t");
                    }
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private void appendIfNotNull(StringBuilder sb, String prefix, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(prefix).append(value).append("\n");
        }
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /** Quick preview: first N characters of extracted text */
    public String preview(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "…";
    }
}