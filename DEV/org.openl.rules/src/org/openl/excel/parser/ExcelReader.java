package org.openl.excel.parser;

import java.util.List;

public interface ExcelReader extends AutoCloseable {
    /**
     * Get all sheet descriptors
     */
    List<? extends SheetDescriptor> getSheets() throws ExcelParseException;

    /**
     * Parse and get all cells from a given sheet
     *
     * @param sheet sheet to parse
     * @return parsed objects with types as in Excel
     */
    Object[][] getCells(SheetDescriptor sheet) throws ExcelParseException;

    /**
     * Sometimes we need to convert parsed double value to date.
     * For example a cell contains value 1.25, user sees it in Excel as 1.25 but in OpenL this value has a type Date.
     * It should be converted from double to Date. (There is unit test for such case)
     * We should get this property from workbook and use it in DateUtil.getJavaDate(double, boolean) to convert it correctly.
     *
     * @return The setting for a given workbook
     */
    boolean isUse1904Windowing() throws ExcelParseException;

    /**
     * Close ExcelReader and release resources.
     */
    @Override
    void close();
}
