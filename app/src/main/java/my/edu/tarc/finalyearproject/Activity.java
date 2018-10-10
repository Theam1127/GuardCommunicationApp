package my.edu.tarc.finalyearproject;

public class Activity {
    private int activityID;
    private String activityStatus;
    private String activityImage;
    private int cctvID;

    public Activity(int activityID, String activityStatus, String activityImage, int cctvID) {
        this.activityID = activityID;
        this.activityStatus = activityStatus;
        this.activityImage = activityImage;
        this.cctvID = cctvID;
    }

    @Override
    public String toString() {
        return "Activity ID: " + activityID +
                "\nStatus: " + activityStatus;
    }

    public int getActivityID() {
        return activityID;
    }

    public void setActivityID(int activityID) {
        this.activityID = activityID;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public String getActivityImage() {
        return activityImage;
    }

    public void setActivityImage(String activityImage) {
        this.activityImage = activityImage;
    }

    public int getCctvID() {
        return cctvID;
    }

    public void setCctvID(int cctvID) {
        this.cctvID = cctvID;
    }
}
