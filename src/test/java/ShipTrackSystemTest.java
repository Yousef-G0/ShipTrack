
import org.example.RegistrationException;
import org.example.ShipTrackSystem;
import org.example.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class ShipTrackSystemTest {

    private ShipTrackSystem system;

    @BeforeEach
    public void setUp() {
        system = new ShipTrackSystem();
        system.initializePermissions();
    }

    @AfterEach
    public void tearDown() {
        deleteFile("users.csv");
        deleteFile("sensitive_pii.csv");
        deleteFile("shipments.csv");
        deleteFile("password_policy.csv");
        deleteFile("audit_log.log");
        deleteFile("role_permissions.csv");
    }

    private void deleteFile(String filename) {
        File file = new File(filename);
        if (file.exists()) file.delete();
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================

    @Test
    public void testRegisterCustomerSuccess() throws RegistrationException {
        boolean result = system.registerUser("cust1", "Customer One", "ID1", "123", User.Role.CUSTOMER, "Cust@123");
        assertTrue(result, "Customer should register successfully with valid password");
    }

    @Test
    public void testRegisterWeakPassword() {
        assertThrows(RegistrationException.class, () -> {
            system.registerUser("cust2", "Customer Two", "ID2", "456", User.Role.CUSTOMER, "weak");
        }, "Weak password should throw RegistrationException");
    }

    @Test
    public void testRegisterDuplicateUsername() throws RegistrationException {
        system.registerUser("cust1", "Customer One", "ID1", "123", User.Role.CUSTOMER, "Cust@123");

        assertThrows(RegistrationException.class, () -> {
            system.registerUser("cust1", "Another One", "ID3", "789", User.Role.CUSTOMER, "Another@1");
        }, "Duplicate username should throw RegistrationException");
    }

    // ==========================================
    // AUTHENTICATION & LOCKOUT TESTS
    // ==========================================

    @Test
    public void testLoginSuccess() throws RegistrationException {
        system.registerUser("cust1", "Customer One", "ID1", "123", User.Role.CUSTOMER, "Cust@123");
        User loggedIn = system.login("cust1", "Cust@123");

        assertNotNull(loggedIn, "User should log in with correct credentials");
        assertEquals("cust1", loggedIn.getUsername(), "Logged in username should match");
    }

    @Test
    public void testLoginWrongPassword() throws RegistrationException {
        system.registerUser("cust1", "Customer One", "ID1", "123", User.Role.CUSTOMER, "Cust@123");
        User loggedIn = system.login("cust1", "WrongPass@1");

        assertNull(loggedIn, "User should not log in with wrong password");
    }

    @Test
    public void testAccountLockoutMechanism() throws RegistrationException {
        system.registerUser("cust1", "Customer One", "ID1", "123", User.Role.CUSTOMER, "Cust@123");

        // Fail 3 times (default max attempts)
        system.login("cust1", "Wrong1@");
        system.login("cust1", "Wrong2@");
        system.login("cust1", "Wrong3@");

        // Try to login with CORRECT password
        User loggedIn = system.login("cust1", "Cust@123");

        assertNull(loggedIn, "Account should be locked after 3 failed attempts, denying correct login");
    }

    // ==========================================
    // AUTHORIZATION (LEAST PRIVILEGE) TESTS
    // ==========================================

    @Test
    public void testAdminCanRemoveDispatcher() throws RegistrationException {
        system.registerUser("admin1", "Admin", "ID1", "123", User.Role.SYSTEM_ADMIN, "Admin@123");
        system.registerUser("disp1", "Dispatcher", "ID2", "456", User.Role.DISPATCHER, "Disp@123");

        boolean removed = system.removeStaffUser("disp1", User.Role.SYSTEM_ADMIN);
        assertTrue(removed, "Admin should be able to remove a Dispatcher");
    }

    @Test
    public void testAdminCannotRemoveCustomer() throws RegistrationException {
        system.registerUser("admin1", "Admin", "ID1", "123", User.Role.SYSTEM_ADMIN, "Admin@123");
        system.registerUser("cust1", "Customer", "ID2", "456", User.Role.CUSTOMER, "Cust@123");

        boolean removed = system.removeStaffUser("cust1", User.Role.SYSTEM_ADMIN);
        assertFalse(removed, "Admin should NOT be able to remove a Customer");
    }

    // ==========================================
    // CONFIDENTIALITY TESTS
    // ==========================================

    @Test
    public void testCustomerCanTrackOwnShipment() throws RegistrationException {
        system.registerUser("cust1", "Customer", "ID1", "123", User.Role.CUSTOMER, "Cust@123");
        String shipmentId = system.createShipment("cust1", "Laptop");

        String status = system.trackShipment(shipmentId, "cust1");
        assertEquals("pending", status, "Customer should be able to track their own shipment");
    }

    @Test
    public void testCustomerCannotTrackOtherShipment() throws RegistrationException {
        system.registerUser("cust1", "Customer 1", "ID1", "123", User.Role.CUSTOMER, "Cust@123");
        system.registerUser("cust2", "Customer 2", "ID2", "456", User.Role.CUSTOMER, "Cust2@123");

        String shipmentId = system.createShipment("cust1", "Secret Laptop");

        String status = system.trackShipment(shipmentId, "cust2");
        assertEquals("Not Found", status, "Customer should not see other customer's shipment");
    }
}