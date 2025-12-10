import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A dedicated JDialog for editing an existing driver.
 * Replaces the clunky JOptionPane.
 */
public class EditDriverDialog extends JDialog {

    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);

    // Form fields
    private JTextField nameField;
    private JTextField contactField;
    private JTextField plateField;

    private boolean isSucceeded = false;

    public EditDriverDialog(Frame parent, Tricycle tricycleToEdit) {
        super(parent, "Edit Driver Details", true);

        Driver driver = tricycleToEdit.getDriver();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Driver Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        panel.add(createDialogLabel("Driver Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        nameField = new JTextField(driver.getName(), 20); // Pre-fill
        nameField.setFont(FONT_LABEL);
        panel.add(nameField, gbc);

        // Contact Number
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(createDialogLabel("Contact Number:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        contactField = new JTextField(driver.getContactNum(), 20); // Pre-fill
        contactField.setFont(FONT_LABEL);
        panel.add(contactField, gbc);

        // Plate Number
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        panel.add(createDialogLabel("Plate Number:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        plateField = new JTextField(tricycleToEdit.getPlateNumber(), 20); // Pre-fill
        plateField.setFont(FONT_LABEL);
        panel.add(plateField, gbc);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Save Changes");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(buttonPanel, gbc);

        this.setContentPane(panel);
        this.pack();
        this.setLocationRelativeTo(parent);
    }

    private void onOK() {
        if (getDriverName().trim().isEmpty() || getContactNum().trim().isEmpty() || getPlateNum().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.isSucceeded = true;
        this.dispose();
    }

    private void onCancel() {
        this.isSucceeded = false;
        this.dispose();
    }

    // --- Public Getters ---

    public boolean isSucceeded() {
        return isSucceeded;
    }

    public String getDriverName() {
        return nameField.getText();
    }

    public String getContactNum() {
        return contactField.getText();
    }

    public String getPlateNum() {
        return plateField.getText();
    }

    // Helper method
    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        return label;
    }
}