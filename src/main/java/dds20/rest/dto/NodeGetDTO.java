package dds20.rest.dto;

import java.util.ArrayList;

public class NodeGetDTO {

    private Long id;
    private Boolean active;
    private String dieAfter;
    private Boolean vote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
