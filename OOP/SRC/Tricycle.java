import java.io.Serializable;

public class Tricycle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String plateNumber;
    private Driver driver;
    private String status;
    private double fare;
    private String defaultRoute;
    private Trip currentTrip;

    public Tricycle(String plateNumber, String defaultRoute, double fare) {
        this.plateNumber = plateNumber;
        this.defaultRoute = defaultRoute;
        this.fare = fare;
        this.status = "Unavailable";
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public double getFare() {
        return fare;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setCurrentTrip(Trip currentTrip) {
        this.currentTrip = currentTrip;
    }

    public Trip getCurrentTrip() {
        return currentTrip;
    }

    @Override
    public String toString() {
        return "Tricycle: " + plateNumber + " (Driver: " + driver.getName() + ")";
    }
}