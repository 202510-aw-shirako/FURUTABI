package com.furutabi.app;

public class MyMapRecordRequest {

    private String title;
    private String body;
    private String visibility;
    private String locationName;
    private String locationPrecisionLevel;
    private Double x;
    private Double y;
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

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }
}
