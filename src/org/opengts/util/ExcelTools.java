// ----------------------------------------------------------------------------
// Copyright 2007-2014, OpenGTS
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;
import java.net.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.xssf.usermodel.*;

public class ExcelTools
{

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final int     IndexColor_NONE             = -1;	
	public static final int     IndexColor_YELLOW           = IndexedColors.YELLOW.getIndex();
    public static final int     IndexColor_WHITE            = IndexedColors.WHITE.getIndex();
    public static final int     IndexColor_BLACK            = IndexedColors.BLACK.getIndex();
    public static final int     IndexColor_GRAY25           = IndexedColors.GREY_25_PERCENT.getIndex();
    public static final int     IndexColor_GRAY50           = IndexedColors.GREY_50_PERCENT.getIndex();
    
    public static int getColorIndex(String name)
    {
        if (StringTools.isBlank(name) || name.equalsIgnoreCase("NONE")) {
            return -1;
        } else {
            IndexedColors color = EnumTools.getValueOf(IndexedColors.class, name);
            if (color != null) {
                return color.getIndex();
            } else {
                return -1;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String STYLE_TITLE      = "title";
    public static final String STYLE_SUBTITLE   = "subtitle";
    public static final String STYLE_HEADER     = "header";
    public static final String STYLE_BODY       = "body";
    public static final String STYLE_SUBTOTAL   = "subtotal";
    public static final String STYLE_TOTAL      = "total";
    public static final String STYLE_BLANK      = "blank";

    public static CellStyle createStyle(Workbook wb,
        int fontPoint, int boldWeight, int fontColor,
        int horzAlign, int vertAlign,
        boolean wrapText,
        int fillColor, int fillPattern,
        int borderTopColor, int borderRightColor, int borderBottomColor, int borderLeftColor
        )
    {

        /* style */
        CellStyle style = wb.createCellStyle();

        /* font */
        Font font = wb.createFont();
        if (fontPoint >= 0) {
            font.setFontHeightInPoints((short)fontPoint);
        }
        if (boldWeight >= 0) {
            font.setBoldweight((short)boldWeight);
        }
        if (fontColor >= 0) {
            font.setColor((short)fontColor);
        }
        style.setFont(font);

        /* text wrap */
        style.setWrapText(wrapText);

        /* alignment */
        if (horzAlign >= 0) {
            style.setAlignment((short)horzAlign);
        }
        if (vertAlign >= 0) {
            style.setVerticalAlignment((short)vertAlign);
        }

        /* fill */
        if (fillColor >= 0) {
            style.setFillForegroundColor((short)fillColor);
        }
        if (fillPattern >= 0) {
            style.setFillPattern((short)fillPattern);
        }

        /* border */
        if (borderTopColor >= 0) {
            style.setBorderTop((short)ExcelAPI.CellStyle_BORDER_THIN);
            style.setTopBorderColor((short)borderTopColor);
        }
        if (borderRightColor >= 0) {
            style.setBorderRight((short)ExcelAPI.CellStyle_BORDER_THIN);
            style.setRightBorderColor((short)borderRightColor);
        }
        if (borderBottomColor >= 0) {
            style.setBorderBottom((short)ExcelAPI.CellStyle_BORDER_THIN);
            style.setBottomBorderColor((short)borderBottomColor);
        }
        if (borderLeftColor >= 0) {
            style.setBorderLeft((short)ExcelAPI.CellStyle_BORDER_THIN);
            style.setLeftBorderColor((short)borderLeftColor);
        }
        
        /* return */
        return style;

    }

    private static Map<String,CellStyle> createStyleMap(Workbook wb)
    {
        Map<String,CellStyle> styleMap = new HashMap<String,CellStyle>();

        // "title"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_18, ExcelAPI.Font_BOLDWEIGHT_BOLD, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_WHITE, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK
                );
            styleMap.put(STYLE_TITLE,style);
        }

        // "subtitle"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_12, ExcelAPI.Font_BOLDWEIGHT_NORMAL, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_WHITE, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK
                );
            styleMap.put(STYLE_SUBTITLE,style);
        }

