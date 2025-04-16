package com.teamformation.model;

public enum EventType {
    SQL_BOOTCAMP("SQL Bootcamp"),
    PHASE1_GHERKIN_HACKATHON("Phase 1 Gherkin Hackathon"),
    PHASE2_SELENIUM_HACKATHON("Phase 2 Selenium Hackathon"),
    PHASE1_API_HACKATHON("Phase 1 API Hackathon"),
    PHASE2_API_HACKATHON("Phase 2 API Hackathon"),
    RECIPE_SCRAPING_HACKATHON("Recipe Scraping Hackathon");
    
    private final String displayName;
    
    EventType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}