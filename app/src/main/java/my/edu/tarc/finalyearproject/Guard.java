package my.edu.tarc.finalyearproject;

public class Guard {
    String guardID;
    String guardName;
    String phoneNumber;

    public Guard(String guardID, String guardName, String phoneNumber) {
        this.guardID=guardID;
        this.guardName = guardName;
        this.phoneNumber = phoneNumber;
    }

    public String getGuardID() {
        return guardID;
    }

    public void setGuardID(String guardID) {
        this.guardID = guardID;
    }

    public String getGuardName() {
        return guardName;
    }

    public void setGuardName(String guardName) {
        this.guardName = guardName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
