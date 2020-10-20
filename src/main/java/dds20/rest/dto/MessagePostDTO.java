package dds20.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class MessagePostDTO {

    private String message;
    private String node;
    private Integer transId;
    private String coordinator;
    private List<String> subordinates;
    private Boolean isStatus;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public int getTransId() { return transId; }

    public void setTransId(int transId) { this.transId = transId; }

    public String getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    public List<String> getSubordinates() {
        return subordinates;
        
    }

    public void setSubordinates(List<String> subordinates) {
        this.subordinates = subordinates;
    }

    public Boolean getIsStatus() {
        return isStatus;
    }

    public void setIsStatus(Boolean isStatus) {
        this.isStatus = isStatus;
    }

}
