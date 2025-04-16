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
    private EventType eventType;
    private List<Team> advancedCourseTeams;
    private List<Team> fullCourseTeams;
    private int advancedCourseStudentsCount;
    private int fullCourseStudentsCount;
    
    public int getAdvancedCourseTeamsCount() {
        return advancedCourseTeams != null ? advancedCourseTeams.size() : 0;
    }
    
    public int getFullCourseTeamsCount() {
        return fullCourseTeams != null ? fullCourseTeams.size() : 0;
    }
    
    public int getTotalStudentsCount() {
        return advancedCourseStudentsCount + fullCourseStudentsCount;
    }
    
    public int getTotalTeamsCount() {
        return getAdvancedCourseTeamsCount() + getFullCourseTeamsCount();
    }
}
