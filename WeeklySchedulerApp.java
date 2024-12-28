import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Main class for the weekly scheduler application
public class WeeklySchedulerApp extends JFrame {
    private LocalDate currentMonday;
    private List<Event> events = new ArrayList<>();
    private JButton[][] timeSlots; // To keep track of time slots for each day
    

    // Constructor
    public WeeklySchedulerApp(LocalDate initialMonday) {
        this.currentMonday = initialMonday;

        setTitle("CEO Weekly Scheduler");
        setSize(1300, 700);
        setLayout(new BorderLayout());

        // Panel for displaying the days of the week (Monday to Sunday)
        JPanel daysPanel = new JPanel(new GridLayout(1, 8)); // 7 days + time column
        JPanel hoursPanel = new JPanel(new GridLayout(13, 8)); // 13 time slots (8AM to 8PM) + time column

        // Create day labels (Monday to Sunday)
        daysPanel.add(new JLabel("Time", SwingConstants.CENTER)); // Time header
        for (int i = 0; i < 7; i++) {
            LocalDate day = currentMonday.plusDays(i);
            JLabel dayLabel = new JLabel(day.getDayOfWeek().toString() + " (" + day + ")", SwingConstants.CENTER);
            daysPanel.add(dayLabel);
        }

        // Initialize the time slots button array
        timeSlots = new JButton[13][7];

        // Create hourly grid for each day
        for (int hour = 8; hour <= 20; hour++) {
             // Display time in the first column with proper AM/PM designation
             String timeLabel;
             if (hour == 12) {
                 timeLabel = "12.00 PM"; // Handle noon correctly
             } else if (hour > 12) {
                 timeLabel = (hour - 12) + ".00 PM"; // Convert to 12-hour format for PM
             } else {
                 timeLabel = hour + ".00 AM"; // For AM times
             }
 
             JLabel label = new JLabel(timeLabel, SwingConstants.CENTER);
             hoursPanel.add(label); // Add time label

            for (int day = 0; day < 7; day++) {
                JButton timeSlot = new JButton(); // Empty button for the event name
                timeSlot.setBackground(Color.WHITE);
                LocalDate date = currentMonday.plusDays(day);
                LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(hour, 0));

                // Open event dialog when a time slot is clicked
                timeSlot.addActionListener(e -> handleTimeSlotClick(dateTime));
                timeSlots[hour - 8][day] = timeSlot; // Store the button in the array
                hoursPanel.add(timeSlot); // Add button to the panel
            }
        }

        // Add panels to the frame
        add(daysPanel, BorderLayout.NORTH);
        add(hoursPanel, BorderLayout.CENTER);

        // Menu for creating, saving, and loading events
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem addEventMenu = new JMenuItem("Create Event");
        addEventMenu.addActionListener(e -> openCreateEventForm(null)); // Option to create a new event
        JMenuItem saveScheduleMenu = new JMenuItem("Save Schedule");
        saveScheduleMenu.addActionListener(e -> saveScheduleToFile());
        JMenuItem loadScheduleMenu = new JMenuItem("Load Schedule");
        loadScheduleMenu.addActionListener(e -> loadScheduleFromFile());

        menu.add(addEventMenu);
        menu.add(saveScheduleMenu);
        menu.add(loadScheduleMenu);
        menuBar.add(menu);

