package dds20.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Internal Node Representation
 * This class composes the internal representation of the node and defines how the node is stored in the database.
 * Every variable will be mapped into a database field with the @Column annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unique across the database -> composes the primary key
 */
@Entity
@Table(name = "NODE")
public class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Boolean isCoordinator;

    @Column(nullable = false)
    private Boolean isSubordinate;

    @Column(nullable = false)
    private ArrayList<String> subordinates;

    @Column
    private Boolean active;

    @Column
    private String dieAfter;

    @Column(nullable = false)
    private String node;

    @Column(nullable = false)
    private String coordinator;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getIsCoordinator() { return isCoordinator; }

    public void setIsCoordinator(Boolean isCoordinator) { this.isCoordinator = isCoordinator; }

    public Boolean getIsSubordinate() { return isSubordinate; }

    public void setIsSubordinate(Boolean isSubordinate) { this.isSubordinate = isSubordinate; }

    public String getCoordinator() { return coordinator; }

    public void setCoordinator(String coordinator) { this.coordinator = coordinator; }

    public ArrayList<String> getSubordinates() {
        return subordinates;
    }

    public void setSubordinates(ArrayList<String> subordinates) {
        this.subordinates = subordinates;
    }

    public Boolean getActive() { return active; }

    public void setActive(Boolean active) { this.active = active; }

    public String getDieAfter() { return dieAfter; }

    public void setDieAfter(String dieAfter) { this.dieAfter = dieAfter; }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }
}
