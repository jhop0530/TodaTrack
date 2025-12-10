import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Trip implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int tripCounter = 1;
    private int tripId;
    private Tricycle tricycle;
    private int passengerCount;
    private String fromLocation;
    private String destination;
    private double totalFare;
    private boolean active;
    private Date departedTime;
    private Date arrivedTime;
    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a");

    public Trip(Tricycle tricycle, int passengerCount, String from, double fare, String destination) {
        this.tripId = tripCounter++;
        this.tricycle = tricycle;
        this.passengerCount = passengerCount;
        this.fromLocation = from;
        this.totalFare = fare;
        this.destination = destination;
        this.active = false;
        this.departedTime = null;
        this.arrivedTime = null;
    }

    public void startTrip() {
        this.active = true;
        this.tricycle.setStatus("On Trip");
        this.departedTime = new Date();
    }

    public void endTrip() {
        this.active = false;
        this.tricycle.setStatus("Waiting");
        this.arrivedTime = new Date();
    }

    public boolean isActive() {
        return this.active;
    }

    public Tricycle getTricycle() {
        return tricycle;
    }

    public double getTotalFare() {
        return totalFare;
    }

    /**
     * Ensures new trip IDs don't collide with loaded ones
     * * @param count The highest trip ID found during data load
     */
    public static void setTripCounter(int count) {
        tripCounter = count;
    }

    public int getTripId() {
        return tripId;
    }

    @Override
    public String toString() {
        String timeString = "";
        if (departedTime != null) {
            timeString += "<br>Departed: " + timeFormatter.format(departedTime);
        }

        if (arrivedTime != null) {
            timeString += "<br>Arrived: " + timeFormatter.format(arrivedTime);
        }

        return String.format("<html><b>%s (Trip #%d)</b><br>From: %s<br>To: %s<br>Fare: ₱%.2f%s</html>",
                tricycle.getDriver().getName(),
                tripId,
                fromLocation,
                destination,
                this.totalFare,
                timeString);
    }
}