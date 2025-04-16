package com.teamformation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private String name;
    private String teamType;
    private List<Student> students;
    
    public int getMemberCount() {
        return students != null ? students.size() : 0;
    }
    
    public int getSdetCount() {
        return students != null ? (int) students.stream()
                .filter(s -> "SDET".equals(s.getTrack()))
                .count() : 0;
    }
    
    public int getDaCount() {
        return students != null ? (int) students.stream()
                .filter(s -> "DA".equals(s.getTrack()))
                .count() : 0;
    }
    
    public List<Student> getSortedStudents() {
        if (students == null) {
            return List.of();
        }
        
        return students.stream()
                .sorted((s1, s2) -> {
                    // First sort by track
                    int trackCompare = s1.getTrack().compareTo(s2.getTrack());
                    if (trackCompare != 0) {
                        return trackCompare;
                    }
                    // Then by name
                    return s1.getName().compareTo(s2.getName());
                })
                .collect(Collectors.toList());
    }
}
