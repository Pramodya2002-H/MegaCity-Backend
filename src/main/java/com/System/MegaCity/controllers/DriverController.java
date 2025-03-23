package com.System.MegaCity.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.System.MegaCity.exception.ResourceNotFoundException;
import com.System.MegaCity.model.Booking;
import com.System.MegaCity.model.Car;
import com.System.MegaCity.model.Customer;
import com.System.MegaCity.model.Driver;
import com.System.MegaCity.repository.CustomerRepository;
import com.System.MegaCity.service.BookingService;
import com.System.MegaCity.service.CloudinaryService;
import com.System.MegaCity.service.DriverService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/auth/driver")
@Slf4j
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/getalldrivers")
    public List<Driver> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/getdriver/{driverId}")
    public ResponseEntity<Driver> getDriverById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId) {
        String email = userDetails.getUsername();

        // Check if the user has a booking with this driver
        boolean hasBooking = bookingService.hasBookingWithDriver(email, driverId);
        if (!hasBooking) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Driver driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(driver);
    }

    @PostMapping(value = "/createdriver", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDriver(
            @RequestParam("driverName") String driverName,
            @RequestParam("email") String email,
            @RequestParam("driverVehicalLicense") String driverVehicalLicense,
            @RequestParam("driverPhone") String driverPhone,
            @RequestParam("password") String password,
            @RequestParam("hasOwnCar") boolean hasOwnCar,
            @RequestParam(value = "carLicensePlate", required = false) String carLicensePlate,
            @RequestParam(value = "carModel", required = false) String carModel,
            @RequestParam(value = "capacity", required = false) Integer capacity,
            @RequestParam(value = "baseRate", required = false) Double baseRate,
            @RequestParam(value = "driverRate", required = false) Double driverRate,
            @RequestParam(value = "carImg", required = false) MultipartFile carImg) {

        try {
            Driver driver = new Driver();
            driver.setDriverName(driverName);
            driver.setEmail(email);
            driver.setDriverVehicalLicense(driverVehicalLicense);
            driver.setDriverPhoneNum(driverPhone);
            driver.setPassword(password);
            driver.setHasOwnCar(hasOwnCar);

            Car car = null;
            if (hasOwnCar) {
                car = new Car();
                car.setLicensePlate(carLicensePlate);
                car.setModel(carModel);
                car.setCapacity(capacity != null ? capacity : 4);
                car.setBaseRate(baseRate != null ? baseRate : 0.0);
                car.setDriverRate(driverRate != null ? driverRate : 0.0);
                if (carImg != null && !carImg.isEmpty()) {
                    String carImageUrl = cloudinaryService.uploadImage(carImg);
                    car.setCarImage(carImageUrl);
                }
            }

            return driverService.createDriver(driver, car);

        } catch (Exception e) {
            log.error("Error creating driver: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating driver: " + e.getMessage());
        }
    }

    @PutMapping("/updatedriver/{driverId}")
    public ResponseEntity<Driver> updateDriver(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId,
            @RequestBody Driver driver) {
        String email = userDetails.getUsername();
        log.info("Updating driver with ID: {} for email: {}", driverId, email);

        Driver updatedDriver = driverService.updateDriver(driverId, driver);
        return ResponseEntity.ok(updatedDriver);
    }

    @PutMapping("/{driverId}/availability")
    public ResponseEntity<Driver> updateAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId,
            @RequestBody Map<String, Boolean> availability) {
        String email = userDetails.getUsername();
        log.info("Updating availability for driver: {} for email: {}", driverId, email);

        if (!availability.containsKey("availability")) {
            return ResponseEntity.badRequest().build();
        }

        Driver driver = driverService.updateAvailability(driverId, availability.get("availability"));
        return ResponseEntity.ok(driver);
    }

    @GetMapping("/{driverId}/bookings")
    public ResponseEntity<List<Booking>> getDriverBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId) {
        String email = userDetails.getUsername();
        log.info("Fetching bookings for driver: {} for email: {}", driverId, email);

        Driver driver = driverService.getDriverById(driverId);
        if (!email.equals(driver.getEmail())) {
            log.warn("Unauthorized access attempt by user: {}", email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Booking> bookings = driverService.getDriverBookings(driverId).stream()
                .map(booking -> {
                    Customer customer = customerRepository.findById(booking.getCustomerId())
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                    booking.setPassengerName(customer.getCustomerName());

                    return booking;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> deleteDriver(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String driverId) {
        String email = userDetails.getUsername();
        log.info("Deleting driver with ID: {} for email: {}", driverId, email);

        driverService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }

}
