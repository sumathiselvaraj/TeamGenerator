package com.teamformation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    // Get distributions for reporting
    public String getAssignmentRate() {
        if (totalStudents == 0) {
            return "0%";
        }
        double rate = (double) assignedStudents / totalStudents * 100;
        return String.format("%.1f%%", rate);
    }
}