package dds20.rest.dto;

public class InquiryPostDTO {

    private int transId;
    private String sender;

    public int getTransId() { return transId; }

    public void setTransId(int transId) { this.transId = transId; }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
