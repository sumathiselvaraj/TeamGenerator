package com.teamformation.exception;

/**
 * Custom exception for Excel formula cell processing errors
 */
public class ExcelFormulaException extends RuntimeException {
    private final String sheetName;
    private final String cellReference;
    private final String formula;
    private final int rowIndex;
    private final int columnIndex;

    public ExcelFormulaException(String message, String sheetName, String cellReference, 
                                String formula, int rowIndex, int columnIndex) {
        super(message);
        this.sheetName = sheetName;
        this.cellReference = cellReference;
        this.formula = formula;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " in cell " + cellReference + 
               " (Sheet: " + sheetName + ", Row: " + (rowIndex + 1) + 
               ", Column: " + getColumnName(columnIndex) + ")";
    }

    /**
     * Converts a 0-based column index to Excel column name (A, B, C, ..., Z, AA, AB, etc.)
     */
    private String getColumnName(int columnIndex) {
        StringBuilder columnName = new StringBuilder();
        
        while (columnIndex >= 0) {
            int remainder = columnIndex % 26;
            columnName.insert(0, (char) (remainder + 'A'));
            columnIndex = (columnIndex / 26) - 1;
        }
        
        return columnName.toString();
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getCellReference() {
        return cellReference;
    }

    public String getFormula() {
        return formula;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColumnIndex() {
        return columnIndex;
    }
}