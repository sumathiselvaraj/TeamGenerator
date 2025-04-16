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
        
        // Count total Advanced and Full course students in all teams
        int totalAdvancedStudents = 0;
        int totalFullCourseStudents = 0;
        
        for (Team team : teams) {
            totalAdvancedStudents += team.countAdvancedCourseParticipants();
            totalFullCourseStudents += team.countFullCourseParticipants();
        }
        
        System.out.println("Total student count check - Advanced: " + totalAdvancedStudents + 
                          ", Full Course: " + totalFullCourseStudents);
        
        // Classify teams based only on their name, exactly as they're created
        advancedCourseTeams = new ArrayList<>();
        fullCourseTeams = new ArrayList<>();
        
        for (Team team : teams) {
            // Only add team to Advanced list if it actually contains Advanced students
            // and has the correct name
            if (team.getName().contains("Advanced") && team.countAdvancedCourseParticipants() > 0) {
                advancedCourseTeams.add(team);
            } 
            // Only add team to Full Course list if it contains no Advanced students
            // and has the correct name
            else if (team.getName().contains("Full Course")) {
                fullCourseTeams.add(team);
            }
        }
        
        // Calculate student counts differently, only counting actual Advanced/Full students
        advancedCourseStudentsCount = 0;
        fullCourseStudentsCount = 0;
        
        // Count only Advanced students in the Advanced teams 
        for (Team team : advancedCourseTeams) {
            advancedCourseStudentsCount += team.countAdvancedCourseParticipants();
        }
        
        // Count only Full Course students in the Full course teams
        for (Team team : fullCourseTeams) {
            fullCourseStudentsCount += team.countFullCourseParticipants();
        }
        
        System.out.println("Classified teams: Advanced teams: " + advancedCourseTeams.size() + 
                ", Advanced students: " + advancedCourseStudentsCount + 
                ", Full teams: " + fullCourseTeams.size() + 
                ", Full students: " + fullCourseStudentsCount);
    }
}