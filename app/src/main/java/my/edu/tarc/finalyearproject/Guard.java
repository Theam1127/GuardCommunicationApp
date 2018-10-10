package my.edu.tarc.finalyearproject;

public class Guard {
    String guardName;
    String phoneNumber;

    public Guard(String guardName, String phoneNumber) {
        this.guardName = guardName;
        this.phoneNumber = phoneNumber;
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