        setJMenuBar(menuBar);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    // New method to prompt for the week start date
    private static LocalDate promptForWeekStartDate() {
        String input = JOptionPane.showInputDialog(null, "Enter the start date of the week (YYYY-MM-DD):");
        if (input != null && !input.trim().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(input);
                if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
                    return date;
                } else {
                    // If not Monday, find the previous Monday
                    return date.with(DayOfWeek.MONDAY);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Invalid date format. Using the current week instead.");
            }
        }
        return LocalDate.now().with(DayOfWeek.MONDAY); // Default to the current week's Monday
    }


    private void confirmAndDeleteEvent(Event eventToDelete) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the event: " + eventToDelete.name + "?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
    
        if (confirm == JOptionPane.YES_OPTION) {
            // Collect secretary input
            String secretaryInput = JOptionPane.showInputDialog(this, "Please enter your name (Secretary):");
            if (secretaryInput != null && !secretaryInput.trim().isEmpty()) {
                // Remove the event from the list
                events.remove(eventToDelete);
                updateScheduleDisplay(); // Update the UI to reflect the deletion
                JOptionPane.showMessageDialog(this, "Event deleted successfully by " + secretaryInput + "!");
            } else {
                JOptionPane.showMessageDialog(this, "Deletion canceled: Secretary name is required.");
            }
        }
    }
    
    
    // Handle time slot click for event creation or editing
    private void handleTimeSlotClick(LocalDateTime dateTime) {
        // Check if the selected date is a Sunday
        if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            JOptionPane.showMessageDialog(this, "Cannot schedule events on Sunday!", "Error", JOptionPane.ERROR_MESSAGE);
            return; // Exit the method to prevent further processing
        }
    
        // Check if the selected date is Saturday and the time is within the allowed range (8 AM to 3 PM)
        if (dateTime.getDayOfWeek() == DayOfWeek.SATURDAY) {
            int hour = dateTime.getHour();
            if (hour < 8 || hour >= 15) {
                JOptionPane.showMessageDialog(this, "Events can only be scheduled on Saturday between 8 AM and 3 PM!", "Error", JOptionPane.ERROR_MESSAGE);
                return; // Exit the method to prevent further processing
            }
        }
    
        // Find the event at the clicked time
        Event eventToEdit = null;
        for (Event event : events) {
            if (event.startTime.equals(dateTime)) {
                eventToEdit = event;
                break;
            }
        }
    
        if (eventToEdit != null) {
            // Display event details and options (Edit and Delete)
            int option = JOptionPane.showOptionDialog(this, "Event: " + eventToEdit.name + "\nLocation: " + eventToEdit.location, 
                    "Event Details", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, 
                    new String[]{"Edit", "Delete", "Cancel"}, null);
    
            if (option == 0) {
                openEditEventForm(eventToEdit); // Edit option
            } else if (option == 1) {
                confirmAndDeleteEvent(eventToEdit); // Delete option
            }
        } else {
            // Handle case where no event exists at that time
            JOptionPane.showMessageDialog(this, "No event scheduled at this time.");
        }
    }
    


    // Method to open the event creation form
    private void openCreateEventForm(LocalDateTime dateTime) {
        JFrame createEventFrame = new JFrame("Create Event");
        createEventFrame.setSize(400, 500);
        createEventFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10); // Padding around components

        // Date spinner for event date
        JLabel dateLabel = new JLabel("Event Date:");
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(java.sql.Date.valueOf(dateTime != null ? dateTime.toLocalDate() : LocalDate.now())); // Set current date

        gbc.gridx = 0;
        gbc.gridy = 0;
        createEventFrame.add(dateLabel, gbc);

        gbc.gridx = 1;
        createEventFrame.add(dateSpinner, gbc);

        // Time spinner for start time
        JLabel startTimeLabel = new JLabel("Start Time:");
        JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "hh:mm a");
        startTimeSpinner.setEditor(startTimeEditor);
        startTimeSpinner.setValue(java.sql.Time.valueOf(dateTime != null ? dateTime.toLocalTime() : LocalTime.now().withMinute(0).withSecond(0))); // Set current time

        gbc.gridx = 0;
        gbc.gridy = 1;
        createEventFrame.add(startTimeLabel, gbc);

        gbc.gridx = 1;
        createEventFrame.add(startTimeSpinner, gbc);

        // Time spinner for end time
        JLabel endTimeLabel = new JLabel("End Time:");
        JSpinner endTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "hh:mm a");
        endTimeSpinner.setEditor(endTimeEditor);
        endTimeSpinner.setValue(java.sql.Time.valueOf(dateTime != null ? dateTime.plusHours(1).toLocalTime() : LocalTime.now().plusHours(1).withMinute(0).withSecond(0))); // Set end time

        gbc.gridx = 0;
        gbc.gridy = 2;
        createEventFrame.add(endTimeLabel, gbc);

        gbc.gridx = 1;
        createEventFrame.add(endTimeSpinner, gbc);

        // Event name
        JLabel eventNameLabel = new JLabel("Event Name:");
        JTextField eventNameField = new JTextField(20);

        gbc.gridx = 0;
        gbc.gridy = 3;
        createEventFrame.add(eventNameLabel, gbc);

        gbc.gridx = 1;
        createEventFrame.add(eventNameField, gbc);

        // Location
        JLabel locationLabel = new JLabel("Location:");
        JTextField locationField = new JTextField(20);

        gbc.gridx = 0;
        gbc.gridy = 4;
        createEventFrame.add(locationLabel, gbc);

        gbc.gridx = 1;
        createEventFrame.add(locationField, gbc);

        // Color selection
        JLabel colorLabel = new JLabel("Select Color:");
        String[] colors = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};
        JComboBox<String> colorComboBox = new JComboBox<>(colors);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        createEventFrame.add(colorLabel, gbc);
        
        gbc.gridx = 1;
        createEventFrame.add(colorComboBox, gbc);

        // Create button
        JButton createButton = new JButton("Create Event");
        createButton.addActionListener(e -> {
            // Logic to create event goes here
            if (createEvent(dateSpinner, startTimeSpinner, endTimeSpinner, eventNameField, locationField, colorComboBox)) {
                JOptionPane.showMessageDialog(createEventFrame, "Event created successfully!");
                createEventFrame.dispose();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2; // Span across both columns
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        
        createEventFrame.add(createButton, gbc);
        createEventFrame.setVisible(true);
        createEventFrame.setLocationRelativeTo(null);
    }

    // Open edit event form
    private void openEditEventForm(Event event) {
        JFrame editEventFrame = new JFrame("Edit Event");
        editEventFrame.setSize(400, 300);
        editEventFrame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10); // Padding around components
    
        // Event name
        JLabel eventNameLabel = new JLabel("Event Name:");
        JTextField eventNameField = new JTextField(event.name, 20);
    
        gbc.gridx = 0;
        gbc.gridy = 0;
        editEventFrame.add(eventNameLabel, gbc);
    
        gbc.gridx = 1;
        editEventFrame.add(eventNameField, gbc);
    
        // Location
        JLabel locationLabel = new JLabel("Location:");
        JTextField locationField = new JTextField(event.location, 20);
    
        gbc.gridx = 0;
        gbc.gridy = 1;
        editEventFrame.add(locationLabel, gbc);
    
        gbc.gridx = 1;
        editEventFrame.add(locationField, gbc);
    
        // Color selection
        JLabel colorLabel = new JLabel("Event Color:");
        String[] colors = {"Red", "Green", "Yellow", "Blue", "Orange", "Gray"};
        JComboBox<String> colorComboBox = new JComboBox<>(colors);
        colorComboBox.setSelectedItem(event.color); // Set the current color of the event
    
        gbc.gridx = 0;
        gbc.gridy = 2;
        editEventFrame.add(colorLabel, gbc);
    
        gbc.gridx = 1;
        editEventFrame.add(colorComboBox, gbc);
    
        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            event.name = eventNameField.getText();
            event.location = locationField.getText();
            event.color = (String) colorComboBox.getSelectedItem(); // Update event color
            updateEventDisplay(event); // Call the updated method to display the event
            JOptionPane.showMessageDialog(editEventFrame, "Event updated successfully!");
            editEventFrame.dispose();
        });
    
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2; // Span across both columns
        gbc.anchor = GridBagConstraints.CENTER; // Center the button
        editEventFrame.add(saveButton, gbc);
    
        editEventFrame.setVisible(true);
        editEventFrame.setLocationRelativeTo(null);
    }
    
    private Color getColor(String colorName) {
        switch (colorName) {
            case "Red":
                return Color.RED;
            case "Green":
                return Color.GREEN;
            case "Yellow":
                return Color.YELLOW;
            case "Blue":
                return Color.BLUE;
            case "Orange":
                return Color.ORANGE;
            case "Gray":
                return Color.GRAY;
            default:
                return Color.WHITE; // Default color if none match
        }
    }
    
    private void updateEventDisplay(Event event) {
        LocalDateTime startTime = event.startTime;
        int hourIndex = startTime.getHour() - 8; // Calculate the hour index (8 AM is 0)
        int dayIndex = startTime.getDayOfWeek().getValue() - 1; // Get the index for the day (Monday is 0)
    
        JButton timeSlotButton = timeSlots[hourIndex][dayIndex];
        timeSlotButton.setText(event.name); // Set button text to event name
        timeSlotButton.setForeground(Color.BLACK); // Set text color
        timeSlotButton.setBackground(getColor(event.color)); // Set background color based on the event
    }
    

    // Create a new event
    private boolean createEvent(JSpinner dateSpinner, JSpinner startTimeSpinner, JSpinner endTimeSpinner, 
                             JTextField eventNameField, JTextField locationField, JComboBox<String> colorComboBox) {
    LocalDate eventDate = ((java.util.Date) dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    LocalTime startTime = ((java.util.Date) startTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
    LocalTime endTime = ((java.util.Date) endTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

    LocalTime eightAM = LocalTime.of(8, 0);
    LocalTime threePM = LocalTime.of(15, 0);
    LocalTime eightPM = LocalTime.of(20, 0);

    // Check if the event is being scheduled on Sunday
    if (eventDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
        JOptionPane.showMessageDialog(this, "Cannot schedule events on Sunday!", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // Check if the event is being scheduled on Saturday
    if (eventDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
        // Check if startTime and endTime are within the allowed range (8 AM to 3 PM)
        if (startTime.isBefore(eightAM) || endTime.isAfter(threePM)) {
            JOptionPane.showMessageDialog(this, "Events on Saturday can only be scheduled between 8 AM and 3 PM.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    } else {
        // For weekdays (Monday to Friday), check if startTime and endTime are within the allowed range (8 AM to 8 PM)
        if (startTime.isBefore(eightAM) || endTime.isAfter(eightPM)) {
            JOptionPane.showMessageDialog(this, "Events on weekdays can only be scheduled between 8 AM and 8 PM.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Check if endTime is after startTime
    if (endTime.isBefore(startTime)) {
        JOptionPane.showMessageDialog(this, "End time must be after start time.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    LocalDateTime startDateTime = LocalDateTime.of(eventDate, startTime);
    LocalDateTime endDateTime = LocalDateTime.of(eventDate, endTime);

    // Check for overlapping events
    for (Event event : events) {
        if (event.startTime.toLocalDate().isEqual(eventDate) && event.overlaps(new Event("", "", startDateTime, endDateTime, ""))) {
            JOptionPane.showMessageDialog(this, "Event time overlaps with an existing event.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Create and add the event
    Event newEvent = new Event(eventNameField.getText(), locationField.getText(), startDateTime, endDateTime, colorComboBox.getSelectedItem().toString());
    events.add(newEvent);

    // Update the schedule view after creating the event
    updateEventDisplay(startDateTime, newEvent.name, newEvent.color);
    setLocationRelativeTo(null);
    return true;
}

    
    // Modify updateEventDisplay method to accept color
    private void updateEventDisplay(LocalDateTime startDateTime, String eventName, String color) {
        int hourIndex = startDateTime.getHour() - 8; // Adjust for the hour index (assuming 8 AM start)
        int dayIndex = startDateTime.getDayOfWeek().getValue() - 1; // Adjust for the day index (Monday=1)
    
        if (hourIndex >= 0 && hourIndex < 13 && dayIndex >= 0 && dayIndex < 7) {
            JButton timeSlot = timeSlots[hourIndex][dayIndex];
            timeSlot.setText(eventName); // Set the button text to the event name
            
            // Set button background color based on event color
            switch (color.toLowerCase()) {
                case "red":
                    timeSlot.setBackground(Color.RED);
                    break;
                case "green":
                    timeSlot.setBackground(Color.GREEN);
                    break;
                case "yellow":
                    timeSlot.setBackground(Color.YELLOW);
                    break;
                case "blue":
                    timeSlot.setBackground(Color.BLUE);
                    break;
                case "orange":
                    timeSlot.setBackground(Color.ORANGE);
                    break;
                case "gray":
                    timeSlot.setBackground(Color.GRAY);
                    break;
                default:
                    timeSlot.setBackground(Color.LIGHT_GRAY); // Default color if something goes wrong
                    break;
            }
        }
    }

    // Method to save schedule to a file
    private void saveScheduleToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("schedule.dat"))) {
            oos.writeObject(events);
            JOptionPane.showMessageDialog(this, "Schedule saved successfully!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving schedule: " + e.getMessage());
        }
    }

    // Method to load schedule from a file
    @SuppressWarnings("unchecked")
    private void loadScheduleFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("schedule.dat"))) {
            events = (List<Event>) ois.readObject();
            JOptionPane.showMessageDialog(this, "Schedule loaded successfully!");
            updateScheduleDisplay(); // Update the UI to reflect loaded events
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error loading schedule: " + e.getMessage());
        }
    }

    // Method to update the UI with loaded events
    private void updateScheduleDisplay() {
        // Reset all time slot buttons to default color
        for (int hour = 0; hour < timeSlots.length; hour++) {
            for (int day = 0; day < timeSlots[hour].length; day++) {
                timeSlots[hour][day].setText(""); // Clear any text
                timeSlots[hour][day].setBackground(Color.WHITE); // Reset to default background color
            }
        }
    
        // Update buttons based on current events
        for (Event event : events) {
            int hour = event.startTime.getHour() - 8; // Adjust hour to array index (8 AM starts at index 0)
            int day = event.startTime.getDayOfWeek().getValue() - 1; // Monday is 1, Sunday is 7 (index 0-6)
    
            if (hour >= 0 && hour < timeSlots.length && day >= 0 && day < timeSlots[hour].length) {
                JButton timeSlotButton = timeSlots[hour][day];
                timeSlotButton.setText(event.name); // Display event name
                timeSlotButton.setBackground(getColor(event.color)); // Set background color based on the event
            }
        }
    }
    

    // Event class to hold event details
    static class Event implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private String location;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        public String color;

        public Event(String name, String location, LocalDateTime startTime, LocalDateTime endTime, String color) {
            this.name = name;
            this.location = location;
            this.startTime = startTime;
            this.endTime = endTime;
            this.color = color;
        }

        public boolean overlaps(Event other) {
            return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format("%s - %s at %s", name, startTime.format(formatter), location);
        }
    }

    // Main method
    public static void main(String[] args) {
        LocalDate weekStartDate = promptForWeekStartDate();
        SwingUtilities.invokeLater(() -> {
            new WeeklySchedulerApp(weekStartDate);
        });
        
    }
    
}
