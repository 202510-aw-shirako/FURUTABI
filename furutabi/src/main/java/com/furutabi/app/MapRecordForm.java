package com.furutabi.app;

public class MapRecordForm {

    private String title;
    private String body;
    private String visibility;
    private String locationName;
    private String locationPrecisionLevel;
    private boolean draft;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationPrecisionLevel() {
        return locationPrecisionLevel;
    }

    public void setLocationPrecisionLevel(String locationPrecisionLevel) {
        this.locationPrecisionLevel = locationPrecisionLevel;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }
}
