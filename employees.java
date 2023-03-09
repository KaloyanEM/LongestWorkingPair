import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

public class employees {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;

    public static void main(String[] args) {
        // Start the GUI application
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    employees window = new employees();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public employees() {
        initialize();
    }

    private void initialize() {
        // Set up the main window
        frame = new JFrame();
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        // Set up the file selection panel
        JPanel panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        JLabel lblNewLabel = new JLabel("Select a CSV file:");
        panel.add(lblNewLabel);

        JButton btnNewButton = new JButton("Browse...");
        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    processFile(file);
                }
            }
        });
        panel.add(btnNewButton);

        // Set up the results table
        model = new DefaultTableModel();
        model.addColumn("Employee ID #1");
        model.addColumn("Employee ID #2");
        model.addColumn("Project ID");
        model.addColumn("Days worked");

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    private void processFile(File file) {
        try {
            // Read the input data from the CSV file
            ArrayList<String[]> data = new ArrayList<String[]>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                data.add(fields);
            }
            reader.close();
    
            // Define the supported date formats
            String[] dateFormats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM|dd|yyyy"};
    
            // Create a map of projects to a map of employees to their start and end dates
            Map<String, Map<String, DateRange>> projects = new HashMap<String, Map<String, DateRange>>();
            Date today = new Date();
            for (String[] fields : data) {
                String projectId = fields[1];   
                String employeeId = fields[0];
                Date fromDate = null;
                Date toDate = null;
                for (String dateFormat : dateFormats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                        fromDate = sdf.parse(fields[2]);
                        if (fields[3].equals("NULL")) {
                            toDate = today;
                        } else {
                            toDate = sdf.parse(fields[3]);
                        }
                        break;
                    } catch (ParseException e) {
                        // Ignore the exception and try the next date format
                    }
                }
                if (fromDate == null || toDate == null) {
                    throw new ParseException("Invalid date format", 0);
                }
                if (!projects.containsKey(projectId)) {
                    projects.put(projectId, new HashMap<String, DateRange>());
                }
                projects.get(projectId).put(employeeId, new DateRange(fromDate, toDate));
            }
    
            // Find the pair of employees who have worked together on common projects for the longest period of time
            List<ProjectEmployeeDaysWorked> pairs = new ArrayList<>();
            for (String projectId : projects.keySet()) {
                Map<String, DateRange> employees = projects.get(projectId);
                for (String employeeId1 : employees.keySet()) {
                    for (String employeeId2 : employees.keySet()) {
                        if (employeeId1.equals(employeeId2)) {
                            continue;
                        }
                        DateRange dateRange1 = employees.get(employeeId1);
                        DateRange dateRange2 = employees.get(employeeId2);
                        int days = dateRange1.getOverlapDays(dateRange2);
                        if (days > 0) {
                            ProjectEmployeeDaysWorked pair = new ProjectEmployeeDaysWorked(employeeId1, employeeId2, projectId, days);
                            ProjectEmployeeDaysWorked pairOther = new ProjectEmployeeDaysWorked(employeeId2, employeeId1, projectId, days);
                            if (!pairs.contains(pair) && !pairs.contains(pairOther) ) {
                                pairs.add(pair);
                            }
                        }
                    }
                }
            }
    
            // Display the results in the UI
            displayResults(pairs);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }    

    //A class to represent a date range between two dates
    private static class DateRange {
        private Date fromDate;
        private Date toDate;

        //Creates a new instance of DateRange with the given start and end dates
        public DateRange(Date fromDate, Date toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        //Returns the number of days that overlap between this date range and another date range
        public int getOverlapDays(DateRange other) {
            if (fromDate.after(other.toDate) || other.fromDate.after(toDate)) {
                return 0;
            }
            Date start = fromDate.after(other.fromDate) ? fromDate : other.fromDate;
            Date end = toDate.before(other.toDate) ? toDate : other.toDate;
            long overlapMillis = end.getTime() - start.getTime();
            return (int) (overlapMillis / (24 * 60 * 60 * 1000));
        }
    }

    public class ProjectEmployeeDaysWorked {
        private String employeeId1;
        private String employeeId2;
        private String projectId;
        private int daysWorked;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProjectEmployeeDaysWorked)) {
                return false;
            }
            ProjectEmployeeDaysWorked that = (ProjectEmployeeDaysWorked) o;
            return getEmployeeId1().equals(that.getEmployeeId1()) && getEmployeeId2().equals(that.getEmployeeId2());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getEmployeeId1(), getEmployeeId2());
        }
    
        //Constructor for creating a ProjectEmployeeDaysWorked object with employee IDs, project ID, and number of days worked
        public ProjectEmployeeDaysWorked(String employeeId1, String employeeId2, String projectId, int daysWorked) {
            this.employeeId1 = employeeId1;
            this.employeeId2 = employeeId2;
            this.projectId = projectId;
            this.daysWorked = daysWorked;
        }

        //Getters and setters for all parameter of ProjectEmployeeDaysWorked
        public String getEmployeeId1() {
            return employeeId1;
        }
    
        public void setEmployeeId1(String employeeId1) {
            this.employeeId1 = employeeId1;
        }
    
        public String getEmployeeId2() {
            return employeeId2;
        }
    
        public void setEmployeeId2(String employeeId2) {
            this.employeeId2 = employeeId2;
        }
    
        public String getProjectId() {
            return projectId;
        }
    
        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }
    
        public int getDaysWorked() {
            return daysWorked;
        }
    
        public void setDaysWorked(int daysWorked) {
            this.daysWorked = daysWorked;
        }
    }
    
    private void displayResults(List<ProjectEmployeeDaysWorked> pairs) {
        // Define the column names and types for the datagrid
        String[] columnNames = {"Employee ID #1", "Employee ID #2", "Project ID", "Days worked"};
    
        // Create a new JTable with the results data and column names/types
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        
        // Add each ProjectEmployeeDaysWorked object as a row in the table
        for (ProjectEmployeeDaysWorked pair : pairs) {
            Object[] rowData = {pair.getEmployeeId1(), pair.getEmployeeId2(), pair.getProjectId(), pair.getDaysWorked()};
            model.addRow(rowData);
        }
        // Create a new JTable with the model and set auto resize mode
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
        // Add the table to a scrollable pane and display it in a new JFrame
        JScrollPane scrollPane = new JScrollPane(table);
        JFrame frame = new JFrame("Common Projects");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
}