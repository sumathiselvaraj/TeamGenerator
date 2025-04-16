package com.teamformation.controller;

import com.teamformation.model.EventType;
import com.teamformation.model.TeamFormationResult;
import com.teamformation.service.ExcelService;
import com.teamformation.service.TeamFormationService;
import com.teamformation.util.ExcelGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class TeamFormationController {

    private final ExcelService excelService;
    private final TeamFormationService teamFormationService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("eventTypes", EventType.values());
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("eventType") String eventType,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/";
        }

        try {
            // Check file extension
            String filename = file.getOriginalFilename();
            if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Only Excel files (.xlsx, .xls) are allowed");
                return "redirect:/";
            }
            
            // Parse event type
            EventType parsedEventType = EventType.valueOf(eventType);

            // Parse file and form teams based on event type
            var students = excelService.parseExcelFile(file.getInputStream(), parsedEventType);
            
            if (students.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No valid data found in the Excel file");
                return "redirect:/";
            }
            
            TeamFormationResult result = teamFormationService.formTeams(students, parsedEventType);
            
            // Store result in session
            session.setAttribute("teamFormationResult", result);
            
            return "redirect:/results";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing file: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/results")
    public String showResults(HttpSession session, Model model) {
        TeamFormationResult result = (TeamFormationResult) session.getAttribute("teamFormationResult");
        
        if (result == null) {
            return "redirect:/";
        }
        
        // For SQL Bootcamp, classify teams into advanced course and full course
        if (result.getEventType() == EventType.SQL_BOOTCAMP) {
            result.classifyTeamsForSqlBootcamp();
        }
        
        model.addAttribute("result", result);
        return "results";
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadExcel(HttpSession session) {
        TeamFormationResult result = (TeamFormationResult) session.getAttribute("teamFormationResult");
        
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        
        try {
            ByteArrayInputStream in = ExcelGenerator.generateExcel(result);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=team_formation_results.xlsx");
            
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(in));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
