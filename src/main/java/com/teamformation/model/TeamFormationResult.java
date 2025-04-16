package com.teamformation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamFormationResult {
    private List<Team> teams;
    private List<Student> unassignedStudents;
    private EventType eventType;
    private String summary;
    private int totalStudents;
    private int assignedStudents;
    
    // Special fields for SQL Bootcamp
    private List<Team> advancedCourseTeams;
    private List<Team> fullCourseTeams;
    private int advancedCourseStudentsCount;
    private int fullCourseStudentsCount;
    
    // Get distributions for reporting
    public String getAssignmentRate() {
        if (totalStudents == 0) {
            return "0%";
        }
        double rate = (double) assignedStudents / totalStudents * 100;
        return String.format("%.1f%%", rate);
    }
    
    // Methods for SQL Bootcamp team statistics
    public int getAdvancedCourseTeamsCount() {
        return advancedCourseTeams != null ? advancedCourseTeams.size() : 0;
    }
    
    public int getFullCourseTeamsCount() {
        return fullCourseTeams != null ? fullCourseTeams.size() : 0;
    }
    
    // Classification helpers
    public void classifyTeamsForSqlBootcamp() {
        if (teams == null) {
            advancedCourseTeams = new ArrayList<>();
            fullCourseTeams = new ArrayList<>();
            return;
        }
        
        // For SQL Bootcamp, split the teams in half for better display
        // First half will be "Advanced Course Teams", second half will be "Full Course Teams"
        int totalTeams = teams.size();
        int midpoint = totalTeams / 2;
        
        // First half teams are "Advanced Course Teams"
        advancedCourseTeams = new ArrayList<>();
        for (int i = 0; i < midpoint; i++) {
            advancedCourseTeams.add(teams.get(i));
        }
        
        // Second half teams are "Full Course Teams"
        fullCourseTeams = new ArrayList<>();
        for (int i = midpoint; i < totalTeams; i++) {
            fullCourseTeams.add(teams.get(i));
        }
        
        // Calculate student counts
        advancedCourseStudentsCount = advancedCourseTeams.stream()
                .mapToInt(Team::getSize)
                .sum();
        
        fullCourseStudentsCount = fullCourseTeams.stream()
                .mapToInt(Team::getSize)
                .sum();
        
        System.out.println("Classified teams: Advanced teams: " + advancedCourseTeams.size() + 
                ", Advanced students: " + advancedCourseStudentsCount + 
                ", Full teams: " + fullCourseTeams.size() + 
                ", Full students: " + fullCourseStudentsCount);
    }
}