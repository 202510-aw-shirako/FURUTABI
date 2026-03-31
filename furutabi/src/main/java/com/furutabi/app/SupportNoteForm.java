package com.furutabi.app;

public class SupportNoteForm {

    private String noteStatus;
    private Long relatedCardId;
    private String body;
    private String carePoints;

    public String getNoteStatus() {
        return noteStatus;
    }

    public void setNoteStatus(String noteStatus) {
        this.noteStatus = noteStatus;
    }

    public Long getRelatedCardId() {
        return relatedCardId;
    }

    public void setRelatedCardId(Long relatedCardId) {
        this.relatedCardId = relatedCardId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCarePoints() {
        return carePoints;
    }

    public void setCarePoints(String carePoints) {
        this.carePoints = carePoints;
    }
}
