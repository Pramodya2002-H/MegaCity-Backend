package com.System.MegaCity.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "cars")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Car {
    @Id
    private String carId;
    private String brand;
    private String model;
    private String licensePlate;
    private int capacity;
    private String carImage;
    private boolean available= true;
    private String assignedDriverId;
    private double baseRate;
    private double driverRate;
}
