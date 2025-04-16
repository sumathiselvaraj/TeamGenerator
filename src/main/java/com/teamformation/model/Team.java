package com.teamformation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
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
    
    public int getDaCount() {
        return countByTrack("DA");
    }
    
    public int getSdetCount() {
        return countByTrack("SDET");
    }
    
    public int getDvlprCount() {
        return countByTrack("DVLPR");
    }
    
    public int countByWorkingStatus(String status) {
        return (int) members.stream()
                .filter(s -> status.equalsIgnoreCase(s.getWorkingStatus()))
                .count();
    }
    
    public int countByPreviousHackathon(String status) {
        return (int) members.stream()
                .filter(s -> status.equalsIgnoreCase(s.getPreviousHackathonParticipation()))
                .count();
    }
    
    /**
     * Checks if the team already has a student from the specified batch number with the same track
     * @param batchNumber Batch number to check 
     * @param track The track to check for this batch
     * @return true if any student in the team has this batch number and track
     */
    public boolean hasBatchNumberWithTrack(String batchNumber, String track) {
        if (batchNumber == null || batchNumber.trim().isEmpty() || track == null) {
            return false;
        }
        
        return members.stream()
                .filter(s -> s.getBatch() != null && s.getTrack() != null)
                .anyMatch(s -> s.getBatch().equalsIgnoreCase(batchNumber.trim()) && 
                         s.getTrack().equalsIgnoreCase(track.trim()));
    }
    
    /**
     * Checks if the team already has a student from the specified batch number
     * @param batchNumber Batch number to check 
     * @return true if any student in the team has this batch number
     */
    public boolean hasBatchNumber(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return false;
        }
        
        return members.stream()
                .filter(s -> s.getBatch() != null)
                .anyMatch(s -> s.getBatch().equalsIgnoreCase(batchNumber.trim()));
    }
    
    public int countAdvancedCourseParticipants() {
        return (int) members.stream()
                .filter(s -> s.getCourseType() != null && 
                        s.getCourseType().toLowerCase().contains("advanced"))
                .count();
    }
    
    public int countFullCourseParticipants() {
        return (int) members.stream()
                .filter(s -> s.getCourseType() != null && 
                        s.getCourseType().toLowerCase().contains("full"))
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
        int sdetCount = getSdetCount();
        int daCount = getDaCount();
        int dvlprCount = getDvlprCount();
        return String.format("SDET: %d, DA: %d, DVLPR: %d", sdetCount, daCount, dvlprCount);
    }
    
    // Get members sorted by track and name for better display
    public List<Student> getSortedStudents() {
        return members.stream()
                .sorted(Comparator.comparing(Student::getTrack)
                        .thenComparing(Student::getName))
                .collect(Collectors.toList());
    }
}