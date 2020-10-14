package dds20.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Internal Data Representation
 * This class composes the internal representation of the data and defines how the data is stored in the database.
 * Every variable will be mapped into a database field with the @Column annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unique across the database -> composes the primary key
 */
@Entity
@Table(name = "DATA")
public class Data implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String node;

    @Column(nullable = false)
    private Integer transId;

    @Column(nullable = false)
    private Integer coordId;

    @Column(nullable = false)
    private ArrayList<Integer> subordinates;

    @Column(nullable = false)
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

    public ArrayList<Integer> getSubordinates() {
        return subordinates;
    }

    public void setSubordinates(ArrayList<Integer> subordinates) {
        this.subordinates = subordinates;
    }

    public Boolean getIsStatus() { return isStatus; }

    public void setIsStatus(Boolean isStatus) { this.isStatus = isStatus; }
}
