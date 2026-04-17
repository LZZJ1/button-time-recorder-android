package com.example.buttontimerecorder;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel 导入/导出工具类
 * 对应原Python中的export_to_excel和import_from_excel方法
 *
 * 导出格式：
 * 表头列: 计划序号, 作业站场, 作业路线, 计划量, 开始时间, 作业时间, 使用高铁维修线标识, 使用量, 作业相关设备信息, 对应占用区段名称, 占用时间（准确时间）
 *
 * 导入格式：
 * D列（作业站场）和 L列（对应占用区段名称），从第4行开始读取
 * 按钮名称 = 作业站场 + 对应占用区段名称
 */
public class ExcelHelper {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    /**
     * 导出记录到Excel文件
     * @param context     上下文
     * @param records     记录列表
     * @param buttons     按钮列表（用于分组）
     * @param outputFile  输出文件
     * @return 是否成功
     */
    public static boolean exportToExcel(Context context,
                                        List<TimeRecord> records,
                                        List<ToggleButtonView> buttons,
                                        File outputFile) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet ws = wb.createSheet("作业时间记录");

        // ---- 样式：表头 ----
        XSSFFont headerFont = wb.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x2c, (byte) 0x3e, (byte) 0x50}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        setBorder(wb, headerStyle);

        // ---- 样式：数据行 ----
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(wb, cellStyle);

        // ---- 表头 ----
        String[] headers = {
            "计划序号", "作业站场", "作业路线", "计划量",
            "开始时间", "作业时间", "使用高铁维修线标识",
            "使用量", "作业相关设备信息", "对应占用区段名称", "占用时间（准确时间）"
        };
        int[] colWidths = {12, 15, 12, 10, 12, 12, 18, 10, 20, 18, 30};

        Row headerRow = ws.createRow(0);
        headerRow.setHeightInPoints(25);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            ws.setColumnWidth(i, colWidths[i] * 256);
        }

        // ---- 按按钮分组数据 ----
        // 建立 按钮名 -> List<TimeRecord> 的映射
        Map<String, List<TimeRecord>> buttonTimes = new LinkedHashMap<>();
        for (ToggleButtonView btn : buttons) {
            buttonTimes.put(btn.getName(), new ArrayList<>());
        }
        for (TimeRecord r : records) {
            if (buttonTimes.containsKey(r.getName())) {
                buttonTimes.get(r.getName()).add(r);
            }
        }

        String today = DATE_FMT.format(new Date());
        int rowIdx = 1;

        for (Map.Entry<String, List<TimeRecord>> entry : buttonTimes.entrySet()) {
            String btnName = entry.getKey();
            List<TimeRecord> times = entry.getValue();

            // 拆分按钮名称：格式 "作业站场占用区段名称"
            // 尝试用 + 分割
            String stationName = btnName;
            String sectionName = "";
            if (btnName.contains("+")) {
                String[] parts = btnName.split("\\+", 2);
                stationName = parts[0].trim();
                sectionName = parts[1].trim();
            }

            // 收集所有时间字符串
            StringBuilder allTimes = new StringBuilder();
            for (TimeRecord r : times) {
                String pressHm = formatHm(r.getPressFullTime());
                String releaseHm = r.getReleaseTime() != null && !r.getReleaseTime().isEmpty()
                        ? formatHm(r.getReleaseFullTime())
                        : "-未弹起";

                String timeStr = pressHm + " " + releaseHm;
                if (allTimes.length() > 0) allTimes.append("\n");
                allTimes.append(timeStr);
            }

            Row row = ws.createRow(rowIdx);
            row.setHeightInPoints(times.isEmpty() ? 20 : Math.max(20, times.size() * 18));

            // 列：计划序号, 作业站场, 作业路线, 计划量, 开始时间, 作业时间,
            //      使用高铁维修线标识, 使用量, 作业相关设备信息, 对应占用区段名称, 占用时间
            setCell(ws, row, 0, "", cellStyle);
            setCell(ws, row, 1, stationName, cellStyle);
            setCell(ws, row, 2, "", cellStyle);
            setCell(ws, row, 3, "", cellStyle);
            setCell(ws, row, 4, today, cellStyle);
            setCell(ws, row, 5, allTimes.toString(), cellStyle);
            setCell(ws, row, 6, "", cellStyle);
            setCell(ws, row, 7, "", cellStyle);
            setCell(ws, row, 8, "", cellStyle);
            setCell(ws, row, 9, sectionName, cellStyle);
            setCell(ws, row, 10, allTimes.toString(), cellStyle);

            rowIdx++;
        }

        // 写文件
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            wb.write(fos);
        }
        wb.close();
        return true;
    }

    /**
     * 从Excel导入按钮名称
     * D列（作业站场）和 L列（对应占用区段名称），从第4行开始读取
     * @return 导入的按钮名称列表
     */
    public static List<String> importFromExcel(Context context, Uri uri) throws Exception {
        List<String> buttonNames = new ArrayList<>();

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("无法打开文件");

            Workbook wb = WorkbookFactory.create(is);
            Sheet ws = wb.getSheetAt(0);

            // 从第4行（index=3）开始读取
            for (int rowNum = 3; rowNum <= ws.getLastRowNum(); rowNum++) {
                Row row = ws.getRow(rowNum);
                if (row == null) continue;

                // D列 = index 3（作业站场）
                String station = getCellString(row.getCell(3));
                // L列 = index 11（对应占用区段名称）
                String section = getCellString(row.getCell(11));

                if (!station.isEmpty() || !section.isEmpty()) {
                    String btnName = station + (section.isEmpty() ? "" : "+" + section);
                    if (!btnName.trim().isEmpty() && !buttonNames.contains(btnName)) {
                        buttonNames.add(btnName.trim());
                    }
                }
            }
            wb.close();
        }
        return buttonNames;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private static String formatHm(String fullTime) {
        if (fullTime == null || fullTime.isEmpty()) return "—";
        if (fullTime.length() >= 16 && fullTime.contains(" ")) {
            return fullTime.substring(11, 16);
        }
        return fullTime;
    }

    private static void setCell(XSSFSheet ws, Row row, int col, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setBorder(XSSFWorkbook wb, XSSFCellStyle style) {
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
    }
}