        // "header"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_11, ExcelAPI.Font_BOLDWEIGHT_NORMAL, ExcelTools.IndexColor_WHITE,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_GRAY50, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE
                );
            styleMap.put(STYLE_HEADER,style);
        }

        // "body"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_11, ExcelAPI.Font_BOLDWEIGHT_NORMAL, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_WHITE, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_BLACK
                );
            styleMap.put(STYLE_BODY,style);
        }

        // "subtotal"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_11, ExcelAPI.Font_BOLDWEIGHT_BOLD, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_GRAY25, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE
                );
            styleMap.put(STYLE_SUBTOTAL,style);
        }

        // "total"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_11, ExcelAPI.Font_BOLDWEIGHT_BOLD, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_GRAY25, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_BLACK, ExcelTools.IndexColor_NONE
                );
            styleMap.put(STYLE_TOTAL,style);
        }

        // "blank"
        {
            CellStyle style = ExcelTools.createStyle(wb, 
                ExcelAPI.Font_POINT_11, ExcelAPI.Font_BOLDWEIGHT_NORMAL, ExcelTools.IndexColor_BLACK,
                ExcelAPI.CellStyle_ALIGN_CENTER, ExcelAPI.CellStyle_VERTICAL_CENTER,
                ExcelAPI.Text_WRAP,
                ExcelTools.IndexColor_WHITE, ExcelAPI.CellStyle_SOLID_FOREGROUND,
                ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE, ExcelTools.IndexColor_NONE
                );
            styleMap.put(STYLE_BLANK,style);
        }

        /* return */
        return styleMap;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class Spreadsheet
        implements ExcelAPI
    {
        private String                      name = null;
        private Workbook                    wb = null;
        private Sheet                       sheet = null;
        private Map<String,CellStyle>       styleMap = null;
        private String                      extn = null;
        
        private Map<Integer,Row>            headerRowMap = new HashMap<Integer,Row>();
        private Map<Integer,Row>            bodyRowMap = new HashMap<Integer,Row>();

        public Spreadsheet() {
            //
        }

        public void init(boolean xlsx, String name) {

            /* workbook type */
            if (xlsx) {
                Print.logInfo("Initializing Excel 'XLSX' Workbook ...");
                try {
                    // performed in this manner in case "XLSX" support is not included
                    this.wb = (Workbook)Class.forName("org.apache.poi.xssf.usermodel.XSSFWorkbook").newInstance(); // new XSSFWorkbook();
                    this.extn = "xlsx";
                } catch (Throwable th) {
                    Print.logError("Excel 'xlsx' not supported");
                    return;
                }
            } else {
                Print.logInfo("Initializing Excel 'XLS' Workbook ...");
                this.wb = new HSSFWorkbook();
                this.extn = "xls";
            }

            /* Sheet init */
            this.name = name;
            this.sheet = this.wb.createSheet(this.name);
            PrintSetup printSetup = this.sheet.getPrintSetup();
            printSetup.setLandscape(true);
            this.sheet.setFitToPage(true);
            this.sheet.setHorizontallyCenter(true);

            /* init styles */
            this.styleMap = ExcelTools.createStyleMap(this.wb);

        }

        public void setTitle(int rowIndex, String title, int colSpan) {

            /* title row */
            Row titleRow = this.sheet.createRow(rowIndex);
            titleRow.setHeightInPoints(30); // sheet.getDefaultRowHeightInPoints()
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(this.styleMap.get(STYLE_TITLE));

            /* span title across all columns */
            //String mergeRegion = "$A$1:$" + (char)('A' + colSpan - 1) + "$1";
            //Print.logInfo("MergeRegion: " + mergeRegion);
            //this.sheet.addMergedRegion(CellRangeAddress.valueOf(mergeRegion));
            this.sheet.addMergedRegion(new CellRangeAddress(rowIndex,rowIndex,0,(colSpan-1)));

        }

        public void setSubtitle(int rowIndex, String title, int colSpan) {

            /* title row */
            Row titleRow = this.sheet.createRow(rowIndex);
            titleRow.setHeightInPoints(30); // sheet.getDefaultRowHeightInPoints()
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(this.styleMap.get(STYLE_SUBTITLE));

            /* span title across all columns */
            //String mergeRegion = "$A$1:$" + (char)('A' + colSpan - 1) + "$1";
            //Print.logInfo("MergeRegion: " + mergeRegion);
            //this.sheet.addMergedRegion(CellRangeAddress.valueOf(mergeRegion));
            this.sheet.addMergedRegion(new CellRangeAddress(rowIndex,rowIndex,0,(colSpan-1)));

        }

        public void setBlankRow(int rowIndex, int colSpan) {

            /* blank row */
            Row blankRow = this.sheet.createRow(rowIndex);
            blankRow.setHeightInPoints(14); // sheet.getDefaultRowHeightInPoints()
            Cell blankCell = blankRow.createCell(0);
            blankCell.setCellValue("");
            blankCell.setCellStyle(this.styleMap.get(STYLE_BLANK));

            /* span blank across all columns */
            //String mergeRegion = "$A$1:$" + (char)('A' + colSpan - 1) + "$1";
            //Print.logInfo("MergeRegion: " + mergeRegion);
            //this.sheet.addMergedRegion(CellRangeAddress.valueOf(mergeRegion));
            this.sheet.addMergedRegion(new CellRangeAddress(rowIndex,rowIndex,0,(colSpan-1)));

        }

        public void addHeaderColumn(int rowIndex, int colIndex, String colTitle, int charWidth) {

            /* get/create header row */
            Integer rowNdxI = new Integer(rowIndex);
            Row headerRow = this.headerRowMap.get(rowNdxI);
            if (headerRow == null) {
                headerRow = this.sheet.createRow(rowIndex);
                headerRow.setHeightInPoints(36);
                this.headerRowMap.put(rowNdxI,headerRow);
            }

            /* column */
            int hc = colIndex;
            Cell headerCell = headerRow.createCell(hc);
            headerCell.setCellStyle(this.styleMap.get(STYLE_HEADER));
            headerCell.setCellValue(colTitle);
            //this.sheet.setColumnWidth(hc, charWidth * 256); // 15 chars
            this.sheet.autoSizeColumn(hc);

        }

        private void _addBodyColumn(int rowIndex, int colIndex, String style, int ptHeight, Object value) {

            /* get/create body row */
            Integer rowNdxI = new Integer(rowIndex);
            Row bodyRow = this.bodyRowMap.get(rowNdxI);
            if (bodyRow == null) {
                bodyRow = this.sheet.createRow(rowIndex);
                bodyRow.setHeightInPoints(ptHeight);
                this.bodyRowMap.put(rowNdxI,bodyRow);
            }

            /* column */
            int bc = colIndex;
            Cell bodyCell = bodyRow.createCell(bc);
            bodyCell.setCellStyle(this.styleMap.get(style));
            if (value instanceof Double) {
                bodyCell.setCellValue(((Double)value).doubleValue());
            } else
            if (value instanceof Float) {
                bodyCell.setCellValue(((Float)value).floatValue());
            } else
            if (value instanceof Long) {
                bodyCell.setCellValue(((Long)value).longValue());
            } else
            if (value instanceof Integer) {
                bodyCell.setCellValue(((Integer)value).intValue());
            } else
            if (value instanceof Short) {
                bodyCell.setCellValue(((Short)value).shortValue());
            } else {
                bodyCell.setCellValue(StringTools.trim(value));
            }
            this.sheet.autoSizeColumn(bc);

        }

        public void addBodyColumn(int rowIndex, int colIndex, Object value) {
            this._addBodyColumn(rowIndex, colIndex, STYLE_BODY, 12, value);
        }

        public void addSubtotalColumn(int rowIndex, int colIndex, Object value) {
            this._addBodyColumn(rowIndex, colIndex, STYLE_SUBTOTAL, 16, value);
        }

        public void addTotalColumn(int rowIndex, int colIndex, Object value) {
            this._addBodyColumn(rowIndex, colIndex, STYLE_TOTAL, 16, value);
        }

        public Workbook getWorkbook() {
            return this.wb;
        }
        
        public String getExtension() {
            return this.extn;
        }
        
        public boolean write(File dir) {
            boolean ok = false;
            FileOutputStream fout = null;
            try {
                String outName = this.name + "." + this.getExtension();
                File outFile = (dir != null)? new File(dir,outName) : new File(outName);
                Print.logInfo("Saving Excel spreadsheet to file: " + outFile);
                fout = new FileOutputStream(outFile);
                this.wb.write(fout);
                ok = true;
            } catch (IOException ioe) {
                Print.logException("XLS save error",ioe);
                ok = false;
            } finally {
                try { fout.close(); } catch (Throwable th) {}
            }
            return ok;
        }
        
        public boolean write(OutputStream out) {
            boolean ok = false;
            try {
                Print.logInfo("Saving Spreadsheet to OutputStream");
                this.wb.write(out);
                ok = true;
            } catch (IOException ioe) {
                Print.logException("XLS save error", ioe);
                ok = false;
            }
            return ok;
        }

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String ARG_XLS[]      = new String[] { "xls" };
    public static String ARG_XLSX[]     = new String[] { "xlsx" };
    
    public static void main(String argv[]) 
    {
        RTConfig.setCommandLineArgs(argv);
        boolean xls  = RTConfig.getBoolean(ARG_XLS ,false);
        boolean xlsx = RTConfig.getBoolean(ARG_XLSX,false);
        Print.logInfo("XLS  = " + xls );
        Print.logInfo("XLSX = " + xlsx);
        if (!xls) { xlsx = true; }
        Print.logInfo("XLSX = " + xlsx);

        /* Spreadsheet */
        ExcelAPI ss = new Spreadsheet();
        ss.init(xlsx, "reportSampe3");
        String columns[] = new String[] { "Date\nTime", "Device", "Latitude", "Longitude" };
        ss.setTitle(0, "Sample Report", columns.length);
        for (int hc = 0; hc < columns.length; hc++) {
            ss.addHeaderColumn(1, hc, columns[hc], 15);
        }

        /* data */
        Object value[] = null;
        value = new Object[] { "2011/01/03", "mydevice"  , new Double(38.1234), new Double(-142.1234) };
        for (int i = 0; i < value.length; i++) { ss.addBodyColumn(2, i, value[i]); }
        value = new Object[] { "2011/01/04", "yourdevice", new Double(38.4321), new Double(-142.4321) };
        for (int i = 0; i < value.length; i++) { ss.addBodyColumn(3, i, value[i]); }

        /* write */
        ss.write((File)null);
    }
    
}
