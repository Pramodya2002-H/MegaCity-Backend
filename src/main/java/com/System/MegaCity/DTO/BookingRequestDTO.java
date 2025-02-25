package com.System.MegaCity.DTO;

import lombok.Data;

@Data
public class BookingRequestDTO {
    private String customerId;
    private String carId;
    private String pickupLocation;
    private String dropLocation;
    private String pickupDate;
    private boolean driverRequired;
    
}
