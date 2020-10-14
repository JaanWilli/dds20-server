package dds20.rest.dto;

import java.util.ArrayList;

public class DataGetDTO {

    private Long id;
    private String message;
    private String node;
    private Integer transId;
    private Integer coordId;
    private Integer senderId;
    private ArrayList<Integer> subordinates;
    private Boolean isStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getTransId() {
        return transId;
    }

    public void setTransId(Integer transId) {
        this.transId = transId;
    }

    public Integer getCoordId() {
        return coordId;
    }

    public void setCoordId(Integer coordId) {
        this.coordId = coordId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public ArrayList<Integer> getSubordinates() {
        return subordinates;
    }

    public void setSubordinates(ArrayList<Integer> subordinates) {
        this.subordinates = subordinates;
    }

    public Boolean getIsStatus() {
        return isStatus;
    }

    public void setIsStatus(Boolean isStatus) {
        this.isStatus = isStatus;
    }
}
