package elms;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class ELMS {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        int choice;

        do {
            System.out.println("\n===== EMPLOYEE LEAVE MANAGEMENT SYSTEM =====");
            System.out.println("1. Register Employee");
            System.out.println("2. View Employees");
            System.out.println("3. Apply for Leave");
            System.out.println("4. Approve or Reject Leave");
            System.out.println("5. View Leave Balance");
            System.out.println("6. View All Leave Requests");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            choice = readInt();

            try {
                switch (choice) {

                    case 1:
                        registerEmployee();
                        break;

                    case 2:
                        viewEmployees();
                        break;

                    case 3:
                        applyForLeave();
                        break;

                    case 4:
                        updateLeaveStatus();
                        break;

                    case 5:
                        viewLeaveBalance();
                        break;

                    case 6:
                        viewLeaveRequests();
                        break;

                    case 0:
                        System.out.println("Thank you for using ELMS!");
                        break;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }

            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }

        } while (choice != 0);

        sc.close();
    }

    // Read an integer safely
    static int readInt() {
        while (!sc.hasNextInt()) {
            System.out.print("Enter a valid number: ");
            sc.next();
        }

        int value = sc.nextInt();
        sc.nextLine();
        return value;
    }

    // 1. Employee Registration
    static void registerEmployee() throws SQLException {

        System.out.print("Enter employee name: ");
        String name = sc.nextLine();

        System.out.print("Enter employee email: ");
        String email = sc.nextLine();

        System.out.print("Enter department: ");
        String department = sc.nextLine();

        String sql = "INSERT INTO employees "
                + "(emp_name, email, department, leave_balance) "
                + "VALUES (?, ?, ?, 20)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, department);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Employee registered successfully!");
            }
        }
    }

    // 2. Display Employees
    static void viewEmployees() throws SQLException {

        String sql = "SELECT * FROM employees";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n----- EMPLOYEE DETAILS -----");

            while (rs.next()) {

                System.out.println(
                    "ID: " + rs.getInt("emp_id")
                    + " | Name: " + rs.getString("emp_name")
                    + " | Email: " + rs.getString("email")
                    + " | Department: " + rs.getString("department")
                    + " | Leave Balance: "
                    + rs.getInt("leave_balance")
                );
            }
        }
    }

    // 3. Apply for Leave
    static void applyForLeave() throws SQLException {

        System.out.print("Enter employee ID: ");
        int empId = readInt();

        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startInput = sc.nextLine();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endInput = sc.nextLine();

        System.out.print("Enter reason for leave: ");
        String reason = sc.nextLine();

        try {

            LocalDate start = LocalDate.parse(startInput);
            LocalDate end = LocalDate.parse(endInput);
            LocalDate today = LocalDate.now();

            if (start.isBefore(today)) {
                System.out.println("Leave start date cannot be in the past.");
                return;
            }

            if (end.isBefore(start)) {
                System.out.println("End date cannot be before start date.");
                return;
            }

            int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

            try (Connection con = DBConnection.getConnection()) {

                String employeeSql =
                        "SELECT leave_balance FROM employees WHERE emp_id = ?";

                try (PreparedStatement employeePs =
                        con.prepareStatement(employeeSql)) {

                    employeePs.setInt(1, empId);

                    try (ResultSet rs = employeePs.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println("Employee not found.");
                            return;
                        }

                        int balance = rs.getInt("leave_balance");

                        if (days > balance) {
                            System.out.println(
                                "Insufficient leave balance. Available: "
                                + balance
                            );
                            return;
                        }
                    }
                }

                String sql = "INSERT INTO leave_requests "
                        + "(emp_id, start_date, end_date, reason, status, days) "
                        + "VALUES (?, ?, ?, ?, 'PENDING', ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, empId);
                    ps.setDate(2, Date.valueOf(start));
                    ps.setDate(3, Date.valueOf(end));
                    ps.setString(4, reason);
                    ps.setInt(5, days);

                    ps.executeUpdate();

                    System.out.println(
                        "Leave application submitted successfully!"
                    );
                    System.out.println("Number of days: " + days);
                    System.out.println("Status: PENDING");
                }
            }

        } catch (java.time.format.DateTimeParseException e) {

            System.out.println("Invalid date format. Use YYYY-MM-DD.");
        }
    }

    // 4. Approve or Reject Leave
    static void updateLeaveStatus() throws SQLException {

        System.out.print("Enter leave request ID: ");
        int leaveId = readInt();

        System.out.print("Enter employee ID: ");
        int empId = readInt();

        System.out.print("Enter decision (APPROVE/REJECT): ");
        String decision = sc.nextLine().trim().toUpperCase();

        if (!decision.equals("APPROVE")
                && !decision.equals("REJECT")) {

            System.out.println("Invalid decision.");
            return;
        }

        String newStatus = decision.equals("APPROVE")
                ? "APPROVED" : "REJECTED";

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                String sql = "SELECT emp_id, days, status "
                        + "FROM leave_requests "
                        + "WHERE leave_id = ? FOR UPDATE";

                int days;
                String oldStatus;

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, leaveId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println("Leave request not found.");
                            con.rollback();
                            return;
                        }

                        if (rs.getInt("emp_id") != empId) {
                            System.out.println("Employee ID does not match.");
                            con.rollback();
                            return;
                        }

                        days = rs.getInt("days");
                        oldStatus = rs.getString("status");
                    }
                }

                if (!oldStatus.equals("PENDING")) {
                    System.out.println(
                        "This request has already been processed."
                    );
                    con.rollback();
                    return;
                }

                if (newStatus.equals("APPROVED")) {

                    String balanceSql =
                            "SELECT leave_balance FROM employees "
                            + "WHERE emp_id = ? FOR UPDATE";

                    try (PreparedStatement ps =
                            con.prepareStatement(balanceSql)) {

                        ps.setInt(1, empId);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                System.out.println("Employee not found.");
                                con.rollback();
                                return;
                            }

                            if (rs.getInt("leave_balance") < days) {
                                System.out.println(
                                    "Insufficient leave balance."
                                );
                                con.rollback();
                                return;
                            }
                        }
                    }

                    String balanceUpdate =
                            "UPDATE employees "
                            + "SET leave_balance = leave_balance - ? "
                            + "WHERE emp_id = ?";

                    try (PreparedStatement ps =
                            con.prepareStatement(balanceUpdate)) {

                        ps.setInt(1, days);
                        ps.setInt(2, empId);
                        ps.executeUpdate();
                    }
                }

                String updateSql =
                        "UPDATE leave_requests "
                        + "SET status = ? WHERE leave_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateSql)) {

                    ps.setString(1, newStatus);
                    ps.setInt(2, leaveId);
                    ps.executeUpdate();
                }

                con.commit();

                System.out.println(
                    "Leave request " + newStatus + " successfully!"
                );

            } catch (SQLException e) {

                con.rollback();
                throw e;

            } finally {

                con.setAutoCommit(true);
            }
        }
    }

    // 5. View Leave Balance
    static void viewLeaveBalance() throws SQLException {

        System.out.print("Enter employee ID: ");
        int empId = readInt();

        String sql = "SELECT emp_name, leave_balance "
                + "FROM employees WHERE emp_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    System.out.println(
                        "Employee Name: " + rs.getString("emp_name")
                    );

                    System.out.println(
                        "Available Leave Balance: "
                        + rs.getInt("leave_balance")
                    );

                } else {

                    System.out.println("Employee not found.");
                }
            }
        }
    }

    // 6. View All Leave Requests
    static void viewLeaveRequests() throws SQLException {

        String sql = "SELECT l.leave_id, e.emp_name, "
                + "l.start_date, l.end_date, l.days, "
                + "l.reason, l.status "
                + "FROM leave_requests l "
                + "JOIN employees e ON l.emp_id = e.emp_id "
                + "ORDER BY l.leave_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n----- LEAVE REQUESTS -----");

            while (rs.next()) {

                System.out.println(
                    "Leave ID: " + rs.getInt("leave_id")
                    + " | Employee: " + rs.getString("emp_name")
                    + " | Start: " + rs.getDate("start_date")
                    + " | End: " + rs.getDate("end_date")
                    + " | Days: " + rs.getInt("days")
                    + " | Reason: " + rs.getString("reason")
                    + " | Status: " + rs.getString("status")
                );
            }
        }
    }
}
