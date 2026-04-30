# ShipTrack - Secure Logistics System

![Java](https://img.shields.io/badge/Java-19-3b82f6?style=for-the-badge)
![Security](https://img.shields.io/badge/Security-SHA--256%20%2B%20Salt-22c55e?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Architecture-Defense%20in%20Depth-f97316?style=for-the-badge)

**Java 19 | Security Architecture**

ShipTrack is a secure, console-based logistics management application. It optimizes package delivery operations while strictly enforcing industry-standard security principles (**C.I.A. Triad**) and clean code practices.

---

## Key Features

### Role-Based Access Control (RBAC)

- **Customer**
  - Self-registration
  - Create shipment requests
  - Track own packages _(confidentiality enforced)_
  - Update personal information

- **Dispatcher**
  - Assign deliveries to drivers
  - Update delivery statuses
  - Register new delivery personnel

- **Delivery Personnel**
  - View assigned deliveries
  - Update delivery statuses

- **System Admin**
  - Register/remove staff
  - Define password policies
  - Lock/unlock accounts

---

## Security-Driven Design

- **Cryptography**
  - Passwords are never stored in plain text
  - Uses **Salted SHA-256 hashing**
  - Cryptographically secure salts via `SecureRandom`

- **Data Segregation**
  - Authentication data → `users.csv`
  - PII data → `sensitive_pii.csv`

- **Defense in Depth**
  - Backend-enforced permissions using **Access Control Matrix**
  - Independent of UI restrictions

- **Fail Securely**
  - Custom exceptions prevent stack trace leakage
  - Generic login errors prevent account enumeration

- **Accountability & Logging**
  - Logging via `java.util.logging`
  - Stored in `audit_log.log`
  - Captures:
    - User actions
    - Security alerts
    - System exceptions

- **Brute Force Protection**
  - Configurable login attempt limits
  - Automatic account locking

---

## Security & Quality Principles

| Principle                | Implementation                                                                        |
| ------------------------ | ------------------------------------------------------------------------------------- |
| Least Privilege          | Customers access only their shipments; Admins cannot delete customers or other admins |
| Defense in Depth         | Password policy + hashing + account lockout + RBAC enforcement                        |
| Fail Securely            | try-with-resources, custom exceptions, no stack trace exposure                        |
| Attack Surface Reduction | Generic login errors, restricted UI options                                           |
| Data Segregation         | Separation of authentication and PII data                                             |
| Maintainability          | Modular design (Models, Logic, UI, Utilities, Logging)                                |

---

## Data Architecture (6 Secure Data Stores)

| File                   | Description                                                              |
| ---------------------- | ------------------------------------------------------------------------ |
| `users.csv`            | Authentication & authorization (username, role, hash, salt, lock status) |
| `sensitive_pii.csv`    | Personally identifiable information (name, ID, contact)                  |
| `shipments.csv`        | Shipment data (ID, status, description)                                  |
| `password_policy.csv`  | Password rules with historical tracking                                  |
| `audit_log.log`        | Append-only log of actions and security events                           |
| `role_permissions.csv` | Access Control Matrix (RBAC rules)                                       |

---

## How to Run

### Prerequisites

- Java JDK 19 or higher

```bash
# Clone repository
git clone https://github.com/Yousef-G0/ShipTrack.git
cd ShipTrack

# Compile
javac *.java

# Run
java Main
```
