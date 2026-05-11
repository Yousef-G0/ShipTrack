package org.example;

public class Shipment {
    private String shipmentId;
    private String customerId;
    private String assignedDriverId;
    private String status; // pending, in transit, delivered, picked up
    private String description;

    public Shipment(String shipmentId, String customerId, String description) {
        this.shipmentId = shipmentId;
        this.customerId = customerId;
        this.description = description;
        this.assignedDriverId = null;
        this.status = "pending";
    }

    public String getShipmentId() { return shipmentId; }
    public String getCustomerId() { return customerId; }
    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String driverId) { this.assignedDriverId = driverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
}