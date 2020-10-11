package dds20.entity;

import javax.persistence.*;
import java.io.Serializable;

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
    private Boolean active;

    @Column(nullable = false)
    private String dieAfter;

    public Boolean getActive() { return active; }

    public void setActive(Boolean active) { this.active = active; }

    public String getDieAfter() { return dieAfter; }

    public void setDieAfter(String dieAfter) { this.dieAfter = dieAfter; }
}
