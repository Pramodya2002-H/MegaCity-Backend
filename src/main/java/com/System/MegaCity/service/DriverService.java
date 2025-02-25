package com.System.MegaCity.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Booking;
import com.System.MegaCity.model.Car;
import com.System.MegaCity.model.Driver;


@Service

public interface DriverService {

    List<Driver> getAllDrivers();
    Driver getDriverById(String driverId);
    ResponseEntity<?> createDriver(Driver driver , Car car);
    Driver updateDriver(String driverId, Driver driver);
    void deleteDriver(String driverId);
    Driver updateAvailability(String driverId, boolean availability);
    List<Booking> getDriverBookings(String driverId);
}
