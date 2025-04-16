package com.teamformation.util;

import com.teamformation.model.Student;
import com.teamformation.model.Team;
import com.teamformation.model.TeamFormationResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
            
            // Create team sheets
            if (result.getTeams() != null && !result.getTeams().isEmpty()) {
                // For SQL Bootcamp, separate teams by course type
                if (result.getEventType().name().contains("SQL")) {
                    List<Team> advancedTeams = result.getTeams().stream()
                            .filter(team -> team.getName().toLowerCase().contains("advanced"))
                            .toList();
                    
                    List<Team> fullTeams = result.getTeams().stream()
                            .filter(team -> team.getName().toLowerCase().contains("full"))
                            .toList();
                    
                    if (!advancedTeams.isEmpty()) {
                        createTeamSheet(workbook, "Advanced Course Teams", advancedTeams);
                    }
                    
                    if (!fullTeams.isEmpty()) {
                        createTeamSheet(workbook, "Full Course Teams", fullTeams);
                    }
                } else {
                    // For other events, just create a single teams sheet
                    createTeamSheet(workbook, result.getEventType().getDisplayName() + " Teams", result.getTeams());
                }
            }
            
            // Create unassigned students sheet if there are any
            if (result.getUnassignedStudents() != null && !result.getUnassignedStudents().isEmpty()) {
                createUnassignedStudentsSheet(workbook, result.getUnassignedStudents());
            }
            
            // Create summary sheet
            createSummarySheet(workbook, result);
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
    
    private static void createTeamSheet(Workbook workbook, String sheetName, List<Team> teams) {
        Sheet sheet = workbook.createSheet(sheetName);
        
        // Determine if this is a SQL Bootcamp sheet
        boolean isSqlBootcamp = sheetName.contains("Advanced Course") || sheetName.contains("Full Course");
        
        // Set column widths
        if (isSqlBootcamp) {
            sheet.setColumnWidth(0, 4000);  // Team Number
            sheet.setColumnWidth(1, 6000);  // Full Name
            sheet.setColumnWidth(2, 10000); // Email
            sheet.setColumnWidth(3, 4000);  // Track
            sheet.setColumnWidth(4, 4000);  // Batch No
            sheet.setColumnWidth(5, 6000);  // Course Type
        } else {
            sheet.setColumnWidth(0, 4000);  // Team name
            sheet.setColumnWidth(1, 6000);  // Student name
            sheet.setColumnWidth(2, 10000); // Email
            sheet.setColumnWidth(3, 4000);  // Track
            sheet.setColumnWidth(4, 4000);  // Batch
            sheet.setColumnWidth(5, 6000);  // Working Status
            sheet.setColumnWidth(6, 5000);  // Time Zone
        }
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFFont headerFont = ((XSSFWorkbook) workbook).createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(14);
        headerStyle.setFont(headerFont);
        
        // Add header cells - different headers for SQL Bootcamp
        String[] headers;
        if (isSqlBootcamp) {
            headers = new String[]{"Team Number", "Full Name", "Email", "Track", "Batch No", "Course Type"};
        } else {
            headers = new String[]{"Team", "Name", "Email", "Track", "Batch", "Working Status", "Time Zone"};
        }
        
        for (int i = 0; i < headers.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headers[i]);
            headerCell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        int rowNum = 1;
        
        CellStyle teamNameStyle = workbook.createCellStyle();
        XSSFFont teamNameFont = ((XSSFWorkbook) workbook).createFont();
        teamNameFont.setBold(true);
        teamNameStyle.setFont(teamNameFont);
        
        CellStyle teamStatStyle = workbook.createCellStyle();
        teamStatStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        teamStatStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            
            if (isSqlBootcamp) {
                // For SQL Bootcamp, use a different layout that matches the screenshot
                String teamNumber = team.getName().replaceAll("[^0-9]", ""); // Extract team number
                String teamPrefix;
                
                if (sheetName.contains("Advanced Course")) {
                    teamPrefix = "Advanced Team ";
                } else {
                    teamPrefix = "Full Course Team ";
                }
                
                // Add team info row with statistics (green background)
                Row teamInfoRow = sheet.createRow(rowNum++);
                
                Cell teamCell = teamInfoRow.createCell(0);
                int trackCount = team.getMembers().get(0).getTrack().equals("DA") ? team.getDaCount() : team.getSdetCount();
                int otherTrackCount = team.getMembers().get(0).getTrack().equals("DA") ? team.getSdetCount() : team.getDaCount();
                
                teamCell.setCellValue("Team " + teamNumber + " (" + team.getMembers().size() + " members, " + 
                        trackCount + " " + team.getMembers().get(0).getTrack() + ", " + 
                        otherTrackCount + " " + (team.getMembers().get(0).getTrack().equals("DA") ? "SDET" : "DA") + ")");
                
                teamCell.setCellStyle(teamStatStyle);
                
                // Span the team info across all columns
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
                
                // Add individual student rows
                for (Student student : team.getMembers()) {
                    Row row = sheet.createRow(rowNum++);
                    
                    // Team number (e.g., "1", "2", "3")
                    Cell cell = row.createCell(0);
                    cell.setCellValue(teamNumber);
                    
                    // Student full name
                    cell = row.createCell(1);
                    cell.setCellValue(student.getName() != null ? student.getName() : "");
                    
                    // Email
                    cell = row.createCell(2);
                    cell.setCellValue(student.getEmail() != null ? student.getEmail() : "");
                    
                    // Track
                    cell = row.createCell(3);
                    cell.setCellValue(student.getTrack() != null ? student.getTrack() : "");
                    
                    // Batch No
                    cell = row.createCell(4);
                    cell.setCellValue(student.getBatch() != null ? student.getBatch() : "");
                    
                    // Course Type
                    cell = row.createCell(5);
                    cell.setCellValue(student.getCourseType() != null ? student.getCourseType() : "");
                }
                
            } else {
                // Original format for non-SQL Bootcamp event types
                // Add team statistics row
                Row statRow = sheet.createRow(rowNum++);
                Cell statCell = statRow.createCell(0);
                statCell.setCellValue(team.getName());
                statCell.setCellStyle(teamNameStyle);
                
                // Add team statistics across the row
                statCell = statRow.createCell(1);
                statCell.setCellValue("Team Statistics:");
                statCell.setCellStyle(teamStatStyle);
                
                statCell = statRow.createCell(2);
                statCell.setCellValue(team.getStatistics() != null ? team.getStatistics() : "");
                statCell.setCellStyle(teamStatStyle);
                
                // Add student rows
                for (Student student : team.getMembers()) {
                    Row row = sheet.createRow(rowNum++);
                    
                    // Team name (only for first student)
                    Cell cell = row.createCell(0);
                    cell.setCellValue(""); // Leave blank for non-first rows
                    
                    // Student name
                    cell = row.createCell(1);
                    cell.setCellValue(student.getName() != null ? student.getName() : "");
                    
                    // Email
                    cell = row.createCell(2);
                    cell.setCellValue(student.getEmail() != null ? student.getEmail() : "");
                    
                    // Track
                    cell = row.createCell(3);
                    cell.setCellValue(student.getTrack() != null ? student.getTrack() : "");
                    
                    // Batch
                    cell = row.createCell(4);
                    cell.setCellValue(student.getBatch() != null ? student.getBatch() : "");
                    
                    // Working Status
                    cell = row.createCell(5);
                    cell.setCellValue(student.getWorkingStatus() != null ? student.getWorkingStatus() : "");
                    
                    // Time Zone
                    cell = row.createCell(6);
                    cell.setCellValue(student.getTimeZone() != null ? student.getTimeZone() : "");
                }
            }
            
            // Add empty row between teams
            sheet.createRow(rowNum++);
        }
    }
    
    private static void createUnassignedStudentsSheet(Workbook workbook, List<Student> students) {
        Sheet sheet = workbook.createSheet("Unassigned Students");
        
        // Set column widths
        sheet.setColumnWidth(0, 6000);  // Name
        sheet.setColumnWidth(1, 10000); // Email
        sheet.setColumnWidth(2, 4000);  // Track
        sheet.setColumnWidth(3, 4000);  // Batch
        sheet.setColumnWidth(4, 6000);  // Working Status
        sheet.setColumnWidth(5, 5000);  // Time Zone
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        XSSFFont headerFont = ((XSSFWorkbook) workbook).createFont();
        headerFont.setBold(true);
        headerFont.setFontHeight(14);
        headerStyle.setFont(headerFont);
        
        // Add header cells
        String[] headers = {"Name", "Email", "Track", "Batch", "Working Status", "Time Zone"};
        for (int i = 0; i < headers.length; i++) {
            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headers[i]);
            headerCell.setCellStyle(headerStyle);
        }
        
        // Create data rows
        int rowNum = 1;
        
        for (Student student : students) {
            Row row = sheet.createRow(rowNum++);
            
            // Student name
            Cell cell = row.createCell(0);
            cell.setCellValue(student.getName() != null ? student.getName() : "");
            
            // Email
            cell = row.createCell(1);
            cell.setCellValue(student.getEmail() != null ? student.getEmail() : "");
            
            // Track
            cell = row.createCell(2);
            cell.setCellValue(student.getTrack() != null ? student.getTrack() : "");
            
            // Batch
            cell = row.createCell(3);
            cell.setCellValue(student.getBatch() != null ? student.getBatch() : "");
            
            // Working Status
            cell = row.createCell(4);
            cell.setCellValue(student.getWorkingStatus() != null ? student.getWorkingStatus() : "");
            
            // Time Zone
            cell = row.createCell(5);
            cell.setCellValue(student.getTimeZone() != null ? student.getTimeZone() : "");
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
        
        // Total teams
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Total Teams");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getTeams() != null ? result.getTeams().size() : 0);
        
        // Total students
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Total Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getTotalStudents());
        
        // Assigned students
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Assigned Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getAssignedStudents());
        
        // Unassigned students
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Unassigned Students");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getUnassignedStudents() != null ? result.getUnassignedStudents().size() : 0);
        
        // Assignment rate
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Assignment Rate");
        cell.setCellStyle(headerStyle);
        
        cell = row.createCell(1);
        cell.setCellValue(result.getAssignmentRate());
        
        // Add empty row
        rowNum++;
        
        // Summary text
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Summary");
        cell.setCellStyle(headerStyle);
        
        if (result.getSummary() != null && !result.getSummary().isEmpty()) {
            String[] summaryLines = result.getSummary().split("\n");
            for (String line : summaryLines) {
                row = sheet.createRow(rowNum++);
                cell = row.createCell(0);
                cell.setCellValue(line);
            }
        }
    }
}
