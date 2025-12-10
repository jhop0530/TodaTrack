import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Iterator;

/**
 * The core logic "controller" for the TodaTrack system.
 * This class is Serializable, allowing the entire system state to be
 * saved/loaded.
 * It manages the lists of tricycles, trips, and the driver queue.
 */
public class TodaTrackSystem implements Serializable {
    private static final long serialVersionUID = 1L;

    // Main data lists
    private List<Tricycle> tricycles; // Master list of all registered tricycles
    private List<Trip> trips; // List of trips for the current day (active and completed)
    private List<Trip> tripArchive; // List of all trips from previous days
    private Queue<Tricycle> waitingQueue; // The queue of "On Duty" drivers

    private String broadcastMessage;

    public TodaTrackSystem() {
        this.tricycles = new ArrayList<>();
        this.trips = new ArrayList<>();
        this.tripArchive = new ArrayList<>();
        this.waitingQueue = new LinkedList<>();
        this.broadcastMessage = "No announcements at this time.";
    }

    /**
     * Adds a new tricycle (and its driver) to the master list.
     * * @param tricycle The Tricycle object to register.
     */
    public void registerTricycle(Tricycle tricycle) {
        this.tricycles.add(tricycle);
    }

    /**
     * Removes a tricycle from the master list and the waiting queue.
     * * @param tricycle The Tricycle object to remove.
     */
    public void removeTricycle(Tricycle tricycle) {
        if (tricycle == null)
            return;

        tricycles.remove(tricycle);
        waitingQueue.remove(tricycle);
    }

    /**
     * Starts a new trip for a *specific* tricycle.
     * This method is MODIFIED to accept a specific driver, not just the front of
     * the queue.
     *
     * @param tricycle       The *specific* tricycle to start the trip for.
     * @param passengerCount Number of passengers.
     * @param from           Location trip started from.
     * @param fare           Total fare for the trip.
     * @param destination    The trip's destination.
     */
    public void startTrip(Tricycle tricycle, int passengerCount, String from, double fare, String destination) {
        // Remove the specific tricycle from the queue
        if (!waitingQueue.remove(tricycle)) {
            System.err.println(
                    "Warning: " + tricycle.getPlateNumber() + " was not in the waiting queue but is starting a trip.");
        }

        if (tricycle == null) {
            System.err.println("Cannot start trip with a null tricycle.");
            return;
        }

        // Create and start the new trip
        Trip newTrip = new Trip(tricycle, passengerCount, from, fare, destination);
        this.trips.add(newTrip); // Add to the "today's trips" list
        tricycle.setCurrentTrip(newTrip);
        newTrip.startTrip(); // This sets the trip and tricycle status
    }

    /**
     * Marks an active trip as completed.
     * * @param trip The Trip to complete.
     */
    public void endTrip(Trip trip) {
        if (trip.isActive()) {
            trip.endTrip(); // Sets status and arrival time
            trip.getTricycle().setCurrentTrip(null);
        }
    }

    /**
     * Archives all completed trips from the current day and generates a report.
     * * @return A String summary of the report.
     */
    public String endOfDayReportAndClear() {
        double totalFares = 0.0;
        int totalTrips = 0;

        // Iterate and move completed trips to the archive
        Iterator<Trip> iterator = trips.iterator();
        while (iterator.hasNext()) {
            Trip trip = iterator.next();
            if (!trip.isActive()) {
                totalTrips++;
                totalFares += trip.getTotalFare();
                tripArchive.add(trip); // Add to archive
                iterator.remove(); // Remove from "today's trips"
            }
        }

        // Check if any active trips are left (e.g., started at 11:59 PM)
        boolean activeTripsRemain = false;
        for (Trip trip : trips) {
            if (trip.isActive()) {
                activeTripsRemain = true;
                break;
            }
        }

        // Reset the static trip counter *only* if no trips are active
        String reportMessage;
        if (!activeTripsRemain) {
            Trip.setTripCounter(1); // Reset for the new day
            reportMessage = "\nAll completed trips have been archived.\nTrip counter has been reset to 1 for the new day.";
        } else {
            reportMessage = "\nAll completed trips have been archived.\nActive trips remain. Trip counter will not be reset to avoid ID conflicts.";
        }

        return String.format("--- End of Day Report ---\n\nTotal Completed Trips: %d\nTotal Fares Earned: ₱%.2f\n%s",
                totalTrips, totalFares, reportMessage);
    }

    // --- Getters and Setters ---

    public List<Tricycle> getAllTricycles() {
        return tricycles;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public Queue<Tricycle> getWaitingQueue() {
        return waitingQueue;
    }

    /**
     * Provides public, read-only access to the trip archive.
     * * @return The list of archived trips.
     */
    public List<Trip> getTripArchive() {
        return tripArchive;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            this.broadcastMessage = "No announcements at this time.";
        } else {
            this.broadcastMessage = message;
        }
    }
}