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
        } else if (eventType == EventType.SELENIUM_HACKATHON) {
            teams = formHackathonTeams(students, eventType);
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateHackathonSummary(teams, unassignedStudents, eventType);
        } else if (eventType == EventType.PHASE1_API_HACKATHON) {
            teams = formApiHackathonTeams(students, eventType, true); // Phase 1 needs DA + DVLPR distribution
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateApiHackathonSummary(teams, unassignedStudents, eventType);
        } else if (eventType == EventType.PHASE2_API_HACKATHON) {
            teams = formApiHackathonTeams(students, eventType, false); // Phase 2 only needs DVLPR distribution
            unassignedStudents = findUnassignedStudents(students, teams);
            summary = generateApiHackathonSummary(teams, unassignedStudents, eventType);
        } else if (eventType == EventType.RECIPE_SCRAPING_HACKATHON) {
            teams = formHackathonTeams(students, eventType); // Using the general hackathon team formation
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
        int advancedTeamCount = Math.max(1, (advancedStudents.size() + SQL_BOOTCAMP_TEAM_SIZE - 1) / SQL_BOOTCAMP_TEAM_SIZE);
        
        // Create advanced course teams
        for (int i = 0; i < advancedTeamCount; i++) {
            Team team = Team.builder()
                    .name("Advanced Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            advancedTeams.add(team);
        }
        
        // Calculate number of teams needed for full course students (7 members per team)
        int fullTeamCount = Math.max(1, (fullStudents.size() + SQL_BOOTCAMP_TEAM_SIZE - 1) / SQL_BOOTCAMP_TEAM_SIZE);
        
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
        
        System.out.println("Forming teams for " + eventType.getDisplayName() + " with " + students.size() + " students");
        
        // Calculate the optimal number of teams based on team size (aiming for 5 students per team)
        int totalStudents = students.size();
        int studentsPerTeam = HACKATHON_TEAM_SIZE; // Using the hackathon team size constant
        int numTeams = Math.max(totalStudents / studentsPerTeam, 1); // At least 1 team
        
        // Adjust number of teams to ensure more even distribution
        // If we have remainder, see if we should add another team or stick with current number
        if (totalStudents % studentsPerTeam > 0) {
            // Calculate average team size with current number of teams vs adding one more team
            double currentAvg = (double) totalStudents / numTeams;
            double nextAvg = (double) totalStudents / (numTeams + 1);
            
            // If adding another team makes the distribution more even, do it
            if (Math.abs(nextAvg - studentsPerTeam) < Math.abs(currentAvg - studentsPerTeam)) {
                numTeams++;
            }
        }
        
        System.out.println("Creating " + numTeams + " teams with approximately " + 
                           (totalStudents / numTeams) + " students per team");
        
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
        
        // Count students by timezone
        studentsByTimeZone.forEach((tz, list) -> 
                System.out.println("Time zone " + tz + ": " + list.size() + " students"));
        
        // List of main time zone groups (in order of compatibility)
        List<String> timeZoneGroups = Arrays.asList("EST", "CST", "PST", "OTHER");
        
        // STEP 1: First, distribute students with previous hackathon experience evenly
        List<Student> withPreviousHackathon = students.stream()
                .filter(s -> s.getPreviousHackathon() != null && 
                            s.getPreviousHackathon().toLowerCase().contains("yes"))
                .collect(Collectors.toList());
        
        System.out.println("Found " + withPreviousHackathon.size() + 
                          " students with previous hackathon experience");
        
        // Randomize experienced students to ensure fair distribution
        Collections.shuffle(withPreviousHackathon);
        
        // Calculate how many experienced students should be in each team for even distribution
        int expPerTeam = withPreviousHackathon.size() / numTeams;
        int remainder = withPreviousHackathon.size() % numTeams;
        
        // Initialize a counter for each team's experienced students
        int[] expCount = new int[numTeams];
        
        // Distribute experienced students evenly across teams
        for (Student student : withPreviousHackathon) {
            // Find the team with the fewest experienced students
            int targetTeam = 0;
            for (int i = 1; i < numTeams; i++) {
                if (expCount[i] < expCount[targetTeam]) {
                    targetTeam = i;
                }
            }
            
            // Add the student to the team
            teams.get(targetTeam).addMember(student);
            expCount[targetTeam]++;
        }
        
        // STEP 2: Count ALL working students (including those already assigned in step 1)
        // First, count working students already assigned in step 1
        int[] workingCount = new int[numTeams];
        for (int i = 0; i < numTeams; i++) {
            Team team = teams.get(i);
            workingCount[i] = (int) team.getMembers().stream()
                .filter(s -> s.getWorkingStatus() != null && 
                        s.getWorkingStatus().toLowerCase().contains("yes"))
                .count();
        }
        
        // Get working students not yet assigned
        List<Student> workingStudents = students.stream()
                .filter(s -> s.getWorkingStatus() != null && 
                        s.getWorkingStatus().toLowerCase().contains("yes"))
                .filter(s -> !hasStudentBeenAssigned(s, teams)) // Skip if already assigned
                .collect(Collectors.toList());
        
        System.out.println("Found " + workingStudents.size() + " working students not yet assigned");
        System.out.println("Current working students per team:");
        for (int i = 0; i < numTeams; i++) {
            System.out.println("Team " + (i+1) + ": " + workingCount[i] + " working students");
        }
        
        // Randomize working students to ensure fair distribution
        Collections.shuffle(workingStudents);
        
        // Distribute working students evenly across teams
        for (Student student : workingStudents) {
            // Find the team with the fewest working students
            int targetTeam = 0;
            for (int i = 1; i < numTeams; i++) {
                if (workingCount[i] < workingCount[targetTeam]) {
                    targetTeam = i;
                }
            }
            
            // Add the student to the team
            teams.get(targetTeam).addMember(student);
            workingCount[targetTeam]++;
        }
        
        System.out.println("After distribution, working students per team:");
        for (int i = 0; i < numTeams; i++) {
            System.out.println("Team " + (i+1) + ": " + workingCount[i] + " working students");
        }
        
        // STEP 3: Create temporary time zone groupings for remaining students
        Map<String, List<Student>> remainingByTimeZone = new HashMap<>();
        
        // Get remaining unassigned students
        for (String timeZone : timeZoneGroups) {
            if (studentsByTimeZone.containsKey(timeZone)) {
                List<Student> unassigned = studentsByTimeZone.get(timeZone).stream()
                        .filter(s -> !hasStudentBeenAssigned(s, teams))
                        .collect(Collectors.toList());
                
                if (!unassigned.isEmpty()) {
                    remainingByTimeZone.put(timeZone, unassigned);
                }
            }
        }
        
        // Calculate how many students each team should have for balanced distribution
        int targetSize = totalStudents / numTeams;
        int[] remainingSpots = new int[numTeams];
        
        // Calculate remaining spots in each team
        for (int i = 0; i < numTeams; i++) {
            remainingSpots[i] = targetSize - teams.get(i).getSize();
            // Adjust for teams that should have one extra student (to handle remainder)
            if (i < totalStudents % numTeams) {
                remainingSpots[i]++;
            }
        }
        
        // Assign remaining students by time zone compatibility, while keeping team sizes balanced
        for (String timeZone : timeZoneGroups) {
            if (remainingByTimeZone.containsKey(timeZone)) {
                List<Student> tzStudents = remainingByTimeZone.get(timeZone);
                System.out.println("Assigning " + tzStudents.size() + " remaining students from " + timeZone + " time zone");
                
                // For each student in this time zone
                for (Student student : tzStudents) {
                    if (hasStudentBeenAssigned(student, teams)) {
                        continue;
                    }
                    
                    // Try to find a team that:
                    // 1. Has compatible time zone students
                    // 2. Has room for more students
                    
                    // First, create a list of compatible time zones
                    List<String> compatibleZones = new ArrayList<>();
                    compatibleZones.add(timeZone); // Same time zone is most compatible
                    compatibleZones.addAll(getCompatibleTimeZones(timeZone)); // Add compatible zones
                    
                    // Find best team for this student
                    Team bestTeam = null;
                    double bestScore = -1;
                    
                    for (int i = 0; i < numTeams; i++) {
                        Team team = teams.get(i);
                        
                        // Skip teams that are already full
                        if (remainingSpots[i] <= 0) {
                            continue;
                        }
                        
                        // Calculate a score based on:
                        // - Time zone compatibility (higher is better)
                        // - Team size (more room is better)
                        
                        // Get current team time zone distribution
                        Map<String, Long> tzCounts = team.getMembers().stream()
                                .filter(s -> s.getTimeZone() != null)
                                .collect(Collectors.groupingBy(
                                        s -> normalizeTimeZone(s.getTimeZone()),
                                        Collectors.counting()));
                        
                        // Check time zone compatibility
                        double tzScore = 0;
                        for (int j = 0; j < compatibleZones.size(); j++) {
                            String tz = compatibleZones.get(j);
                            // Weight decreases with compatibility order
                            double weight = 1.0 / (j + 1);
                            tzScore += tzCounts.getOrDefault(tz, 0L) * weight;
                        }
                        
                        // Calculate size score (teams with more room get higher score)
                        double sizeScore = remainingSpots[i];
                        
                        // Combine scores (time zone compatibility is more important)
                        double totalScore = tzScore * 2 + sizeScore;
                        
                        if (totalScore > bestScore) {
                            bestScore = totalScore;
                            bestTeam = team;
                        }
                    }
                    
                    // If we found a team, assign the student
                    if (bestTeam != null) {
                        int teamIndex = teams.indexOf(bestTeam);
                        bestTeam.addMember(student);
                        remainingSpots[teamIndex]--;
                    } else {
                        // If no suitable team found, find team with most space
                        int maxIndex = 0;
                        for (int i = 1; i < numTeams; i++) {
                            if (remainingSpots[i] > remainingSpots[maxIndex]) {
                                maxIndex = i;
                            }
                        }
                        
                        teams.get(maxIndex).addMember(student);
                        remainingSpots[maxIndex]--;
                    }
                }
            }
        }
        
        // Final check for any unassigned students
        List<Student> finalRemaining = students.stream()
                .filter(s -> !hasStudentBeenAssigned(s, teams))
                .collect(Collectors.toList());
        
        System.out.println("Final check: " + finalRemaining.size() + " students still need assignment");
        
        // Assign any remaining students to teams with space
        for (Student student : finalRemaining) {
            int maxIndex = 0;
            for (int i = 1; i < numTeams; i++) {
                if (remainingSpots[i] > remainingSpots[maxIndex]) {
                    maxIndex = i;
                }
            }
            
            teams.get(maxIndex).addMember(student);
            remainingSpots[maxIndex]--;
        }
        
        // Set statistics for each team
        for (Team team : teams) {
            if (!team.getMembers().isEmpty()) {
                int workingStudentCount = (int) team.getMembers().stream()
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
                
                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, Working: %d, Previous Hackathon: %d, TimeZones: %s Total: %d", 
                    team.countByTrack("SDET"), 
                    team.countByTrack("DA"),
                    team.countByTrack("DVLPR"),
                    workingStudentCount,
                    withPreviousHackathonCount,
                    timeZoneStats.length() > 0 ? timeZoneStats.substring(0, timeZoneStats.length() - 2) : "None",
                    team.getSize()));
            }
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
    
    // Helper method to get compatible time zones
    private List<String> getCompatibleTimeZones(String timeZone) {
        switch (timeZone) {
            case "EST":
                return Collections.singletonList("CST");
            case "CST":
                return Arrays.asList("EST", "PST");
            case "PST":
                return Collections.singletonList("CST");
            default:
                return Arrays.asList("EST", "CST", "PST");
        }
    }
    
    // Helper method to assign students to teams with matching time zone
    private void assignStudentsToMatchingTeams(List<Student> students, List<Team> matchingTeams) {
        // Sort teams by size (smallest first)
        List<Team> sortedTeams = new ArrayList<>(matchingTeams);
        Collections.sort(sortedTeams, Comparator.comparingInt(Team::getSize));
        
        // Assign students to teams with matching time zone, starting with smallest teams
        for (Student student : new ArrayList<>(students)) {
            if (hasStudentBeenAssigned(student, sortedTeams)) {
                continue;
            }
            
            // Find the smallest team
            Team targetTeam = sortedTeams.stream()
                    .min(Comparator.comparingInt(Team::getSize))
                    .orElse(null);
            
            if (targetTeam != null) {
                targetTeam.addMember(student);
                students.remove(student);
            }
        }
    }
    
    // Helper method to group teams by dominant time zone
    private Map<String, List<Team>> getTeamsByDominantTimeZone(List<Team> teams) {
        Map<String, List<Team>> teamsByTimeZone = new HashMap<>();
        
        for (Team team : teams) {
            // Find dominant time zone
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
            
            // Add team to the map under its dominant time zone
            teamsByTimeZone.computeIfAbsent(dominantTimeZone, k -> new ArrayList<>()).add(team);
        }
        
        // Add teams with no members yet to the NONE category
        for (Team team : teams) {
            if (team.getMembers().isEmpty()) {
                teamsByTimeZone.computeIfAbsent("NONE", k -> new ArrayList<>()).add(team);
            }
        }
        
        return teamsByTimeZone;
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
    
    /**
     * Find the best team for a student considering working status count and batch diversity.
     * For SQL Bootcamp, we don't enforce batch diversity.
     * For all other events, we try to avoid placing students from the same batch on the same team.
     * 
     * @param teams List of teams
     * @param student Student to place
     * @param workingCount Array with count of working students per team
     * @param numTeams Number of teams
     * @return Index of the best team for this student
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
        int numTeams = (totalStudents + HACKATHON_TEAM_SIZE - 1) / HACKATHON_TEAM_SIZE; // Ceiling division
        
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
    
    private String generateApiHackathonSummary(List<Team> teams, List<Student> unassignedStudents, EventType eventType) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Created %d teams for %s\n", teams.size(), eventType.getDisplayName()));
        summary.append(String.format("Total students: %d\n", 
                teams.stream().mapToInt(Team::getSize).sum() + unassignedStudents.size()));
        
        int sdetCount = teams.stream().mapToInt(t -> t.countByTrack("SDET")).sum();
        int daCount = teams.stream().mapToInt(t -> t.countByTrack("DA")).sum();
        int dvlprCount = teams.stream().mapToInt(t -> t.countByTrack("DVLPR")).sum();
        int workingCount = 0;
        int prevHackathonCount = 0;
        
        for (Team team : teams) {
            workingCount += team.getMembers().stream()
                    .filter(s -> s.getWorkingStatus() != null && 
                            s.getWorkingStatus().toLowerCase().contains("yes"))
                    .count();
            
            prevHackathonCount += team.getMembers().stream()
                    .filter(s -> s.getPreviousHackathon() != null && 
                            s.getPreviousHackathon().toLowerCase().contains("yes"))
                    .count();
        }
        
        if (eventType == EventType.PHASE1_API_HACKATHON) {
            summary.append(String.format("Distribution - SDET: %d, DA: %d, DVLPR: %d, Working: %d, Previous API Hackathon: %d\n", 
                    sdetCount, daCount, dvlprCount, workingCount, prevHackathonCount));
        } else { // Phase 2 API Hackathon
            summary.append(String.format("Distribution - SDET: %d, DA: %d, DVLPR: %d, Working: %d, Previous API Hackathon: %d\n", 
                    sdetCount, daCount, dvlprCount, workingCount, prevHackathonCount));
        }
        
        if (!unassignedStudents.isEmpty()) {
            summary.append(String.format("Unassigned students: %d\n", unassignedStudents.size()));
        }
        
        return summary.toString();
    }
    
    private List<Team> formApiHackathonTeams(List<Student> students, EventType eventType, boolean distributeDATrack) {
        List<Team> teams = new ArrayList<>();
        
        System.out.println("Forming teams for " + eventType.getDisplayName() + " with " + students.size() + " students");
        
        // Calculate the optimal number of teams based on team size (aiming for 5 students per team)
        int totalStudents = students.size();
        int studentsPerTeam = HACKATHON_TEAM_SIZE; // Using the hackathon team size constant
        int numTeams = Math.max(totalStudents / studentsPerTeam, 1); // At least 1 team
        
        // Adjust number of teams to ensure more even distribution
        // If we have remainder, see if we should add another team or stick with current number
        if (totalStudents % studentsPerTeam > 0) {
            // Calculate average team size with current number of teams vs adding one more team
            double currentAvg = (double) totalStudents / numTeams;
            double nextAvg = (double) totalStudents / (numTeams + 1);
            
            // If adding another team makes the distribution more even, do it
            if (Math.abs(nextAvg - studentsPerTeam) < Math.abs(currentAvg - studentsPerTeam)) {
                numTeams++;
            }
        }
        
        System.out.println("Creating " + numTeams + " teams with approximately " + 
                           (totalStudents / numTeams) + " students per team");
        
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
        
        // Count students by timezone
        studentsByTimeZone.forEach((tz, list) -> 
                System.out.println("Time zone " + tz + ": " + list.size() + " students"));
        
        // List of main time zone groups (in order of compatibility)
        List<String> timeZoneGroups = Arrays.asList("EST", "CST", "PST", "OTHER");
        
        // STEP 1: First, distribute students with previous API hackathon experience evenly
        List<Student> withPreviousHackathon = students.stream()
                .filter(s -> s.getPreviousHackathon() != null && 
                            s.getPreviousHackathon().toLowerCase().contains("yes"))
                .collect(Collectors.toList());
        
        System.out.println("Found " + withPreviousHackathon.size() + 
                          " students with previous API hackathon experience");
        
        // Randomize experienced students to ensure fair distribution
        Collections.shuffle(withPreviousHackathon);
        
        // Initialize a counter for each team's experienced students
        int[] expCount = new int[numTeams];
        
        // Distribute experienced students evenly across teams
        for (Student student : withPreviousHackathon) {
            // Find the team with the fewest experienced students
            int targetTeam = 0;
            for (int i = 1; i < numTeams; i++) {
                if (expCount[i] < expCount[targetTeam]) {
                    targetTeam = i;
                }
            }
            
            // Add the student to the team
            teams.get(targetTeam).addMember(student);
            expCount[targetTeam]++;
        }
        
        // STEP 2: Count ALL working students (including those already assigned in step 1)
        // First, count working students already assigned in step 1
        int[] workingCount = new int[numTeams];
        for (int i = 0; i < numTeams; i++) {
            Team team = teams.get(i);
            workingCount[i] = (int) team.getMembers().stream()
                .filter(s -> s.getWorkingStatus() != null && 
                        s.getWorkingStatus().toLowerCase().contains("yes"))
                .count();
        }
        
        // Get working students not yet assigned
        List<Student> workingStudents = students.stream()
                .filter(s -> s.getWorkingStatus() != null && 
                        s.getWorkingStatus().toLowerCase().contains("yes"))
                .filter(s -> !hasStudentBeenAssigned(s, teams)) // Skip if already assigned
                .collect(Collectors.toList());
        
        System.out.println("Found " + workingStudents.size() + " working students not yet assigned");
        System.out.println("Current working students per team:");
        for (int i = 0; i < numTeams; i++) {
            System.out.println("Team " + (i+1) + ": " + workingCount[i] + " working students");
        }
        
        // Randomize working students to ensure fair distribution
        Collections.shuffle(workingStudents);
        
        // Distribute working students evenly across teams
        for (Student student : workingStudents) {
            // Find the team with the fewest working students
            int targetTeam = 0;
            for (int i = 1; i < numTeams; i++) {
                if (workingCount[i] < workingCount[targetTeam]) {
                    targetTeam = i;
                }
            }
            
            // Add the student to the team
            teams.get(targetTeam).addMember(student);
            workingCount[targetTeam]++;
        }
        
        System.out.println("After distribution, working students per team:");
        for (int i = 0; i < numTeams; i++) {
            System.out.println("Team " + (i+1) + ": " + workingCount[i] + " working students");
        }
        
        // STEP 3: Create temporary time zone groupings for remaining students
        Map<String, List<Student>> remainingByTimeZone = new HashMap<>();
        
        // Get remaining unassigned students
        for (String timeZone : timeZoneGroups) {
            if (studentsByTimeZone.containsKey(timeZone)) {
                List<Student> unassigned = studentsByTimeZone.get(timeZone).stream()
                        .filter(s -> !hasStudentBeenAssigned(s, teams))
                        .collect(Collectors.toList());
                
                if (!unassigned.isEmpty()) {
                    remainingByTimeZone.put(timeZone, unassigned);
                }
            }
        }
        
        // STEP 4: Distribute DA and/or DVLPR track students evenly
        if (distributeDATrack) { // Phase 1 API Hackathon
            // Get all unassigned DA students
            List<Student> daStudents = students.stream()
                    .filter(s -> "DA".equalsIgnoreCase(s.getTrack()))
                    .filter(s -> !hasStudentBeenAssigned(s, teams))
                    .collect(Collectors.toList());
            
            // Get all unassigned DVLPR students
            List<Student> dvlprStudents = students.stream()
                    .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                    .filter(s -> !hasStudentBeenAssigned(s, teams))
                    .collect(Collectors.toList());
            
            System.out.println("Found " + daStudents.size() + " unassigned DA students");
            System.out.println("Found " + dvlprStudents.size() + " unassigned DVLPR students");
            
            // Track DA and DVLPR counts per team
            int[] daTrackCount = new int[numTeams];
            int[] dvlprTrackCount = new int[numTeams];
            
            // Count DA and DVLPR students already assigned
            for (int i = 0; i < numTeams; i++) {
                Team team = teams.get(i);
                daTrackCount[i] = (int) team.getMembers().stream()
                    .filter(s -> s.getTrack() != null && s.getTrack().equalsIgnoreCase("DA"))
                    .count();
                
                dvlprTrackCount[i] = (int) team.getMembers().stream()
                    .filter(s -> s.getTrack() != null && s.getTrack().equalsIgnoreCase("DVLPR"))
                    .count();
            }
            
            // Distribute DA students evenly
            Collections.shuffle(daStudents);
            for (Student student : daStudents) {
                // Find team with fewest DA students
                int targetTeam = 0;
                for (int i = 1; i < numTeams; i++) {
                    if (daTrackCount[i] < daTrackCount[targetTeam]) {
                        targetTeam = i;
                    }
                }
                
                teams.get(targetTeam).addMember(student);
                daTrackCount[targetTeam]++;
                
                // Remove from time zone group
                for (List<Student> tzStudents : remainingByTimeZone.values()) {
                    tzStudents.remove(student);
                }
            }
            
            // Distribute DVLPR students evenly
            Collections.shuffle(dvlprStudents);
            for (Student student : dvlprStudents) {
                // Find team with fewest DVLPR students
                int targetTeam = 0;
                for (int i = 1; i < numTeams; i++) {
                    if (dvlprTrackCount[i] < dvlprTrackCount[targetTeam]) {
                        targetTeam = i;
                    }
                }
                
                teams.get(targetTeam).addMember(student);
                dvlprTrackCount[targetTeam]++;
                
                // Remove from time zone group
                for (List<Student> tzStudents : remainingByTimeZone.values()) {
                    tzStudents.remove(student);
                }
            }
        } else { // Phase 2 API Hackathon - only distribute DVLPR students
            // Get all unassigned DVLPR students
            List<Student> dvlprStudents = students.stream()
                    .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                    .filter(s -> !hasStudentBeenAssigned(s, teams))
                    .collect(Collectors.toList());
            
            System.out.println("Found " + dvlprStudents.size() + " unassigned DVLPR students");
            
            // Track DVLPR counts per team
            int[] dvlprTrackCount = new int[numTeams];
            
            // Count DVLPR students already assigned
            for (int i = 0; i < numTeams; i++) {
                Team team = teams.get(i);
                dvlprTrackCount[i] = (int) team.getMembers().stream()
                    .filter(s -> s.getTrack() != null && s.getTrack().equalsIgnoreCase("DVLPR"))
                    .count();
            }
            
            // Distribute DVLPR students evenly
            Collections.shuffle(dvlprStudents);
            for (Student student : dvlprStudents) {
                // Find team with fewest DVLPR students
                int targetTeam = 0;
                for (int i = 1; i < numTeams; i++) {
                    if (dvlprTrackCount[i] < dvlprTrackCount[targetTeam]) {
                        targetTeam = i;
                    }
                }
                
                teams.get(targetTeam).addMember(student);
                dvlprTrackCount[targetTeam]++;
                
                // Remove from time zone group
                for (List<Student> tzStudents : remainingByTimeZone.values()) {
                    tzStudents.remove(student);
                }
            }
        }
        
        // Calculate how many students each team should have for balanced distribution
        int targetSize = totalStudents / numTeams;
        int[] remainingSpots = new int[numTeams];
        
        // Calculate remaining spots in each team
        for (int i = 0; i < numTeams; i++) {
            remainingSpots[i] = targetSize - teams.get(i).getSize();
            // Adjust for teams that should have one extra student (to handle remainder)
            if (i < totalStudents % numTeams) {
                remainingSpots[i]++;
            }
        }
        
        // STEP 5: Assign remaining students by time zone compatibility, while keeping team sizes balanced
        for (String timeZone : timeZoneGroups) {
            if (remainingByTimeZone.containsKey(timeZone)) {
                List<Student> tzStudents = remainingByTimeZone.get(timeZone);
                System.out.println("Assigning " + tzStudents.size() + " remaining students from " + timeZone + " time zone");
                
                // For each student in this time zone
                for (Student student : tzStudents) {
                    if (hasStudentBeenAssigned(student, teams)) {
                        continue;
                    }
                    
                    // Try to find a team that:
                    // 1. Has compatible time zone students
                    // 2. Has room for more students
                    
                    // First, create a list of compatible time zones
                    List<String> compatibleZones = new ArrayList<>();
                    compatibleZones.add(timeZone); // Same time zone is most compatible
                    compatibleZones.addAll(getCompatibleTimeZones(timeZone)); // Add compatible zones
                    
                    // Find best team for this student
                    Team bestTeam = null;
                    double bestScore = -1;
                    
                    for (int i = 0; i < numTeams; i++) {
                        Team team = teams.get(i);
                        
                        // Skip teams that are already full
                        if (remainingSpots[i] <= 0) {
                            continue;
                        }
                        
                        // Calculate a score based on:
                        // - Time zone compatibility (higher is better)
                        // - Team size (more room is better)
                        
                        // Get current team time zone distribution
                        Map<String, Long> tzCounts = team.getMembers().stream()
                                .filter(s -> s.getTimeZone() != null)
                                .collect(Collectors.groupingBy(
                                        s -> normalizeTimeZone(s.getTimeZone()),
                                        Collectors.counting()));
                        
                        // Check time zone compatibility
                        double tzScore = 0;
                        for (int j = 0; j < compatibleZones.size(); j++) {
                            String tz = compatibleZones.get(j);
                            // Weight decreases with compatibility order
                            double weight = 1.0 / (j + 1);
                            tzScore += tzCounts.getOrDefault(tz, 0L) * weight;
                        }
                        
                        // Calculate size score (teams with more room get higher score)
                        double sizeScore = remainingSpots[i];
                        
                        // Combine scores (time zone compatibility is more important)
                        double totalScore = tzScore * 2 + sizeScore;
                        
                        if (totalScore > bestScore) {
                            bestScore = totalScore;
                            bestTeam = team;
                        }
                    }
                    
                    // If we found a team, assign the student
                    if (bestTeam != null) {
                        int teamIndex = teams.indexOf(bestTeam);
                        bestTeam.addMember(student);
                        remainingSpots[teamIndex]--;
                    } else {
                        // If no suitable team found, find team with most space
                        int maxIndex = 0;
                        for (int i = 1; i < numTeams; i++) {
                            if (remainingSpots[i] > remainingSpots[maxIndex]) {
                                maxIndex = i;
                            }
                        }
                        
                        teams.get(maxIndex).addMember(student);
                        remainingSpots[maxIndex]--;
                    }
                }
            }
        }
        
        // Final check for any unassigned students
        List<Student> finalRemaining = students.stream()
                .filter(s -> !hasStudentBeenAssigned(s, teams))
                .collect(Collectors.toList());
        
        System.out.println("Final check: " + finalRemaining.size() + " students still need assignment");
        
        // Assign any remaining students to teams with space
        for (Student student : finalRemaining) {
            int maxIndex = 0;
            for (int i = 1; i < numTeams; i++) {
                if (remainingSpots[i] > remainingSpots[maxIndex]) {
                    maxIndex = i;
                }
            }
            
            teams.get(maxIndex).addMember(student);
            remainingSpots[maxIndex]--;
        }
        
        // Set statistics for each team
        for (Team team : teams) {
            if (!team.getMembers().isEmpty()) {
                int workingStudentCount = (int) team.getMembers().stream()
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
                
                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, Working: %d, Previous API Hackathon: %d, TimeZones: %s Total: %d", 
                    team.countByTrack("SDET"), 
                    team.countByTrack("DA"),
                    team.countByTrack("DVLPR"),
                    workingStudentCount,
                    withPreviousHackathonCount,
                    timeZoneStats.length() > 0 ? timeZoneStats.substring(0, timeZoneStats.length() - 2) : "None",
                    team.getSize()));
            }
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
}
