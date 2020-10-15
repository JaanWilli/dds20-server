package dds20.rest.dto;

import java.util.ArrayList;

public class SetupPostDTO {

    private Boolean isCoordinator;
    private Boolean isSubordinate;
    private String coordinator;
    private ArrayList<String> subordinates;

    public Boolean getIsCoordinator() {
        return isCoordinator;
    }

    public void setIsCoordinator(Boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }

    public Boolean getIsSubordinate() {
        return isSubordinate;
    }

    public void setIsSubordinate(Boolean isSubordinate) {
        this.isSubordinate = isSubordinate;
    }

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
