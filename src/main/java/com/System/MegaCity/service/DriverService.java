package com.System.MegaCity.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Driver;

@Service
public interface DriverService {
    List<Driver> getAllDrivers();

    Driver getDriverById(String driverId);

    Driver createDriver(Driver driver);

    Driver updateDriver(String driverId, Driver driver);

    void deleteDriver(String driverId);
}
