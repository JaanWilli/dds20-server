package dds20.rest.dto;

public class SettingsPostDTO {

    private Boolean active;
    private String dieAfter;
    private Boolean vote;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDieAfter() {
        return dieAfter;
    }

    public void setDieAfter(String dieAfter) {
        this.dieAfter = dieAfter;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }
}
