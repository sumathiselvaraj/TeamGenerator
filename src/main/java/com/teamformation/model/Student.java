package com.teamformation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private String timestamp;
    private String email;
    private String name;
    private String track;
    private String batch;
    private String courseType;
    
    // Additional fields for hackathon events
    private String workingStatus;
    private String timeZone;
    private String dsAlgoCompletion;
    private String previousHackathon;
    private String previousHackathonParticipation; // Yes/No for previous hackathon participation
    
    // Additional fields for API hackathon events
    private String apiBootcampCompletion;
    
    // Additional fields for SQL hackathon
    private String sqlExpertiseLevel;
}
