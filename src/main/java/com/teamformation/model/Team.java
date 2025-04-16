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
public class Team {
    private String name;
    @Builder.Default
    private List<Student> members = new ArrayList<>();
    private String statistics;
    
    public void addMember(Student student) {
        members.add(student);
    }
    
    public int getSize() {
        return members.size();
    }
    
    public int countByTrack(String track) {
        return (int) members.stream()
                .filter(s -> track.equalsIgnoreCase(s.getTrack()))
                .count();
    }
    
    public int countByWorkingStatus(String status) {
        return (int) members.stream()
                .filter(s -> status.equalsIgnoreCase(s.getWorkingStatus()))
                .count();
    }
    
    public int countAdvancedCourseParticipants() {
        return (int) members.stream()
                .filter(s -> s.getCourseType() != null && 
                        (s.getCourseType().toLowerCase().contains("advanced") || 
                         s.getCourseType().toLowerCase().contains("full")))
                .count();
    }
    
    public List<String> getMemberNames() {
        return members.stream()
                .map(Student::getName)
                .collect(Collectors.toList());
    }
    
    public String getMemberNamesAsString() {
        return String.join(", ", getMemberNames());
    }
    
    public String getTrackDistribution() {
        int sdetCount = countByTrack("SDET");
        int daCount = countByTrack("DA");
        return String.format("SDET: %d, DA: %d", sdetCount, daCount);
    }
}