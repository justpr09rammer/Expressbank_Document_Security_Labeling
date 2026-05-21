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

public class DocumentParser {

    public record ParseResult(String text, int wordCount, long fileSizeBytes, String fileType) {}

    public ParseResult parse(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        long size = Files.size(path);
        String name = path.getFileName().toString().toLowerCase();

        String text;
        String type;

        if (name.endsWith(".docx")) {
            text = parseDocx(path);
            type = "docx";
        } else if (name.endsWith(".doc")) {
            text = parseDoc(path);
            type = "doc";
        } else if (name.endsWith(".xlsx")) {
            text = parseXlsx(path);
            type = "xlsx";
        } else if (name.endsWith(".xls")) {
            text = parseXls(path);
            type = "xls";
        } else if (name.endsWith(".txt") || name.endsWith(".csv")) {
            text = Files.readString(path);
            type = name.endsWith(".csv") ? "csv" : "txt";
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }

        return new ParseResult(text, countWords(text), size, type);
    }

    // DOCX
    private String parseDocx(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (InputStream in = Files.newInputStream(path);
             XWPFDocument doc = new XWPFDocument(in)) {

            for (XWPFParagraph p : doc.getParagraphs()) {
                if (p.getText() != null) {
                    sb.append(p.getText()).append("\n");
                }
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append(" ");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    // DOC (FIXED - requires poi-scratchpad)
    private String parseDoc(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {

            return String.join("\n", extractor.getParagraphText());
        }
    }

    // XLSX
    private String parseXlsx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             XSSFWorkbook wb = new XSSFWorkbook(in)) {

            return extractExcel(wb);
        }
    }

    // XLS
    private String parseXls(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             HSSFWorkbook wb = new HSSFWorkbook(in)) {

            return extractExcel(wb);
        }
    }

    private String extractExcel(Workbook wb) {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();

        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);

            sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");

            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(formatter.formatCellValue(cell)).append(" ");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    public String preview(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }
}
