package com.teamformation.util;

import com.teamformation.model.Student;
import com.teamformation.model.Team;
import com.teamformation.model.TeamFormationResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelGenerator {

    public static ByteArrayInputStream generateExcel(TeamFormationResult result) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Create sheet for Advanced Course Teams
            if (!result.getAdvancedCourseTeams().isEmpty()) {
                createTeamSheet(workbook, "Advanced Course Teams", result.getAdvancedCourseTeams());
            }
            
            // Create sheet for Full Course Teams
            if (!result.getFullCourseTeams().isEmpty()) {
                createTeamSheet(workbook, "Full Course Teams", result.getFullCourseTeams());
            }
            
            // Create summary sheet
            createSummarySheet(workbook, result);
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
    
    private static void createTeamSheet(Workbook workbook, String sheetName, List<Team> teams) {
        Sheet sheet = workbook.createSheet(sheetName);
        
        // Set column widths
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 10000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 4000);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFFont headerFont = ((XSSFWorkbook) workbook).createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(14);
        headerStyle.setFont(headerFont);
        
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Team");
        headerCell.setCellStyle(headerStyle);
        
        headerCell = headerRow.createCell(1);
        headerCell.setCellValue("Name");
        headerCell.setCellStyle(headerStyle);
        
        headerCell = headerRow.createCell(2);
        headerCell.setCellValue("Email");
        headerCell.setCellStyle(headerStyle);
        
        headerCell = headerRow.createCell(3);
        headerCell.setCellValue("Track");
        headerCell.setCellStyle(headerStyle);
        
        headerCell = headerRow.createCell(4);
        headerCell.setCellValue("Batch");
        headerCell.setCellStyle(headerStyle);
        
        // Create data rows
        int rowNum = 1;
        
        CellStyle teamNameStyle = workbook.createCellStyle();
        XSSFFont teamNameFont = ((XSSFWorkbook) workbook).createFont();
        teamNameFont.setBold(true);
        teamNameStyle.setFont(teamNameFont);
        
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            
            for (int j = 0; j < team.getStudents().size(); j++) {
                Student student = team.getStudents().get(j);
                Row row = sheet.createRow(rowNum++);
                
                Cell cell = row.createCell(0);
                if (j == 0) {
                    cell.setCellValue(team.getName());
                    cell.setCellStyle(teamNameStyle);
                }
                
                cell = row.createCell(1);
                cell.setCellValue(student.getName());
                
                cell = row.createCell(2);
                cell.setCellValue(student.getEmail());
                
                cell = row.createCell(3);
                cell.setCellValue(student.getTrack());
                
                cell = row.createCell(4);
                cell.setCellValue(student.getBatch());
            }
            
            // Add empty row between teams
            sheet.createRow(rowNum++);
        }
    }
    
    private static void createSummarySheet(Workbook workbook, TeamFormationResult result) {
        Sheet sheet = workbook.createSheet("Summary");
        
        // Set column widths
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFFont headerFont = ((XSSFWorkbook) workbook).createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(14);
        headerStyle.setFont(headerFont);
        
        // Create summary rows
        int rowNum = 0;
        
        // Event Type
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue("Event Type");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getEventType().getDisplayName());
        
        // Add empty row
        rowNum++;
        
        // Advanced Course Summary
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Advanced Course Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getAdvancedCourseStudentsCount());
        
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Advanced Course Teams");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getAdvancedCourseTeamsCount());
        
        // Add empty row
        rowNum++;
        
        // Full Course Summary
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Full Course Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getFullCourseStudentsCount());
        
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Full Course Teams");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getFullCourseTeamsCount());
        
        // Add empty row
        rowNum++;
        
        // Total Summary
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Total Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getTotalStudentsCount());
        
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Total Teams");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getTotalTeamsCount());
    }
}
