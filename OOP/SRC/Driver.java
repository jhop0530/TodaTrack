import java.io.Serializable;

public class Driver implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String contactNum;
    private boolean isAvailable;

    public Driver(String name, String contactNum) {
        this.name = name;
        this.contactNum = contactNum;
        this.isAvailable = false;
    }

    public void setAvailability(boolean available) {
        this.isAvailable = available;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getContactInfo() {
        return "Name: " + name + ", Contact: " + contactNum;
    }

    public String getName() {
        return name;
    }

    public String getContactNum() {
        return contactNum;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContactNum(String contactNum) {
        this.contactNum = contactNum;
    }

    @Override
    public String toString() {
        return "Driver: " + name + " (Contact: " + contactNum + ")";
    }
}