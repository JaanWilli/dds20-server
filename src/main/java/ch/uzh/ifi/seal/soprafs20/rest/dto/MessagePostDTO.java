package ch.uzh.ifi.seal.soprafs20.rest.dto;

public class MessagePostDTO {

    private String message;

    private int transaction;

    private String sender;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTransaction() { return transaction; }

    public void setTransaction(int transaction) { this.transaction = transaction; }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
