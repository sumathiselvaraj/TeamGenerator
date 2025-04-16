package com.teamformation.service;

import com.teamformation.model.EventType;
import com.teamformation.model.Student;
import com.teamformation.model.Team;
import com.teamformation.model.TeamFormationResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamFormationService {

    private static final int TEAM_SIZE = 7;

    public TeamFormationResult formTeams(List<Student> students, EventType eventType) {
        // Standardize track values
        students.forEach(student -> {
            String track = student.getTrack().trim().toUpperCase();
            student.setTrack(track);
        });
        
        // Split students by course type
        List<Student> advancedCourseStudents = students.stream()
                .filter(student -> "ADVANCED".equalsIgnoreCase(student.getCourseType()) || 
                                  student.getCourseType().toUpperCase().contains("ADVANCED"))
                .collect(Collectors.toList());
        
        List<Student> fullCourseStudents = students.stream()
                .filter(student -> "FULL".equalsIgnoreCase(student.getCourseType()) || 
                                  student.getCourseType().toUpperCase().contains("FULL"))
                .collect(Collectors.toList());
        
        // Form teams based on course type
        List<Team> advancedCourseTeams = formAdvancedCourseTeams(advancedCourseStudents);
        List<Team> fullCourseTeams = formFullCourseTeams(fullCourseStudents);
        
        return TeamFormationResult.builder()
                .eventType(eventType)
                .advancedCourseTeams(advancedCourseTeams)
                .fullCourseTeams(fullCourseTeams)
                .advancedCourseStudentsCount(advancedCourseStudents.size())
                .fullCourseStudentsCount(fullCourseStudents.size())
                .build();
    }
    
    private List<Team> formAdvancedCourseTeams(List<Student> students) {
        List<Team> teams = new ArrayList<>();
        
        if (students.isEmpty()) {
            return teams;
        }
        
        // Calculate number of teams needed
        int numStudents = students.size();
        int numTeams = (numStudents + TEAM_SIZE - 1) / TEAM_SIZE; // Ceiling division
        
        // Shuffle students for random distribution
        Collections.shuffle(students);
        
        // Assign students to teams
        for (int i = 0; i < numTeams; i++) {
            Team team = new Team();
            team.setName("Team " + (i + 1) + " - Advanced Course");
            team.setTeamType("Advanced Course");
            team.setStudents(new ArrayList<>());
            teams.add(team);
        }
        
        // Distribute students evenly across teams
        for (int i = 0; i < students.size(); i++) {
            int teamIndex = i % numTeams;
            teams.get(teamIndex).getStudents().add(students.get(i));
        }
        
        return teams;
    }
    
    private List<Team> formFullCourseTeams(List<Student> students) {
        List<Team> teams = new ArrayList<>();
        
        if (students.isEmpty()) {
            return teams;
        }
        
        // Separate students by track
        List<Student> sdetStudents = students.stream()
                .filter(s -> "SDET".equals(s.getTrack()))
                .collect(Collectors.toList());
        
        List<Student> daStudents = students.stream()
                .filter(s -> "DA".equals(s.getTrack()))
                .collect(Collectors.toList());
        
        // Calculate number of teams needed
        int numStudents = students.size();
        int numTeams = (numStudents + TEAM_SIZE - 1) / TEAM_SIZE; // Ceiling division
        
        // Create team objects
        for (int i = 0; i < numTeams; i++) {
            Team team = new Team();
            team.setName("Team " + (i + 1) + " - Full Course");
            team.setTeamType("Full Course");
            team.setStudents(new ArrayList<>());
            teams.add(team);
        }
        
        // Calculate ideal distribution of SDET and DA per team
        int idealSdetPerTeam = sdetStudents.size() / numTeams;
        int idealDaPerTeam = daStudents.size() / numTeams;
        
        // Remaining students after even distribution
        int remainingSdet = sdetStudents.size() % numTeams;
        int remainingDa = daStudents.size() % numTeams;
        
        // Shuffle students for random distribution
        Collections.shuffle(sdetStudents);
        Collections.shuffle(daStudents);
        
        // Distribute SDET students across teams
        int sdetIndex = 0;
        for (int i = 0; i < numTeams; i++) {
            int sdetToAdd = idealSdetPerTeam + (i < remainingSdet ? 1 : 0);
            for (int j = 0; j < sdetToAdd && sdetIndex < sdetStudents.size(); j++) {
                teams.get(i).getStudents().add(sdetStudents.get(sdetIndex++));
            }
        }
        
        // Distribute DA students across teams
        int daIndex = 0;
        for (int i = 0; i < numTeams; i++) {
            int daToAdd = idealDaPerTeam + (i < remainingDa ? 1 : 0);
            for (int j = 0; j < daToAdd && daIndex < daStudents.size(); j++) {
                teams.get(i).getStudents().add(daStudents.get(daIndex++));
            }
        }
        
        return teams;
    }
}
