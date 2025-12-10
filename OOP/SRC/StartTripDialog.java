import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
// The imports below can be removed (as your IDE warning showed)
// but are harmless to leave in.
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A dedicated JDialog for starting a new trip.
 * Replaces the clunky JOptionPane.
 */
public class StartTripDialog extends JDialog {

    // Fonts from TodaTrackApp (simplified for this example)
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);

    // Form fields
    private JTextField fromField;
    private JTextField passengersField;
    private JTextField fareField;
    private JTextField destinationField;

    // Data to be returned
    private boolean isSucceeded = false;
    private String fromLocation;
    private int passengerCount;
    private double farePerPassenger;
    private String destination;

    /**
     * Creates the dialog.
     *
     * @param parent    The parent frame (the main app).
     * @param tricycle  The tricycle starting the trip.
     */
    public StartTripDialog(Frame parent, Tricycle tricycle) {
        super(parent, "Start Trip for " + tricycle.getDriver().getName(), true); // 'true' for modal

        JPanel myPanel = new JPanel(new BorderLayout(10, 10));
        myPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Use a more flexible GridBagLayout for the form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. From
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createDialogLabel("1. From:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        fromField = new JTextField("TSU San Isidro", 20);
        fromField.setFont(FONT_LABEL);
        formPanel.add(fromField, gbc);

        // 2. Passenger Count
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createDialogLabel("2. Passenger Count:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passengersField = new JTextField("1", 20);
        passengersField.setFont(FONT_LABEL);
        formPanel.add(passengersField, gbc);

        // 3. Fare per Passenger
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createDialogLabel("3. Fare per Passenger (₱):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        fareField = new JTextField("20.00", 20);
        fareField.setFont(FONT_LABEL);
        formPanel.add(fareField, gbc);

        // 4. Destination
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(createDialogLabel("4. Destination:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        destinationField = new JTextField(20);
        destinationField.setFont(FONT_LABEL);
        formPanel.add(destinationField, gbc);

        myPanel.add(formPanel, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Start Trip");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        myPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Final setup
        this.setContentPane(myPanel);
        this.pack();
        this.setLocationRelativeTo(parent);
    }

    /**
     * Handles the OK button click. Validates input and saves data.
     */
    private void onOK() {
        try {
            // Parse and validate
            this.fromLocation = fromField.getText();
            this.passengerCount = Integer.parseInt(passengersField.getText());
            this.farePerPassenger = Double.parseDouble(fareField.getText());
            this.destination = destinationField.getText();

            if (destination.trim().isEmpty() || fromLocation.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "From and Destination cannot be empty.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (passengerCount <= 0) {
                 JOptionPane.showMessageDialog(this, "Passenger count must be at least 1.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // If all is valid
            this.isSucceeded = true;
            this.dispose(); // Close the dialog

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for Passenger Count and Fare.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the Cancel button click.
     */
    private void onCancel() {
        this.isSucceeded = false;
        this.dispose();
    }

    // --- Public Getters ---

    public boolean isSucceeded() {
        return isSucceeded;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public double getFarePerPassenger() {
        return farePerPassenger;
    }

    public String getDestination() {
        return destination;
    }

    // Helper method to create a standardized label
    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        return label;
    }
}