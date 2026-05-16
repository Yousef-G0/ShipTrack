package org.example;

import java.util.Scanner;

public class Main {
    private static final ShipTrackSystem system = new ShipTrackSystem();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        // Load data on startup
        system.loadPolicyData();
        system.loadUsers();
        system.loadShipments();
        system.initializePermissions();
        System.out.println(" Welcome to ShipTrack - Secure Logistics System");

        // First-time setup check
        if (!system.adminExists()) {
            System.out.println("\n First-Time Setup: Create System Admin Account");
            System.out.print("Admin Username: ");
            String adminUser = sc.nextLine();
            System.out.print("Full Name: ");
            String adminName = sc.nextLine();
            System.out.print("ID Number: ");
            String adminId = sc.nextLine();
            System.out.print("Contact: ");
            String adminContact = sc.nextLine();
            System.out.print("Password (must meet policy): ");
            String adminPass = sc.nextLine();

            try {
                system.registerUser(adminUser, adminName, adminId, adminContact, User.Role.SYSTEM_ADMIN, adminPass);
                System.out.println(" System Admin created successfully.");
            } catch (RegistrationException e) {
                System.out.println(" Admin creation failed: " + e.getMessage());
                return;
            }
        }

        // Main Menu
        label:
        while (true) {
            System.out.println("\n=== ShipTrack ===");
            System.out.println("1. Login");
            System.out.println("2. Register as a Customer");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    handleRegistration();
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    break label;
            }
        }
    }

    private static void handleLogin() {
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        User loggedIn = system.login(username, password);
        if (loggedIn != null) routeByRole(loggedIn);
    }

    private static void handleRegistration() {
        System.out.println("\n--- Customer Registration ---");
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("ID: ");
        String id = sc.nextLine();
        System.out.print("Contact: ");
        String contact = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        try {
            User registeredUser = system.registerUser(username, name, id, contact, User.Role.CUSTOMER, password);

            System.out.println(" Registered successfully!");

            routeByRole(registeredUser);

        } catch (RegistrationException e) {
            System.out.println(" Registration Failed: " + e.getMessage());
        }
    }
    private static void routeByRole(User user) {
        System.out.println("\n Logged in as: " + user.getUsername() + " | Role: " + user.getRole());

        // Maintainability: Clear routing based on role
        if (user.getRole() == User.Role.CUSTOMER) customerMenu(user);
        else if (user.getRole() == User.Role.DISPATCHER) dispatcherMenu(user);
        else if (user.getRole() == User.Role.DELIVERY_PERSONNEL) driverMenu(user);
        else if (user.getRole() == User.Role.SYSTEM_ADMIN) adminMenu(user);
    }

    private static void customerMenu(User user) {
        label:
        while (true) {
            System.out.println("\n[C]reate Shipment  [T]rack  [U]pdate Info  [L]ogout");
            String choice = sc.nextLine().toUpperCase();

            switch (choice) {
                case "L":
                    break label;
                case "C": {
                    System.out.print("Description: ");
                    String desc = sc.nextLine();
                    String id = system.createShipment(user.getUsername(), desc);
                    System.out.println(" Shipment ID: " + id);
                    break;
                }
                case "T": {
                    System.out.print("Shipment ID: ");
                    String id = sc.nextLine();
                    System.out.println(" Status: " + system.trackShipment(id, user.getUsername()));
                    break;
                }
                case "U":
                    System.out.print("New Name (enter to skip): ");
                    String name = sc.nextLine();
                    System.out.print("New Contact (enter to skip): ");
                    String contact = sc.nextLine();
                    system.updateUserInfo(user.getUsername(), name.isEmpty() ? null : name, contact.isEmpty() ? null : contact);
                    System.out.println(" Info updated.");
                    break;
            }
        }
    }

    private static void dispatcherMenu(User user) {
        label:
        while (true) {
            System.out.println("\n[A]ssign Delivery  [S]tatus Update  [R]egister Personnel  [U]pdate Info  [L]ogout");
            String choice = sc.nextLine().toUpperCase();

            switch (choice) {
                case "L":
                    break label;
                case "A": {
                    System.out.print("Shipment ID: ");
                    String sId = sc.nextLine();
                    System.out.print("Driver Username: ");
                    String driver = sc.nextLine();
                    if (system.assignDelivery(sId, driver, user.getRole())) System.out.println(" Assigned!");
                    else System.out.println(" Failed to assign.");
                    break;
                }
                case "S": {
                    System.out.print("Shipment ID: ");
                    String sId = sc.nextLine();
                    System.out.print("New Status (pending/in transit/delivered): ");
                    String status = sc.nextLine();
                    if (system.updateDeliveryStatusByDispatcher(sId, status, user.getRole())) System.out.println(" Status updated!");
                    else System.out.println(" Invalid status or shipment.");
                    break;
                }
                case "R":
                    System.out.println("-- Register Delivery Personnel --");
                    System.out.print("Username: ");
                    String u = sc.nextLine();
                    System.out.print("Name: ");
                    String n = sc.nextLine();
                    System.out.print("ID: ");
                    String i = sc.nextLine();
                    System.out.print("Contact: ");
                    String c = sc.nextLine();
                    System.out.print("Password: ");
                    String p = sc.nextLine();
                    try {
                        system.registerUser(u, n, i, c, User.Role.DELIVERY_PERSONNEL, p);
                        System.out.println(" Delivery personnel registered!");
                    } catch (RegistrationException e) {
                        System.out.println(" Registration Failed: " + e.getMessage());
                    }
                    break;
                case "U":
                    System.out.print("New Contact: ");
                    String contact = sc.nextLine();
                    system.updateUserInfo(user.getUsername(), null, contact);
                    System.out.println(" Updated.");
                    break;
            }
        }
    }

    private static void driverMenu(User user) {
        while (true) {
            System.out.println("\n[V]iew Assignments  [U]pdate Status  [L]ogout");
            String choice = sc.nextLine().toUpperCase();

            if (choice.equals("L")) break;
            else if (choice.equals("V")) {

                String assignments = system.getDriverAssignments(user.getUsername());
                System.out.println(assignments);
            } else if (choice.equals("U")) {
                System.out.print("Shipment ID: ");
                String sId = sc.nextLine();
                System.out.print("New Status (picked up/in transit/delivered): ");
                String status = sc.nextLine();
                if (system.updateDeliveryStatus(sId, status,user.getRole())) System.out.println(" Status updated!");
                else System.out.println(" Invalid status or shipment.");
            }
        }
    }

    private static void adminMenu(User user) {
        while (true) {
            System.out.println("\n[P]olicy Setup  [R]egister User  [D]elete User  [K]Lock/Unlock  [L]ogout");
            try {
                String c = sc.nextLine().toUpperCase();
                if (c.equals("L")) break;
                else if (c.equals("P")) {
                    System.out.println("\n--- Password Policy Setup ---");
                    System.out.println("Current Policy:");
                    System.out.println("  Min Length      → 8");
                    System.out.println("  Min Uppercase   → 1");
                    System.out.println("  Min Lowercase   → 1");
                    System.out.println("  Min Digits      → 1");
                    System.out.println("  Min Special Chars → 1");
                    System.out.println("  Max Login Attempts → 3");
                    System.out.println("\nEnter NEW values as NUMBERS separated by spaces:");
                    System.out.println("Example: 10 2 2 2 1 5");
                    System.out.println("  (This means: min 10 chars, 2 upper, 2 lower, 2 digits, 1 special, 5 max attempts)");
                    System.out.print("\nYour input: ");
                    String[] parts = sc.nextLine().trim().split(" ");
                    if (parts.length != 6) {
                        System.out.println(" Invalid input. You must enter exactly 6 numbers.");
                        continue;
                    }
                    try {
                        system.setPolicy(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                                Integer.parseInt(parts[4]), Integer.parseInt(parts[5]),user.getRole());
                        System.out.println(" Policy updated successfully!");
                        System.out.println("  → Min Length: " + parts[0]
                                + ", Min Upper: " + parts[1]
                                + ", Min Lower: " + parts[2]
                                + ", Min Digits: " + parts[3]
                                + ", Min Special: " + parts[4]
                                + ", Max Attempts: " + parts[5]);
                    } catch (NumberFormatException e) {
                        System.out.println(" Invalid input. Please enter NUMBERS only.");
                    }
                } else if (c.equals("R")) {
                    System.out.println("\n--- Register New Staff ---");
                    System.out.println("Roles: 1-Dispatcher 2-DeliveryPersonnel 3-Admin");
                    String roleChoice = sc.nextLine().trim();

                    User.Role role = null;
                    if (roleChoice.equals("1")) role = User.Role.DISPATCHER;
                    else if (roleChoice.equals("2")) role = User.Role.DELIVERY_PERSONNEL;
                    else if (roleChoice.equals("3")) role = User.Role.SYSTEM_ADMIN;
                    else {
                        System.out.println(" Invalid role selection.");
                        continue;
                    }

                    System.out.print("Username: ");
                    String u = sc.nextLine();
                    System.out.print("Name: ");
                    String n = sc.nextLine();
                    System.out.print("ID: ");
                    String i = sc.nextLine();
                    System.out.print("Contact: ");
                    String c1 = sc.nextLine();
                    System.out.print("Password: ");
                    String p = sc.nextLine();
                    try {
                        system.registerUser(u, n, i, c1, role, p);
                        System.out.println(" Staff registered successfully!");
                    } catch (RegistrationException e) {
                        System.out.println(" Registration Failed: " + e.getMessage());
                    }
                } else if (c.equals("D")) {
                System.out.print("Username to remove: ");
                String target = sc.nextLine();
                String info = system.getUserInfo(target);

                // Check if user exists first
                if (info.equals("User not found.")) {
                    System.out.println("  " + info);
                    continue;
                }

                System.out.println("  " + info);
                System.out.print("Are you sure you want to delete this user? (yes/no): ");
                String confirm = sc.nextLine().trim().toLowerCase();

                if (confirm.equals("yes")) {
                    // Capture the true/false result from the secure backend method
                    boolean success = system.removeStaffUser(target, user.getRole());

                    if (success) {
                        System.out.println(" Staff member removed successfully.");
                    } else {
                        System.out.println(" Cannot remove this user. Admin can only remove Dispatchers and Delivery Personnel.");
                    }
                } else {
                    System.out.println(" Cancelled.");
                }
            } else if (c.equals("K")) {
                    System.out.print("Username to manage: ");
                    String target = sc.nextLine();
                    String info = system.getUserInfo(target);
                    System.out.println("  " + info);
                    if (info.equals("User not found.")) continue;

                    System.out.print("What do you want to do? [L]ock  [U]nlock: ");
                    String action = sc.nextLine().toUpperCase();
                    User targetUser = system.getUser(target); // You'll need to add this getter
                    if (action.equals("L") && !targetUser.isLocked()) {
                        system.toggleLock(target,user.getRole());
                        System.out.println(" Account LOCKED ");
                    } else if (action.equals("U") && targetUser.isLocked()) {
                        system.toggleLock(target,user.getRole());
                        System.out.println(" Account UNLOCKED ");
                    } else if (action.equals("L")) {
                        System.out.println(" Account is already locked.");
                    } else if (action.equals("U")) {
                        System.out.println(" Account is already unlocked.");
                    } else {
                        System.out.println(" Invalid option.");
                    }
                }
            } catch (Exception e) {
                System.out.println(" Invalid input.");
            }
        }
    }
}
