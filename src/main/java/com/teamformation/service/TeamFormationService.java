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
        if (students == null || students.isEmpty()) {
            return TeamFormationResult.builder()
                    .eventType(eventType)
                    .teams(new ArrayList<>())
                    .unassignedStudents(new ArrayList<>())
                    .totalStudents(0)
                    .assignedStudents(0)
                    .summary("No students available for team formation")
                    .build();
        }
        
        // Standardize track values and handle null values
        students.forEach(student -> {
            if (student.getTrack() != null) {
                String track = student.getTrack().trim().toUpperCase();
                student.setTrack(track);
            }
        });
        
        List<Team> teams = new ArrayList<>();
        List<Student> unassignedStudents = new ArrayList<>();
        String summary;
        
        // Form teams based on event type
        if (eventType == EventType.SQL_BOOTCAMP) {
            teams = formSqlBootcampTeams(students);
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateSqlBootcampSummary(teams, unassignedStudents);
        } else if (eventType == EventType.PHASE1_GHERKIN_HACKATHON || eventType == EventType.PHASE2_SELENIUM_HACKATHON) {
            teams = formHackathonTeams(students, eventType);
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateHackathonSummary(teams, unassignedStudents, eventType);
        } else {
            // Default handling for other event types
            teams = formGenericTeams(students);
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateGenericSummary(teams, unassignedStudents);
        }
        
        int totalStudents = students.size();
        int assignedStudents = totalStudents - unassignedStudents.size();
        
        return TeamFormationResult.builder()
                .eventType(eventType)
                .teams(teams)
                .unassignedStudents(unassignedStudents)
                .totalStudents(totalStudents)
                .assignedStudents(assignedStudents)
                .summary(summary)
                .build();
    }
    
    private List<Student> findUnassignedStudents(List<Student> allStudents, List<Team> teams) {
        // Find all assigned students
        Set<String> assignedEmails = new HashSet<>();
        for (Team team : teams) {
            for (Student student : team.getMembers()) {
                assignedEmails.add(student.getEmail().toLowerCase());
            }
        }
        
        // Return students not in any team
        return allStudents.stream()
                .filter(student -> !assignedEmails.contains(student.getEmail().toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<Team> formSqlBootcampTeams(List<Student> students) {
        List<Team> teams = new ArrayList<>();
        
        // Process and categorize students by course type
        List<Student> advancedStudents = students.stream()
                .filter(student -> student.getCourseType() != null && 
                        (student.getCourseType().toLowerCase().contains("advanced") &&
                         !student.getCourseType().toLowerCase().contains("full")))
                .collect(Collectors.toList());
        
        List<Student> fullStudents = students.stream()
                .filter(student -> student.getCourseType() != null && 
                        (student.getCourseType().toLowerCase().contains("full")))
                .collect(Collectors.toList());
        
        System.out.println("Found " + advancedStudents.size() + " advanced students and " + fullStudents.size() + " full students");
        
        // Create separate lists for advanced and full course teams
        List<Team> advancedTeams = new ArrayList<>();
        List<Team> fullTeams = new ArrayList<>();
        
        // Calculate number of teams needed for advanced students (7 members per team)
        int advancedTeamCount = Math.max(1, (advancedStudents.size() + TEAM_SIZE - 1) / TEAM_SIZE);
        
        // Create advanced course teams
        for (int i = 0; i < advancedTeamCount; i++) {
            Team team = Team.builder()
                    .name("Advanced Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            advancedTeams.add(team);
        }
        
        // Calculate number of teams needed for full course students (7 members per team)
        int fullTeamCount = Math.max(1, (fullStudents.size() + TEAM_SIZE - 1) / TEAM_SIZE);
        
        // Create full course teams
        for (int i = 0; i < fullTeamCount; i++) {
            Team team = Team.builder()
                    .name("Full Course Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            fullTeams.add(team);
        }
        
        // Distribute advanced course students evenly
        Collections.shuffle(advancedStudents); // Randomize order
        for (int i = 0; i < advancedStudents.size(); i++) {
            Student student = advancedStudents.get(i);
            advancedTeams.get(i % advancedTeamCount).addMember(student);
        }
        
        // Split full course students by track
        List<Student> sdetStudents = fullStudents.stream()
                .filter(s -> "SDET".equalsIgnoreCase(s.getTrack()))
                .collect(Collectors.toList());
        
        List<Student> daStudents = fullStudents.stream()
                .filter(s -> "DA".equalsIgnoreCase(s.getTrack()))
                .collect(Collectors.toList());
        
        List<Student> dvlprStudents = fullStudents.stream()
                .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                .collect(Collectors.toList());
        
        System.out.println("Full course: " + sdetStudents.size() + " SDET students, " + 
                           daStudents.size() + " DA students, " + 
                           dvlprStudents.size() + " DVLPR students");
        
        // Try to distribute SDET, DA and DVLPR students to balance each team's ratio
        Collections.shuffle(sdetStudents);
        Collections.shuffle(daStudents);
        Collections.shuffle(dvlprStudents);
        
        // Try to ensure one DVLPR per team first
        int dvlprPerTeam = Math.min(1, dvlprStudents.size() / fullTeamCount);
        
        // First, distribute one DVLPR to each team (if available)
        for (int i = 0; i < fullTeamCount && !dvlprStudents.isEmpty(); i++) {
            Team team = fullTeams.get(i);
            if (dvlprPerTeam > 0) {
                team.addMember(dvlprStudents.remove(0));
            }
        }
        
        // Basic strategy: try to assign equal numbers of each track to each team
        for (int i = 0; i < fullTeamCount; i++) {
            Team team = fullTeams.get(i);
            
            // Determine expected number of SDET and DA per team
            int sdetPerTeam = sdetStudents.size() / fullTeamCount;
            int daPerTeam = daStudents.size() / fullTeamCount;
            
            // Add extra student to some teams if division isn't even
            if (i < sdetStudents.size() % fullTeamCount) {
                sdetPerTeam++;
            }
            
            if (i < daStudents.size() % fullTeamCount) {
                daPerTeam++;
            }
            
            // Add SDET students to team
            for (int j = 0; j < sdetPerTeam && !sdetStudents.isEmpty(); j++) {
                team.addMember(sdetStudents.remove(0));
            }
            
            // Add DA students to team
            for (int j = 0; j < daPerTeam && !daStudents.isEmpty(); j++) {
                team.addMember(daStudents.remove(0));
            }
        }
        
        // If any students remain, distribute them to teams with fewest members
        List<Student> remainingStudents = new ArrayList<>();
        remainingStudents.addAll(sdetStudents);
        remainingStudents.addAll(daStudents);
        remainingStudents.addAll(dvlprStudents);
        
        for (Student student : remainingStudents) {
            // Find team with fewest members
            Team targetTeam = fullTeams.stream()
                    .min(Comparator.comparingInt(Team::getSize))
                    .orElse(fullTeams.get(0));
            targetTeam.addMember(student);
        }
        
        // Set statistics for each team
        for (Team team : advancedTeams) {
            if (!team.getMembers().isEmpty()) {
                int dvlprCount = (int) team.getMembers().stream()
                        .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                        .count();
                
                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, Total: %d", 
                    team.getSdetCount(), 
                    team.getDaCount(),
                    dvlprCount,
                    team.getSize()));
            }
        }
        
        for (Team team : fullTeams) {
            if (!team.getMembers().isEmpty()) {
                int dvlprCount = (int) team.getMembers().stream()
                        .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                        .count();
                
                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, Total: %d", 
                    team.getSdetCount(), 
                    team.getDaCount(),
                    dvlprCount,
                    team.getSize()));
            }
        }
        
        // Remove any empty teams
        advancedTeams.removeIf(team -> team.getMembers().isEmpty());
        fullTeams.removeIf(team -> team.getMembers().isEmpty());
        
        // Combine all teams for return
        teams.addAll(advancedTeams);
        teams.addAll(fullTeams);
        
        return teams;
    }
    
    private List<Team> formHackathonTeams(List<Student> students, EventType eventType) {
        List<Team> teams = new ArrayList<>();
        
        // Calculate number of teams needed for all students
        int totalStudents = students.size();
        int numTeams = (totalStudents + TEAM_SIZE - 1) / TEAM_SIZE; // Ceiling division
        
        // Create teams
        for (int i = 0; i < numTeams; i++) {
            Team team = Team.builder()
                    .name("Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            teams.add(team);
        }
        
        // Group students by time zone for better time zone compatibility
        Map<String, List<Student>> studentsByTimeZone = new HashMap<>();
        
        // Normalize and group time zones
        for (Student student : students) {
            String timeZone = normalizeTimeZone(student.getTimeZone());
            studentsByTimeZone.computeIfAbsent(timeZone, k -> new ArrayList<>()).add(student);
        }
        
        // List of main time zone groups (in order of compatibility)
        List<String> timeZoneGroups = Arrays.asList("EST", "CST", "PST", "OTHER");
        
        // First, distribute students with previous hackathon experience evenly
        List<Student> withPreviousHackathon = students.stream()
                .filter(s -> s.getPreviousHackathon() != null && 
                            s.getPreviousHackathon().toLowerCase().contains("yes"))
                .collect(Collectors.toList());
        
        Collections.shuffle(withPreviousHackathon);
        
        for (int i = 0; i < withPreviousHackathon.size(); i++) {
            teams.get(i % numTeams).addMember(withPreviousHackathon.get(i));
        }
        
        // Then, distribute working students evenly among teams
        List<Student> workingStudents = students.stream()
                .filter(s -> s.getWorkingStatus() != null && 
                        s.getWorkingStatus().toLowerCase().contains("yes"))
                .filter(s -> !hasStudentBeenAssigned(s, teams)) // Skip if already assigned
                .collect(Collectors.toList());
        
        Collections.shuffle(workingStudents);
        
        for (int i = 0; i < workingStudents.size(); i++) {
            teams.get(i % numTeams).addMember(workingStudents.get(i));
        }
        
        // Finally, distribute remaining students by time zone compatibility
        for (String timeZone : timeZoneGroups) {
            if (studentsByTimeZone.containsKey(timeZone)) {
                List<Student> timeZoneStudents = studentsByTimeZone.get(timeZone).stream()
                        .filter(s -> !hasStudentBeenAssigned(s, teams))
                        .collect(Collectors.toList());
                
                // Process students from this time zone
                if (!timeZoneStudents.isEmpty()) {
                    distributeStudentsByTimeZone(timeZoneStudents, teams);
                }
            }
        }
        
        // If any students remain unassigned, distribute them to teams with fewest members
        List<Student> remainingStudents = students.stream()
                .filter(s -> !hasStudentBeenAssigned(s, teams))
                .collect(Collectors.toList());
        
        for (Student student : remainingStudents) {
            // Find team with fewest members
            Team targetTeam = teams.stream()
                    .min(Comparator.comparingInt(Team::getSize))
                    .orElse(teams.get(0));
            targetTeam.addMember(student);
        }
        
        // Set statistics for each team
        for (Team team : teams) {
            if (!team.getMembers().isEmpty()) {
                int workingCount = (int) team.getMembers().stream()
                        .filter(s -> s.getWorkingStatus() != null && 
                                s.getWorkingStatus().toLowerCase().contains("yes"))
                        .count();
                
                int withPreviousHackathonCount = (int) team.getMembers().stream()
                        .filter(s -> s.getPreviousHackathon() != null && 
                                s.getPreviousHackathon().toLowerCase().contains("yes"))
                        .count();
                
                Map<String, Long> timeZoneCounts = team.getMembers().stream()
                        .filter(s -> s.getTimeZone() != null)
                        .collect(Collectors.groupingBy(
                                s -> normalizeTimeZone(s.getTimeZone()),
                                Collectors.counting()));
                
                StringBuilder timeZoneStats = new StringBuilder();
                timeZoneCounts.forEach((tz, count) -> 
                        timeZoneStats.append(tz).append(": ").append(count).append(", "));
                
                team.setStatistics(String.format("SDET: %d, DA: %d, Working: %d, Previous Hackathon: %d, TimeZones: %s Total: %d", 
                    team.countByTrack("SDET"), 
                    team.countByTrack("DA"),
                    workingCount,
                    withPreviousHackathonCount,
                    timeZoneStats.length() > 0 ? timeZoneStats.substring(0, timeZoneStats.length() - 2) : "None",
                    team.getSize()));
            }
        }
        
        // Remove any empty teams
        teams.removeIf(team -> team.getMembers().isEmpty());
        
        return teams;
    }
    
    private boolean hasStudentBeenAssigned(Student student, List<Team> teams) {
        String email = student.getEmail().toLowerCase();
        for (Team team : teams) {
            for (Student member : team.getMembers()) {
                if (member.getEmail().toLowerCase().equals(email)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private String normalizeTimeZone(String timeZone) {
        if (timeZone == null) {
            return "OTHER";
        }
        
        String upperTimeZone = timeZone.toUpperCase();
        
        if (upperTimeZone.contains("EST") || upperTimeZone.contains("EASTERN") || 
                upperTimeZone.contains("ET") || upperTimeZone.contains("GMT-5") || 
                upperTimeZone.contains("GMT-4") || upperTimeZone.contains("UTC-5") || 
                upperTimeZone.contains("UTC-4")) {
            return "EST";
        } else if (upperTimeZone.contains("CST") || upperTimeZone.contains("CENTRAL") || 
                upperTimeZone.contains("CT") || upperTimeZone.contains("GMT-6") || 
                upperTimeZone.contains("GMT-5") || upperTimeZone.contains("UTC-6") || 
                upperTimeZone.contains("UTC-5")) {
            return "CST";
        } else if (upperTimeZone.contains("PST") || upperTimeZone.contains("PACIFIC") || 
                upperTimeZone.contains("PT") || upperTimeZone.contains("GMT-8") || 
                upperTimeZone.contains("GMT-7") || upperTimeZone.contains("UTC-8") || 
                upperTimeZone.contains("UTC-7")) {
            return "PST";
        } else {
            return "OTHER";
        }
    }
    
    private void distributeStudentsByTimeZone(List<Student> timeZoneStudents, List<Team> teams) {
        // Group teams by dominant time zone
        Map<String, List<Team>> teamsByTimeZone = new HashMap<>();
        
        for (Team team : teams) {
            Map<String, Long> timeZoneCounts = team.getMembers().stream()
                    .filter(s -> s.getTimeZone() != null)
                    .collect(Collectors.groupingBy(
                            s -> normalizeTimeZone(s.getTimeZone()),
                            Collectors.counting()));
            
            String dominantTimeZone = "NONE";
            long maxCount = 0;
            
            for (Map.Entry<String, Long> entry : timeZoneCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    dominantTimeZone = entry.getKey();
                }
            }
            
            teamsByTimeZone.computeIfAbsent(dominantTimeZone, k -> new ArrayList<>()).add(team);
        }
        
        // Add teams with no members yet to the mapping
        for (Team team : teams) {
            if (team.getMembers().isEmpty()) {
                teamsByTimeZone.computeIfAbsent("NONE", k -> new ArrayList<>()).add(team);
            }
        }
        
        // Distribute students to teams with matching time zone or to teams with fewer members
        Collections.shuffle(timeZoneStudents);
        String currentTimeZone = normalizeTimeZone(timeZoneStudents.get(0).getTimeZone());
        
        // First, try to assign to teams with same time zone
        if (teamsByTimeZone.containsKey(currentTimeZone)) {
            List<Team> matchingTeams = teamsByTimeZone.get(currentTimeZone);
            for (Student student : timeZoneStudents) {
                Team targetTeam = matchingTeams.stream()
                        .min(Comparator.comparingInt(Team::getSize))
                        .orElse(null);
                
                if (targetTeam != null) {
                    targetTeam.addMember(student);
                    continue;
                }
                
                // If we can't find a matching team, assign to any team with fewest members
                targetTeam = teams.stream()
                        .min(Comparator.comparingInt(Team::getSize))
                        .orElse(teams.get(0));
                targetTeam.addMember(student);
            }
        } 
        // If no matching teams, try compatible time zones (EST with CST, CST with PST)
        else {
            List<Team> compatibleTeams = new ArrayList<>();
            
            // Find compatible time zones
            if ("EST".equals(currentTimeZone) && teamsByTimeZone.containsKey("CST")) {
                compatibleTeams.addAll(teamsByTimeZone.get("CST"));
            } else if ("CST".equals(currentTimeZone)) {
                if (teamsByTimeZone.containsKey("EST")) {
                    compatibleTeams.addAll(teamsByTimeZone.get("EST"));
                }
                if (teamsByTimeZone.containsKey("PST")) {
                    compatibleTeams.addAll(teamsByTimeZone.get("PST"));
                }
            } else if ("PST".equals(currentTimeZone) && teamsByTimeZone.containsKey("CST")) {
                compatibleTeams.addAll(teamsByTimeZone.get("CST"));
            }
            
            // If we found compatible teams, use them
            if (!compatibleTeams.isEmpty()) {
                for (Student student : timeZoneStudents) {
                    Team targetTeam = compatibleTeams.stream()
                            .min(Comparator.comparingInt(Team::getSize))
                            .orElse(null);
                    
                    if (targetTeam != null) {
                        targetTeam.addMember(student);
                        continue;
                    }
                    
                    // If we can't find a compatible team, assign to any team with fewest members
                    targetTeam = teams.stream()
                            .min(Comparator.comparingInt(Team::getSize))
                            .orElse(teams.get(0));
                    targetTeam.addMember(student);
                }
            }
            // If no compatible teams, just assign to teams with fewest members
            else {
                for (Student student : timeZoneStudents) {
                    Team targetTeam = teams.stream()
                            .min(Comparator.comparingInt(Team::getSize))
                            .orElse(teams.get(0));
                    targetTeam.addMember(student);
                }
            }
        }
    }
    
    private List<Team> formGenericTeams(List<Student> students) {
        List<Team> teams = new ArrayList<>();
        
        // Calculate number of teams needed
        int totalStudents = students.size();
        int numTeams = (totalStudents + TEAM_SIZE - 1) / TEAM_SIZE; // Ceiling division
        
        // Create teams
        for (int i = 0; i < numTeams; i++) {
            Team team = Team.builder()
                    .name("Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            teams.add(team);
        }
        
        // Shuffle for random distribution
        Collections.shuffle(students);
        
        // Distribute students evenly across teams
        for (int i = 0; i < students.size(); i++) {
            teams.get(i % numTeams).addMember(students.get(i));
        }
        
        // Set statistics for each team
        for (Team team : teams) {
            team.setStatistics(String.format("Members: %d", team.getSize()));
        }
        
        return teams;
    }
    
    private String generateSqlBootcampSummary(List<Team> teams, List<Student> unassignedStudents) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Created %d teams for SQL Bootcamp\n", teams.size()));
        summary.append(String.format("Total students: %d\n", 
                teams.stream().mapToInt(Team::getSize).sum() + unassignedStudents.size()));
        
        int sdetCount = teams.stream().mapToInt(t -> t.countByTrack("SDET")).sum();
        int daCount = teams.stream().mapToInt(t -> t.countByTrack("DA")).sum();
        int advancedCount = teams.stream().mapToInt(Team::countAdvancedCourseParticipants).sum();
        
        summary.append(String.format("Distribution - SDET: %d, DA: %d, Advanced: %d\n", 
                sdetCount, daCount, advancedCount));
        
        if (!unassignedStudents.isEmpty()) {
            summary.append(String.format("Unassigned students: %d\n", unassignedStudents.size()));
        }
        
        return summary.toString();
    }
    
    private String generateHackathonSummary(List<Team> teams, List<Student> unassignedStudents, EventType eventType) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Created %d teams for %s\n", teams.size(), eventType.getDisplayName()));
        summary.append(String.format("Total students: %d\n", 
                teams.stream().mapToInt(Team::getSize).sum() + unassignedStudents.size()));
        
        int sdetCount = teams.stream().mapToInt(t -> t.countByTrack("SDET")).sum();
        int daCount = teams.stream().mapToInt(t -> t.countByTrack("DA")).sum();
        int workingCount = 0;
        
        for (Team team : teams) {
            workingCount += team.getMembers().stream()
                    .filter(s -> s.getWorkingStatus() != null && 
                            s.getWorkingStatus().toLowerCase().contains("yes"))
                    .count();
        }
        
        summary.append(String.format("Distribution - SDET: %d, DA: %d, Working: %d\n", 
                sdetCount, daCount, workingCount));
        
        if (!unassignedStudents.isEmpty()) {
            summary.append(String.format("Unassigned students: %d\n", unassignedStudents.size()));
        }
        
        return summary.toString();
    }
    
    private String generateGenericSummary(List<Team> teams, List<Student> unassignedStudents) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Created %d teams\n", teams.size()));
        summary.append(String.format("Total students: %d\n", 
                teams.stream().mapToInt(Team::getSize).sum() + unassignedStudents.size()));
        
        if (!unassignedStudents.isEmpty()) {
            summary.append(String.format("Unassigned students: %d\n", unassignedStudents.size()));
        }
        
        return summary.toString();
    }
}
