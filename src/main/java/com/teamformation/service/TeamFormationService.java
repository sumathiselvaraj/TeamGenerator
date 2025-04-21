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

    private static final int SQL_BOOTCAMP_FULL_COURSE_TEAM_SIZE = 7; // SQL Bootcamp Full Course uses 7-member teams
    private static final int SQL_BOOTCAMP_ADVANCED_TEAM_SIZE = 5; // SQL Bootcamp Advanced Course uses 5-member teams
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
        } else if (eventType == EventType.SQL_HACKATHON || eventType == EventType.PYTHON_HACKATHON) {
            teams = formSqlHackathonTeams(students);
            unassignedStudents = findUnassignedStudents(students, teams);
            
            if (eventType == EventType.SQL_HACKATHON) {
                summary = generateSqlHackathonSummary(teams, unassignedStudents);
            } else { // Python Hackathon
                summary = generatePythonHackathonSummary(teams, unassignedStudents);
            }
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

        // Calculate number of teams needed for advanced students (5 members per team)
        int advancedTeamCount = Math.max(1, (advancedStudents.size() + SQL_BOOTCAMP_ADVANCED_TEAM_SIZE - 1) / SQL_BOOTCAMP_ADVANCED_TEAM_SIZE);

        // Create advanced course teams
        for (int i = 0; i < advancedTeamCount; i++) {
            Team team = Team.builder()
                    .name("Advanced Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            advancedTeams.add(team);
        }

        // Calculate number of teams needed for full course students (7 members per team)
        int fullTeamCount = Math.max(1, (fullStudents.size() + SQL_BOOTCAMP_FULL_COURSE_TEAM_SIZE - 1) / SQL_BOOTCAMP_FULL_COURSE_TEAM_SIZE);

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
                
        List<Student> smpoStudents = fullStudents.stream()
                .filter(s -> "SMPO".equalsIgnoreCase(s.getTrack()))
                .collect(Collectors.toList());

        System.out.println("Full course: " + sdetStudents.size() + " SDET students, " + 
                           daStudents.size() + " DA students, " + 
                           dvlprStudents.size() + " DVLPR students, " +
                           smpoStudents.size() + " SMPO students");

        // Try to distribute SDET, DA, DVLPR and SMPO students to balance each team's ratio
        Collections.shuffle(sdetStudents);
        Collections.shuffle(daStudents);
        Collections.shuffle(dvlprStudents);
        Collections.shuffle(smpoStudents);

        // Try to ensure one DVLPR per team first
        int dvlprPerTeam = Math.min(1, dvlprStudents.size() / fullTeamCount);
        
        // Try to ensure one SMPO per team if available
        int smpoPerTeam = Math.min(1, smpoStudents.size() / fullTeamCount);

        // First, distribute one DVLPR to each team (if available)
        for (int i = 0; i < fullTeamCount && !dvlprStudents.isEmpty(); i++) {
            Team team = fullTeams.get(i);
            if (dvlprPerTeam > 0) {
                team.addMember(dvlprStudents.remove(0));
            }
        }
        
        // Then, distribute one SMPO to each team (if available)
        for (int i = 0; i < fullTeamCount && !smpoStudents.isEmpty(); i++) {
            Team team = fullTeams.get(i);
            if (smpoPerTeam > 0) {
                team.addMember(smpoStudents.remove(0));
            }
        }

        // Basic strategy: try to assign equal numbers of each track to each team
        // While ensuring we don't exceed the maximum team size limit
        for (int i = 0; i < fullTeamCount; i++) {
            Team team = fullTeams.get(i);
            
            // Get current team size (should include any DVLPR already added)
            int currentTeamSize = team.getSize();
            
            // Calculate remaining space in this team
            int remainingSpace = SQL_BOOTCAMP_FULL_COURSE_TEAM_SIZE - currentTeamSize;
            
            if (remainingSpace <= 0) {
                continue; // Skip this team if it's already at max capacity
            }

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
            
            // Make sure we don't exceed the team size limit with SDET + DA
            if (sdetPerTeam + daPerTeam > remainingSpace) {
                // Adjust numbers proportionally if they exceed the limit
                int totalNeeded = sdetPerTeam + daPerTeam;
                double proportion = (double) remainingSpace / totalNeeded;
                
                // Ensure at least one of each if possible
                int newSdetPerTeam = Math.max(1, (int) Math.round(sdetPerTeam * proportion));
                int newDaPerTeam = Math.max(1, (int) Math.round(daPerTeam * proportion));
                
                // Final check to ensure we don't exceed the limit
                if (newSdetPerTeam + newDaPerTeam > remainingSpace) {
                    // If still over, reduce the larger group by the excess
                    int excess = (newSdetPerTeam + newDaPerTeam) - remainingSpace;
                    if (newSdetPerTeam >= newDaPerTeam) {
                        newSdetPerTeam -= excess;
                    } else {
                        newDaPerTeam -= excess;
                    }
                }
                
                sdetPerTeam = newSdetPerTeam;
                daPerTeam = newDaPerTeam;
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
        // But ensure we respect the maximum team size limit of 7 members for full course teams
        List<Student> remainingStudents = new ArrayList<>();
        remainingStudents.addAll(sdetStudents);
        remainingStudents.addAll(daStudents);
        remainingStudents.addAll(dvlprStudents);
        remainingStudents.addAll(smpoStudents);

        for (Student student : remainingStudents) {
            // Find team with fewest members that hasn't reached max size
            Team targetTeam = fullTeams.stream()
                    .filter(team -> team.getSize() < SQL_BOOTCAMP_FULL_COURSE_TEAM_SIZE) // Only consider teams below max size
                    .min(Comparator.comparingInt(Team::getSize))
                    .orElse(null);
            
            // If all teams are at capacity, create a new team
            if (targetTeam == null) {
                targetTeam = Team.builder()
                        .name("Full Course Team " + (fullTeams.size() + 1))
                        .members(new ArrayList<>())
                        .build();
                fullTeams.add(targetTeam);
            }
            
            targetTeam.addMember(student);
        }

        // Set statistics for each team
        for (Team team : advancedTeams) {
            if (!team.getMembers().isEmpty()) {
                int dvlprCount = (int) team.getMembers().stream()
                        .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                        .count();
                        
                int smpoCount = (int) team.getMembers().stream()
                        .filter(s -> "SMPO".equalsIgnoreCase(s.getTrack()))
                        .count();

                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, SMPO: %d, Total: %d", 
                    team.getSdetCount(), 
                    team.getDaCount(),
                    dvlprCount,
                    smpoCount,
                    team.getSize()));
            }
        }

        for (Team team : fullTeams) {
            if (!team.getMembers().isEmpty()) {
                int dvlprCount = (int) team.getMembers().stream()
                        .filter(s -> "DVLPR".equalsIgnoreCase(s.getTrack()))
                        .count();
                        
                int smpoCount = (int) team.getMembers().stream()
                        .filter(s -> "SMPO".equalsIgnoreCase(s.getTrack()))
                        .count();

                team.setStatistics(String.format("SDET: %d, DA: %d, DVLPR: %d, SMPO: %d, Total: %d", 
                    team.getSdetCount(), 
                    team.getDaCount(),
                    dvlprCount,
                    smpoCount,
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

    private List<Team> formSqlHackathonTeams(List<Student> students) {
        List<Team> teams = new ArrayList<>();
        
        System.out.println("Forming teams for Hackathon with " + students.size() + " students");
        
        // Calculate the optimal number of teams based on team size (aiming for 5 students per team)
        int totalStudents = students.size();
        int optimalTeamCount = Math.max(1, (totalStudents + HACKATHON_TEAM_SIZE - 1) / HACKATHON_TEAM_SIZE);

        System.out.println("Creating " + optimalTeamCount + " teams for Hackathon");
        
        // Create empty teams with the correct event type name
        for (int i = 0; i < optimalTeamCount; i++) {
            String eventName = "Hackathon";
            
            // Get the event type from the first student's course type
            if (!students.isEmpty() && students.get(0).getCourseType() != null) {
                if (students.get(0).getCourseType().contains("Python")) {
                    eventName = "Python";
                } else if (students.get(0).getCourseType().contains("SQL")) {
                    eventName = "SQL";
                }
            }
            
            Team team = Team.builder()
                    .name(eventName + " Team " + (i + 1))
                    .members(new ArrayList<>())
                    .build();
            teams.add(team);
        }
        
        // Group students by SQL expertise level
        Map<String, List<Student>> expertiseGroups = new HashMap<>();
        expertiseGroups.put("Advanced", new ArrayList<>());
        expertiseGroups.put("Intermediate", new ArrayList<>());
        expertiseGroups.put("Beginner", new ArrayList<>());
        
        for (Student student : students) {
            String expertise = student.getSqlExpertiseLevel();
            if (expertise == null) {
                expertise = "Beginner"; // Default if not specified
            }
            
            // Add to the appropriate group
            if (expertise.contains("Advanced")) {
                expertiseGroups.get("Advanced").add(student);
            } else if (expertise.contains("Intermediate")) {
                expertiseGroups.get("Intermediate").add(student);
            } else {
                expertiseGroups.get("Beginner").add(student);
            }
        }
        
        // Group students by track
        Map<String, List<Student>> trackGroups = new HashMap<>();
        trackGroups.put("SDET", new ArrayList<>());
        trackGroups.put("DA", new ArrayList<>());
        trackGroups.put("DVLPR", new ArrayList<>());
        trackGroups.put("SMPO", new ArrayList<>());
        
        for (Student student : students) {
            String track = student.getTrack();
            if (track == null || track.isEmpty()) {
                track = "Unknown";
            }
            
            // Ensure we have a list for this track
            if (!trackGroups.containsKey(track)) {
                trackGroups.put(track, new ArrayList<>());
            }
            
            trackGroups.get(track).add(student);
        }
        
        System.out.println("Expertise distribution: " +
                "Advanced: " + expertiseGroups.get("Advanced").size() + ", " +
                "Intermediate: " + expertiseGroups.get("Intermediate").size() + ", " +
                "Beginner: " + expertiseGroups.get("Beginner").size());
        
        System.out.println("Track distribution: " +
                "SDET: " + trackGroups.getOrDefault("SDET", Collections.emptyList()).size() + ", " +
                "DA: " + trackGroups.getOrDefault("DA", Collections.emptyList()).size() + ", " +
                "DVLPR: " + trackGroups.getOrDefault("DVLPR", Collections.emptyList()).size() + ", " +
                "SMPO: " + trackGroups.getOrDefault("SMPO", Collections.emptyList()).size());
        
        // Distribute expertise levels across teams
        distributeStudents(teams, expertiseGroups.get("Advanced"), "Advanced");
        distributeStudents(teams, expertiseGroups.get("Intermediate"), "Intermediate");
        distributeStudents(teams, expertiseGroups.get("Beginner"), "Beginner");
        
        // Check if we still have space in teams
        boolean teamsHaveSpace = teams.stream().anyMatch(team -> team.getSize() < HACKATHON_TEAM_SIZE);
        
        // If we still have space, distribute by tracks to ensure track diversity
        if (teamsHaveSpace) {
            // Get students not yet assigned
            Set<String> assignedStudentEmails = new HashSet<>();
            for (Team team : teams) {
                for (Student student : team.getMembers()) {
                    assignedStudentEmails.add(student.getEmail().toLowerCase());
                }
            }
            
            // Prepare lists of unassigned students by track
            for (String track : trackGroups.keySet()) {
                List<Student> trackStudents = trackGroups.get(track);
                List<Student> unassignedTrackStudents = trackStudents.stream()
                        .filter(student -> !assignedStudentEmails.contains(student.getEmail().toLowerCase()))
                        .collect(Collectors.toList());
                
                // Distribute these students
                if (!unassignedTrackStudents.isEmpty()) {
                    distributeStudents(teams, unassignedTrackStudents, track);
                }
            }
        }
        
        // Set statistics for each team
        for (Team team : teams) {
            Map<String, Integer> expertiseCounts = countAttributeInTeam(team, "sqlExpertiseLevel");
            Map<String, Integer> trackCounts = countAttributeInTeam(team, "track");
            
            StringBuilder stats = new StringBuilder();
            
            // Expertise stats
            stats.append("Expertise: ");
            stats.append("Advanced: ").append(expertiseCounts.getOrDefault("Advanced", 0)).append(", ");
            stats.append("Intermediate: ").append(expertiseCounts.getOrDefault("Intermediate", 0)).append(", ");
            stats.append("Beginner: ").append(expertiseCounts.getOrDefault("Beginner", 0)).append(" | ");
            
            // Track stats
            stats.append("Tracks: ");
            stats.append("SDET: ").append(trackCounts.getOrDefault("SDET", 0)).append(", ");
            stats.append("DA: ").append(trackCounts.getOrDefault("DA", 0)).append(", ");
            stats.append("DVLPR: ").append(trackCounts.getOrDefault("DVLPR", 0)).append(", ");
            stats.append("SMPO: ").append(trackCounts.getOrDefault("SMPO", 0));
            
            team.setStatistics(stats.toString());
        }
        
        // Remove any empty teams
        teams.removeIf(team -> team.getMembers().isEmpty());
        
        return teams;
    }
    
    /**
     * Helper method to count occurrences of various attribute values in a team
     */
    private Map<String, Integer> countAttributeInTeam(Team team, String attributeName) {
        Map<String, Integer> counts = new HashMap<>();
        
        for (Student student : team.getMembers()) {
            String value = "";
            
            // Get the appropriate field value based on attributeName
            if ("sqlExpertiseLevel".equals(attributeName)) {
                value = student.getSqlExpertiseLevel();
                if (value == null) {
                    value = "Beginner"; // Default value
                }
            } else if ("track".equals(attributeName)) {
                value = student.getTrack();
                if (value == null) {
                    value = "Unknown";
                }
            }
            
            // Update counts
            counts.put(value, counts.getOrDefault(value, 0) + 1);
        }
        
        return counts;
    }
    
    /**
     * Helper method to distribute students across teams
     */
    private void distributeStudents(List<Team> teams, List<Student> students, String category) {
        if (students.isEmpty() || teams.isEmpty()) {
            return;
        }
        
        System.out.println("Distributing " + students.size() + " " + category + " students across " + teams.size() + " teams");
        
        // Randomize students to avoid patterns
        Collections.shuffle(students);
        
        // Track assigned students to avoid duplicates
        Set<String> assignedEmails = new HashSet<>();
        for (Team team : teams) {
            for (Student member : team.getMembers()) {
                assignedEmails.add(member.getEmail().toLowerCase());
            }
        }
        
        // Remove already assigned students
        List<Student> unassignedStudents = students.stream()
                .filter(student -> !assignedEmails.contains(student.getEmail().toLowerCase()))
                .collect(Collectors.toList());
        
        if (unassignedStudents.isEmpty()) {
            return;
        }
        
        System.out.println(unassignedStudents.size() + " unassigned " + category + " students to distribute");
        
        // First distribute students among teams that have space
        for (Student student : unassignedStudents) {
            // Find the team with the lowest number of students in this category
            // that hasn't reached the maximum team size
            Team targetTeam = null;
            int minStudentsOfCategory = Integer.MAX_VALUE;
            
            for (Team team : teams) {
                if (team.getSize() >= HACKATHON_TEAM_SIZE) {
                    continue; // Skip teams at capacity
                }
                
                int studentsOfCategory;
                if ("Advanced".equals(category) || "Intermediate".equals(category) || "Beginner".equals(category)) {
                    studentsOfCategory = (int) team.getMembers().stream()
                            .filter(s -> {
                                String expertise = s.getSqlExpertiseLevel();
                                return expertise != null && expertise.contains(category);
                            })
                            .count();
                } else {
                    // It's a track category
                    studentsOfCategory = (int) team.getMembers().stream()
                            .filter(s -> {
                                String track = s.getTrack();
                                return track != null && track.equals(category);
                            })
                            .count();
                }
                
                if (studentsOfCategory < minStudentsOfCategory) {
                    minStudentsOfCategory = studentsOfCategory;
                    targetTeam = team;
                }
            }
            
            // If we found a team with space, add the student to it
            if (targetTeam != null) {
                targetTeam.addMember(student);
                assignedEmails.add(student.getEmail().toLowerCase());
            }
        }
    }
    
    private String generateSqlHackathonSummary(List<Team> teams, List<Student> unassignedStudents) {
        StringBuilder summary = new StringBuilder();
        summary.append("SQL Hackathon Team Formation Results:\n");
        summary.append("Total teams: ").append(teams.size()).append("\n");
        summary.append("Total assigned students: ").append(teams.stream().mapToInt(Team::getSize).sum()).append("\n");
        
        if (!unassignedStudents.isEmpty()) {
            summary.append("Unassigned students: ").append(unassignedStudents.size()).append("\n");
        }
        
        // Expertise level distribution
        Map<String, Integer> expertiseCounts = new HashMap<>();
        for (Team team : teams) {
            for (Student student : team.getMembers()) {
                String expertise = student.getSqlExpertiseLevel();
                if (expertise == null) {
                    expertise = "Beginner";
                }
                expertiseCounts.put(expertise, expertiseCounts.getOrDefault(expertise, 0) + 1);
            }
        }
        
        summary.append("\nExpertise Distribution:\n");
        summary.append("Advanced: ").append(expertiseCounts.getOrDefault("Advanced", 0)).append("\n");
        summary.append("Intermediate: ").append(expertiseCounts.getOrDefault("Intermediate", 0)).append("\n");
        summary.append("Beginner: ").append(expertiseCounts.getOrDefault("Beginner", 0)).append("\n");
        
        return summary.toString();
    }
    
    private String generatePythonHackathonSummary(List<Team> teams, List<Student> unassignedStudents) {
        StringBuilder summary = new StringBuilder();
        summary.append("Python Hackathon Team Formation Results:\n");
        summary.append("Total teams: ").append(teams.size()).append("\n");
        summary.append("Total assigned students: ").append(teams.stream().mapToInt(Team::getSize).sum()).append("\n");
        
        if (!unassignedStudents.isEmpty()) {
            summary.append("Unassigned students: ").append(unassignedStudents.size()).append("\n");
        }
        
        // Expertise level distribution
        Map<String, Integer> expertiseCounts = new HashMap<>();
        for (Team team : teams) {
            for (Student student : team.getMembers()) {
                String expertise = student.getSqlExpertiseLevel();
                if (expertise == null) {
                    expertise = "Beginner";
                }
                expertiseCounts.put(expertise, expertiseCounts.getOrDefault(expertise, 0) + 1);
            }
        }
        
        summary.append("\nExpertise Distribution:\n");
        summary.append("Advanced: ").append(expertiseCounts.getOrDefault("Advanced", 0)).append("\n");
        summary.append("Intermediate: ").append(expertiseCounts.getOrDefault("Intermediate", 0)).append("\n");
        summary.append("Beginner: ").append(expertiseCounts.getOrDefault("Beginner", 0)).append("\n");
        
        // Track distribution
        Map<String, Integer> trackCounts = new HashMap<>();
        for (Team team : teams) {
            for (Student student : team.getMembers()) {
                String track = student.getTrack();
                if (track == null || track.isEmpty()) {
                    track = "Unknown";
                }
                trackCounts.put(track, trackCounts.getOrDefault(track, 0) + 1);
            }
        }
        
        summary.append("\nTrack Distribution:\n");
        summary.append("SDET: ").append(trackCounts.getOrDefault("SDET", 0)).append("\n");
        summary.append("DA: ").append(trackCounts.getOrDefault("DA", 0)).append("\n");
        summary.append("DVLPR: ").append(trackCounts.getOrDefault("DVLPR", 0)).append("\n");
        summary.append("SMPO: ").append(trackCounts.getOrDefault("SMPO", 0)).append("\n");
        
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
