package com.System.MegaCity.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Driver;
import com.System.MegaCity.repository.DriverRepository;

@Service
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Override
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @Override
    public Driver getDriverById(String driverId) {
        return driverRepository.findById(driverId).orElse(null);
    }

    @Override
    public Driver createDriver(Driver driver) {
        return driverRepository.save(driver);
    }

    @Override
    public Driver updateDriver(String driverId, Driver driver) {
        Driver existingDriver = getDriverById(driverId);

        existingDriver.setDriverName(driver.getDriverName());
        existingDriver.setDriverEmail(driver.getDriverEmail());
        existingDriver.setDriverPassword(driver.getDriverPassword());
        existingDriver.setLicenseNumber(driver.getLicenseNumber());

        return driverRepository.save(existingDriver);
    }

    @Override
    public void deleteDriver(String driverId) {
        driverRepository.deleteById(driverId);
    }

}
