package dds20.rest.dto;

public class MessagePostDTO {

    private String message;
    private Integer transId;
    private Integer senderId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTransId() { return transId; }

    public void setTransId(int transId) { this.transId = transId; }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }
}
