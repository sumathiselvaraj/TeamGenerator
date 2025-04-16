package com.teamformation.service;

import com.teamformation.model.EventType;
import com.teamformation.model.Student;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class ExcelService {

    public List<Student> parseExcelFile(InputStream inputStream, EventType eventType) throws Exception {
        List<Student> students = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Get column indices from header row
            int timestampIdx = -1;
            int emailIdx = -1;
            int nameIdx = -1;
            int trackIdx = -1;
            int batchIdx = -1;
            int courseTypeIdx = -1;
            int workingStatusIdx = -1;
            int timeZoneIdx = -1;
            int dsAlgoCompletionIdx = -1;
            int previousHackathonIdx = -1;
            
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new Exception("Excel file is empty or does not contain a header row");
            }
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String header = cell.getStringCellValue().trim().toLowerCase();
                    
                    if (header.contains("timestamp")) {
                        timestampIdx = i;
                    } else if (header.contains("email")) {
                        emailIdx = i;
                    } else if (header.contains("name") && !header.contains("user")) {
                        nameIdx = i;
                    } else if (header.contains("track")) {
                        trackIdx = i;
                    } else if (header.contains("batch")) {
                        batchIdx = i;
                    } else if (header.contains("course type") || header.contains("course_type")) {
                        courseTypeIdx = i;
                    } else if (header.contains("working")) {
                        workingStatusIdx = i;
                    } else if (header.contains("time zone")) {
                        timeZoneIdx = i;
                    } else if (header.contains("dsalgo") || header.contains("ds algo")) {
                        dsAlgoCompletionIdx = i;
                    } else if (header.contains("previous") && header.contains("hackathon")) {
                        previousHackathonIdx = i;
                    }
                }
            }
            
            // Validate required columns based on event type
            if (emailIdx == -1 || nameIdx == -1) {
                throw new Exception("Required columns (Email, Name) missing in the Excel file");
            }
            
            if (eventType == EventType.SQL_BOOTCAMP) {
                if (trackIdx == -1 || courseTypeIdx == -1) {
                    throw new Exception("Required columns (Track, Course Type) missing for SQL Bootcamp");
                }
            } else if (eventType == EventType.PHASE1_GHERKIN_HACKATHON || eventType == EventType.PHASE2_SELENIUM_HACKATHON) {
                if (trackIdx == -1 || workingStatusIdx == -1 || timeZoneIdx == -1) {
                    throw new Exception("Required columns (Track with Batch No, Working Status, Time Zone) missing for Hackathon");
                }
            }
            
            // Parse data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Student student = new Student();
                    
                    // Parse timestamp if available
                    if (timestampIdx >= 0) {
                        Cell cell = row.getCell(timestampIdx);
                        if (cell != null) {
                            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                student.setTimestamp(cell.getDateCellValue().toString());
                            } else {
                                student.setTimestamp(getCellValueAsString(cell));
                            }
                        }
                    }
                    
                    // Parse email
                    Cell emailCell = row.getCell(emailIdx);
                    if (emailCell == null || emailCell.getCellType() == CellType.BLANK) {
                        continue; // Skip rows without email
                    }
                    student.setEmail(getCellValueAsString(emailCell));
                    
                    // Parse name
                    Cell nameCell = row.getCell(nameIdx);
                    if (nameCell == null || nameCell.getCellType() == CellType.BLANK) {
                        continue; // Skip rows without name
                    }
                    student.setName(getCellValueAsString(nameCell));
                    
                    // Parse based on event type
                    if (eventType == EventType.SQL_BOOTCAMP) {
                        // Parse track for SQL Bootcamp
                        if (trackIdx >= 0) {
                            Cell trackCell = row.getCell(trackIdx);
                            if (trackCell != null) {
                                String track = getCellValueAsString(trackCell).trim();
                                // Standardize track values
                                if (track.toUpperCase().contains("SDET")) {
                                    student.setTrack("SDET");
                                } else if (track.toUpperCase().contains("DA")) {
                                    student.setTrack("DA");
                                } else {
                                    student.setTrack(track);
                                }
                            } else {
                                student.setTrack("Unknown");
                            }
                        }
                        
                        // Parse batch
                        if (batchIdx >= 0) {
                            Cell batchCell = row.getCell(batchIdx);
                            if (batchCell != null) {
                                student.setBatch(getCellValueAsString(batchCell));
                            }
                        }
                        
                        // Parse course type
                        Cell courseTypeCell = row.getCell(courseTypeIdx);
                        if (courseTypeCell != null) {
                            student.setCourseType(getCellValueAsString(courseTypeCell));
                        } else {
                            continue; // Skip rows without course type
                        }
                    } 
                    else if (eventType == EventType.PHASE1_GHERKIN_HACKATHON || eventType == EventType.PHASE2_SELENIUM_HACKATHON) {
                        // Parse track with batch for Hackathons
                        if (trackIdx >= 0) {
                            Cell trackWithBatchCell = row.getCell(trackIdx);
                            if (trackWithBatchCell != null) {
                                String trackWithBatch = getCellValueAsString(trackWithBatchCell).trim();
                                
                                // Extract track and batch from combined field
                                if (trackWithBatch.toUpperCase().contains("SDET")) {
                                    student.setTrack("SDET");
                                    // Try to extract batch number
                                    String batchStr = trackWithBatch.replaceAll("(?i).*?SDET\\s*", "").trim();
                                    student.setBatch(batchStr);
                                } else if (trackWithBatch.toUpperCase().contains("DA")) {
                                    student.setTrack("DA");
                                    // Try to extract batch number
                                    String batchStr = trackWithBatch.replaceAll("(?i).*?DA\\s*", "").trim();
                                    student.setBatch(batchStr);
                                } else {
                                    student.setTrack(trackWithBatch);
                                }
                            } else {
                                student.setTrack("Unknown");
                            }
                        }
                        
                        // Parse working status
                        if (workingStatusIdx >= 0) {
                            Cell workingCell = row.getCell(workingStatusIdx);
                            if (workingCell != null) {
                                student.setWorkingStatus(getCellValueAsString(workingCell));
                            }
                        }
                        
                        // Parse time zone
                        if (timeZoneIdx >= 0) {
                            Cell timeZoneCell = row.getCell(timeZoneIdx);
                            if (timeZoneCell != null) {
                                student.setTimeZone(getCellValueAsString(timeZoneCell));
                            }
                        }
                        
                        // Parse DSAlgo completion status
                        if (dsAlgoCompletionIdx >= 0) {
                            Cell dsAlgoCell = row.getCell(dsAlgoCompletionIdx);
                            if (dsAlgoCell != null) {
                                student.setDsAlgoCompletion(getCellValueAsString(dsAlgoCell));
                            }
                        }
                        
                        // Parse previous hackathon participation
                        if (previousHackathonIdx >= 0) {
                            Cell previousHackathonCell = row.getCell(previousHackathonIdx);
                            if (previousHackathonCell != null) {
                                student.setPreviousHackathon(getCellValueAsString(previousHackathonCell));
                            }
                        }
                        
                        // For hackathons, use event type as course type to maintain compatibility
                        student.setCourseType(eventType.getDisplayName());
                    }
                    
                    students.add(student);
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", i, e.getMessage());
                    // Continue with next row
                }
            }
        }
        
        return students;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert numeric to string without decimal for integers
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((int) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
}
