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

    private static final int SQL_BOOTCAMP_TEAM_SIZE = 7; // SQL Bootcamp uses 7-member teams
    private static final int HACKATHON_TEAM_SIZE = 5; // Hackathons use 5-member teams

    public TeamFormationResult formTeams(List<Student> students, EventType eventType) {
        if (students == null || students.isEmpty()) {
            return TeamFormationResult.builder()
                    .teams(new ArrayList<>())
                    .eventType(eventType)
                    .summary("No students provided")
                    .build();
        }

        System.out.println("Forming teams for event type: " + eventType);
        System.out.println("Number of students: " + students.size());

        List<Team> teams;
        
        switch (eventType) {
            case SQL_BOOTCAMP:
                teams = formSqlBootcampTeams(students);
                break;
            case SELENIUM_HACKATHON:
                teams = formSeleniumHackathonTeams(students);
                break;
            case PHASE1_API_HACKATHON:
                teams = formPhase1ApiHackathonTeams(students);
                break;
            case PHASE2_API_HACKATHON:
                teams = formPhase2ApiHackathonTeams(students);
                break;
            case RECIPE_SCRAPING_HACKATHON:
                teams = formScrapingHackathonTeams(students);
                break;
            default:
                return TeamFormationResult.builder()
                        .teams(new ArrayList<>())
                        .eventType(eventType)
                        .summary("Unsupported event type: " + eventType)
                        .build();
        }

        return TeamFormationResult.builder()
                .teams(teams)
                .eventType(eventType)
                .summary("Successfully formed " + teams.size() + " teams")
                .build();
    }

    /**
     * Helper method that finds the best team for a student based on batch diversity
     * and minimizing the count of a specific attribute (working status, previous experience, etc.)
     * 
     * @param teams The list of teams
     * @param student The student to place
     * @param workingCount Array tracking count of working students per team
     * @param numTeams Number of teams
     * @return The index of the team where the student should be placed
     */
    private int findBestTeamForStudent(List<Team> teams, Student student, int[] workingCount, int numTeams) {
        // For SQL Bootcamp, we don't enforce batch diversity
        if (student.getCourseType() != null && 
            student.getCourseType().equalsIgnoreCase(EventType.SQL_BOOTCAMP.getDisplayName())) {
            // Just find the team with the fewest working students
            int targetTeam = 0;
            for (int i = 1; i < numTeams; i++) {
                if (workingCount[i] < workingCount[targetTeam]) {
                    targetTeam = i;
                }
            }
            return targetTeam;
        }
        
        // For other events, consider batch diversity
        String studentBatch = student.getBatch();
        
        // First, try to find teams that don't have this batch
        List<Integer> teamsWithoutBatch = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            Team team = teams.get(i);
            if (!team.hasBatchNumber(studentBatch)) {
                teamsWithoutBatch.add(i);
            }
        }
        
        // If we found teams without this batch, find the one with the fewest working students
        if (!teamsWithoutBatch.isEmpty()) {
            int targetTeam = teamsWithoutBatch.get(0);
            for (int i = 1; i < teamsWithoutBatch.size(); i++) {
                int teamIdx = teamsWithoutBatch.get(i);
                if (workingCount[teamIdx] < workingCount[targetTeam]) {
                    targetTeam = teamIdx;
                }
            }
            return targetTeam;
        }
        
        // If all teams have this batch already, just find the team with the fewest working students
        int targetTeam = 0;
        for (int i = 1; i < numTeams; i++) {
            if (workingCount[i] < workingCount[targetTeam]) {
                targetTeam = i;
            }
        }
        return targetTeam;
    }

    private String normalizeTimeZone(String timeZone) {
        if (timeZone == null) {
            return "OTHER";
        }
        
        timeZone = timeZone.toUpperCase().trim();
        
        if (timeZone.contains("EST") || timeZone.contains("EASTERN") || 
            timeZone.contains("EDT") || timeZone.contains("ET")) {
            return "EST";
        } else if (timeZone.contains("CST") || timeZone.contains("CENTRAL") || 
                 timeZone.contains("CDT") || timeZone.contains("CT")) {
            return "CST";
        } else if (timeZone.contains("MST") || timeZone.contains("MOUNTAIN") || 
                 timeZone.contains("MDT") || timeZone.contains("MT")) {
            return "MST";
        } else if (timeZone.contains("PST") || timeZone.contains("PACIFIC") || 
                 timeZone.contains("PDT") || timeZone.contains("PT")) {
            return "PST";
        } else if (timeZone.contains("IST") || timeZone.contains("INDIAN") || 
                 timeZone.contains("INDIA")) {
            return "IST";
        } else {
            return "OTHER";
        }
    }

    private List<Team> formSqlBootcampTeams(List<Student> students) {
        int numStudents = students.size();
        System.out.println("Forming SQL Bootcamp teams with " + numStudents + " students");
        
        // Sort students by course type, placing 'advanced' ones first
        students.sort((s1, s2) -> {
            String type1 = s1.getCourseType() == null ? "" : s1.getCourseType().toLowerCase();
            String type2 = s2.getCourseType() == null ? "" : s2.getCourseType().toLowerCase();
            boolean isAdvanced1 = type1.contains("advanced") || type1.contains("full");
            boolean isAdvanced2 = type2.contains("advanced") || type2.contains("full");
            if (isAdvanced1 && !isAdvanced2) return -1;
            if (!isAdvanced1 && isAdvanced2) return 1;
            return 0;
        });
        
        // Calculate number of teams needed
        int numTeams = (int) Math.ceil((double) numStudents / SQL_BOOTCAMP_TEAM_SIZE);
        System.out.println("Initial number of teams: " + numTeams);
        
        // Initialize teams
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teams.add(Team.builder()
                    .name("Team " + (i + 1))
                    .build());
        }
        
        // Sort by track, this helps with distribution
        students.sort(Comparator.comparing(Student::getTrack, 
                Comparator.nullsLast(String::compareToIgnoreCase)));
        
        // First, distribute advanced students evenly across teams
        List<Student> advancedStudents = students.stream()
                .filter(s -> s.getCourseType() != null && 
                        (s.getCourseType().toLowerCase().contains("advanced") || 
                         s.getCourseType().toLowerCase().contains("full")))
                .collect(Collectors.toList());
        
        System.out.println("Number of advanced students: " + advancedStudents.size());
        
        // Remove advanced students from main list
        students.removeAll(advancedStudents);
        
        // Distribute advanced students evenly
        int[] expCount = new int[numTeams]; // Count of experienced students per team
        
        for (Student student : advancedStudents) {
            // Find the team with fewest experienced members
            int targetTeam = findBestTeamForStudent(teams, student, expCount, numTeams);
            
            // Add student to the team
            teams.get(targetTeam).addMember(student);
            expCount[targetTeam]++;
        }
        
        // Now distribute remaining students by track to keep DA/SDET ratio balanced
        // and DVLPR count as even as possible
        
        // First process DVLPR students to ensure one per team if possible
        List<Student> dvlprStudents = students.stream()
                .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                .collect(Collectors.toList());
        
        System.out.println("Number of DVLPR students: " + dvlprStudents.size());
        
        // Remove DVLPR students from main list
        students.removeAll(dvlprStudents);
        
        // Distribute one DVLPR per team if possible
        for (int i = 0; i < Math.min(dvlprStudents.size(), numTeams); i++) {
            teams.get(i).addMember(dvlprStudents.get(i));
        }
        
        // If more DVLPRs than teams, distribute the rest
        if (dvlprStudents.size() > numTeams) {
            List<Student> remainingDvlprs = dvlprStudents.subList(numTeams, dvlprStudents.size());
            for (Student student : remainingDvlprs) {
                // Find the team with the fewest members
                int targetTeam = 0;
                for (int i = 1; i < numTeams; i++) {
                    if (teams.get(i).getSize() < teams.get(targetTeam).getSize()) {
                        targetTeam = i;
                    }
                }
                teams.get(targetTeam).addMember(student);
            }
        }
        
        // Distribute remaining students evenly, considering working status
        int[] workingCount = new int[numTeams]; // Count of working students per team
        
        // First, track current working students
        for (int i = 0; i < numTeams; i++) {
            workingCount[i] = (int) teams.get(i).getMembers().stream()
                    .filter(s -> "Yes".equalsIgnoreCase(s.getWorkingStatus()))
                    .count();
        }
        
        // Now distribute remaining students
        for (Student student : students) {
            // Find the team with fewest working members
            int targetTeam = findBestTeamForStudent(teams, student, workingCount, numTeams);
            
            // Add student to the team
            teams.get(targetTeam).addMember(student);
            if ("Yes".equalsIgnoreCase(student.getWorkingStatus())) {
                workingCount[targetTeam]++;
            }
        }
        
        // Calculate and set statistics for each team
        for (Team team : teams) {
            StringBuilder builder = new StringBuilder();
            builder.append("Total: ").append(team.getSize()).append(", ");
            builder.append("Advanced: ").append(team.countAdvancedCourseParticipants()).append(", ");
            builder.append("DA: ").append(team.getDaCount()).append(", ");
            builder.append("SDET: ").append(team.getSdetCount()).append(", ");
            builder.append("DVLPR: ").append(team.getDvlprCount()).append(", ");
            builder.append("Working: ").append(team.countByWorkingStatus("Yes"));
            
            team.setStatistics(builder.toString());
        }
        
        // Print team statistics for verification
        System.out.println("Team statistics:");
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            System.out.println(String.format("Team %d: %s", i+1, team.getStatistics()));
        }
        
        // Remove any empty teams
        teams.removeIf(team -> team.getMembers().isEmpty());
        
        return teams;
    }

    // Other team formation methods will be implemented here...
    // For example: formSeleniumHackathonTeams, formPhase1ApiHackathonTeams, etc.
    
    private List<Team> formSeleniumHackathonTeams(List<Student> students) {
        // Implementation for Selenium Hackathon team formation
        return new ArrayList<>();
    }
    
    private List<Team> formPhase1ApiHackathonTeams(List<Student> students) {
        int numStudents = students.size();
        System.out.println("Forming Phase 1 API Hackathon teams with " + numStudents + " students");
        
        // Calculate number of teams needed (5 members per team for API Hackathon)
        int numTeams = (int) Math.ceil((double) numStudents / HACKATHON_TEAM_SIZE);
        System.out.println("Initial number of teams: " + numTeams);
        
        // Initialize teams
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teams.add(Team.builder()
                    .name("Team " + (i + 1))
                    .build());
        }
        
        // First, identify students who participated in previous API hackathons
        List<Student> experiencedStudents = students.stream()
                .filter(s -> "Yes".equalsIgnoreCase(s.getPreviousHackathonParticipation()))
                .collect(Collectors.toList());
        
        // Remove experienced students from main list for separate distribution
        List<Student> remainingStudents = new ArrayList<>(students);
        remainingStudents.removeAll(experiencedStudents);
        
        System.out.println("Number of students with previous API Hackathon experience: " + experiencedStudents.size());
        
        // Distribute experienced students evenly across teams
        int[] expCount = new int[numTeams]; // Count of experienced students per team
        
        for (Student student : experiencedStudents) {
            // Find the team with fewest experienced members, ensuring batch diversity
            int targetTeam = findBestTeamForStudent(teams, student, expCount, numTeams);
            
            // Add student to the team
            teams.get(targetTeam).addMember(student);
            expCount[targetTeam]++;
        }
        
        // Next, distribute working students
        List<Student> workingStudents = remainingStudents.stream()
                .filter(s -> "Yes".equalsIgnoreCase(s.getWorkingStatus()))
                .collect(Collectors.toList());
        
        // Remove working students from main list
        remainingStudents.removeAll(workingStudents);
        
        System.out.println("Number of working students: " + workingStudents.size());
        
        // Track working students per team
        int[] workingCount = new int[numTeams];
        
        // Update working count for already assigned students
        for (int i = 0; i < numTeams; i++) {
            workingCount[i] = (int) teams.get(i).getMembers().stream()
                    .filter(s -> "Yes".equalsIgnoreCase(s.getWorkingStatus()))
                    .count();
        }
        
        // Distribute working students evenly
        for (Student student : workingStudents) {
            // Find the team with fewest working members, ensuring batch diversity
            int targetTeam = findBestTeamForStudent(teams, student, workingCount, numTeams);
            
            // Add student to the team
            teams.get(targetTeam).addMember(student);
            workingCount[targetTeam]++;
        }
        
        // Now sort remaining students by time zone to group compatible time zones
        Map<String, List<Student>> timeZoneGroups = new HashMap<>();
        
        for (Student student : remainingStudents) {
            String normalizedTimeZone = normalizeTimeZone(student.getTimeZone());
            if (!timeZoneGroups.containsKey(normalizedTimeZone)) {
                timeZoneGroups.put(normalizedTimeZone, new ArrayList<>());
            }
            timeZoneGroups.get(normalizedTimeZone).add(student);
        }
        
        // Log time zone distribution
        for (Map.Entry<String, List<Student>> entry : timeZoneGroups.entrySet()) {
            System.out.println("Time zone " + entry.getKey() + ": " + entry.getValue().size() + " students");
        }
        
        // Special handling for track distribution in Phase 1 API Hackathon
        // We need to split DA and DVLPR tracks equally across teams
        
        // First, process students based on time zone compatibility
        List<String> timeZones = new ArrayList<>(timeZoneGroups.keySet());
        for (String timeZone : timeZones) {
            List<Student> studentsInTimeZone = timeZoneGroups.get(timeZone);
            
            // Process DA track students first
            List<Student> daStudents = studentsInTimeZone.stream()
                    .filter(s -> "DA".equalsIgnoreCase(s.getTrack()))
                    .collect(Collectors.toList());
            
            for (Student student : daStudents) {
                // Find the best team based on batch diversity and working status
                int targetTeam = findBestTeamForStudent(teams, student, workingCount, numTeams);
                teams.get(targetTeam).addMember(student);
            }
            
            // Then process DVLPR track students
            List<Student> dvlprStudents = studentsInTimeZone.stream()
                    .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                    .collect(Collectors.toList());
            
            for (Student student : dvlprStudents) {
                // Find the best team based on batch diversity and working status
                int targetTeam = findBestTeamForStudent(teams, student, workingCount, numTeams);
                teams.get(targetTeam).addMember(student);
            }
            
            // Finally, process other tracks
            List<Student> otherStudents = studentsInTimeZone.stream()
                    .filter(s -> !"DA".equalsIgnoreCase(s.getTrack()) && !"DVLPR".equalsIgnoreCase(s.getTrack()))
                    .collect(Collectors.toList());
            
            for (Student student : otherStudents) {
                // Find the best team based on batch diversity and working status
                int targetTeam = findBestTeamForStudent(teams, student, workingCount, numTeams);
                teams.get(targetTeam).addMember(student);
            }
        }
        
        // Calculate and set statistics for each team
        for (Team team : teams) {
            // Generate statistics for time zones
            StringBuilder timeZoneStats = new StringBuilder();
            Map<String, Long> tzCount = team.getMembers().stream()
                    .collect(Collectors.groupingBy(
                            s -> normalizeTimeZone(s.getTimeZone()),
                            Collectors.counting()
                    ));
            
            for (Map.Entry<String, Long> entry : tzCount.entrySet()) {
                timeZoneStats.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
            }
            
            // Count students with previous hackathon experience
            int withPreviousHackathonCount = (int) team.getMembers().stream()
                    .filter(s -> "Yes".equalsIgnoreCase(s.getPreviousHackathonParticipation()))
                    .count();
            
            // Count working students
            int workingStudentCount = (int) team.getMembers().stream()
                    .filter(s -> "Yes".equalsIgnoreCase(s.getWorkingStatus()))
                    .count();
            
            team.setStatistics(String.format(
                    "Total: %d, DA: %d, DVLPR: %d, Working: %d, Experienced: %d, Time Zones: %s",
                    team.getSize(),
                    team.getDaCount(),
                    team.countByTrack("DVLPR"),
                    workingStudentCount,
                    withPreviousHackathonCount,
                    timeZoneStats.length() > 0 ? timeZoneStats.substring(0, timeZoneStats.length() - 2) : "None"));
        }
        
        // Print team sizes for verification
        System.out.println("Final team sizes:");
        for (int i = 0; i < teams.size(); i++) {
            System.out.println("Team " + (i+1) + ": " + teams.get(i).getSize() + " members");
        }
        
        // Remove any empty teams
        teams.removeIf(team -> team.getMembers().isEmpty());
        
        return teams;
    }
    
    private List<Team> formPhase2ApiHackathonTeams(List<Student> students) {
        // Implementation for Phase 2 API Hackathon team formation
        return new ArrayList<>();
    }
    
    private List<Team> formScrapingHackathonTeams(List<Student> students) {
        // Implementation for Recipe Scraping Hackathon team formation
        return new ArrayList<>();
    }
}