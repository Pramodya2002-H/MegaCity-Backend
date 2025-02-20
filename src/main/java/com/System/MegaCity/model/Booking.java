package com.System.MegaCity.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "bookings")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Booking {
    @Id
    
    private String bookingId;
    private String pickupLoction;
    private String dropLocation;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String bookingStatus;
    private String bookingDate;
    
   
}
