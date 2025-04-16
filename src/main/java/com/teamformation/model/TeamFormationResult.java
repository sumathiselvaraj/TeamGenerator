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
        
        // Classify teams based on their name
        advancedCourseTeams = teams.stream()
                .filter(team -> team.getName().contains("Advanced"))
                .collect(Collectors.toList());
        
        fullCourseTeams = teams.stream()
                .filter(team -> team.getName().contains("Full Course"))
                .collect(Collectors.toList());
        
        // If no teams were classified, do a backup classification based on student course types
        if (advancedCourseTeams.isEmpty() && fullCourseTeams.isEmpty()) {
            // For SQL Bootcamp, if team naming doesn't work, separate based on course type counts
            for (Team team : teams) {
                // Count advanced and full course students in this team
                int advancedCount = team.countAdvancedCourseParticipants();
                int fullCount = team.countFullCourseParticipants();
                
                // Add to appropriate list based on majority
                if (advancedCount > fullCount) {
                    advancedCourseTeams.add(team);
                } else {
                    fullCourseTeams.add(team);
                }
            }
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