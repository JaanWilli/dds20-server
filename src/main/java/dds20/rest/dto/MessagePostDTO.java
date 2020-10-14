package dds20.rest.dto;

import java.util.ArrayList;

public class MessagePostDTO {

    private String message;
    private String node;
    private int transId;
    private String coordinator;
    private ArrayList<String> subordinates;

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

    public ArrayList<String> getSubordinates() {
        return subordinates;
    }

    public void setSubordinates(ArrayList<String> subordinates) {
        this.subordinates = subordinates;
    }
}
