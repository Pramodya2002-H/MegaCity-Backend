package com.System.MegaCity.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.System.MegaCity.model.Driver;
import com.System.MegaCity.service.DriverService;

@RestController
public class DriverController {
    @Autowired
    private DriverService driverService;

    @GetMapping("/driver")
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/viewDriver/{driverId}")
    public ResponseEntity<Driver> getDrivertById(@PathVariable String driverId) {
        Driver driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(driver);
    }

    @PostMapping("/createDriver")
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        Driver createDriver = driverService.createDriver(driver);
        return ResponseEntity.status(HttpStatus.CREATED).body(createDriver);
    }

    @PutMapping("/updateDriver/{driverId}")
    public ResponseEntity<Driver> upadateDriver(@PathVariable String driverId, @RequestBody Driver driver) {
        Driver updatedDriver = driverService.updateDriver(driverId, driver);
        return ResponseEntity.ok(updatedDriver);
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<String> deleteDriver(@PathVariable String driverId) {
        driverService.deleteDriver(driverId);
        return ResponseEntity.ok("Driver deleted successfully");
    }
}
