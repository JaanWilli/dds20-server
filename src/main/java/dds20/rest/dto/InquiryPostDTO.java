package dds20.rest.dto;

public class InquiryPostDTO {

    private int transId;
    private String senderId;

    public int getTransId() { return transId; }

    public void setTransId(int transId) { this.transId = transId; }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String sender) {
        this.senderId = senderId;
    }
}
