package com.System.MegaCity.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.System.MegaCity.model.Car;
import com.System.MegaCity.service.CarService;
import com.System.MegaCity.service.CloudinaryService;

@RestController
@RequestMapping(value = "/auth/cars")
@CrossOrigin(origins = "*")

public class CarController {
    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private CarService carService;

    @GetMapping("/car")
    public List<Car> getAllCars() {
        return carService.getAllCars();
    }

    @GetMapping("/{carId}")
    public ResponseEntity<Car> getCarById(@PathVariable String carId) {
        Car car = carService.getCarById(carId);
        return new ResponseEntity<>(carService.getCarById(carId), HttpStatus.OK);
    }

    @PostMapping("/createCar")
    public ResponseEntity<Car> createCar(@RequestParam String brand,
            @RequestParam String model,
            @RequestParam String licenseplate,
            @RequestParam int capacity,
            @RequestParam String availableStatus,
            @RequestParam String baseRate,
            @RequestParam MultipartFile carImg) throws IOException {

        String carImage = cloudinaryService.uploadImage(carImg);

        Car car = new Car();
        car.setBrand(brand);
        car.setModel(model);
        car.setLicensePlate(licenseplate);
        car.setCarImage(carImage);
        car.setCapacity(capacity);

        Car savedCar = carService.createCar(car);
        return ResponseEntity.ok(savedCar);
    }

    @PutMapping("/updateCar/{carId}")
    public ResponseEntity<Car> upadateCar(@PathVariable String carId, @RequestBody Car car) {
        Car updatedCar = carService.updateCar(carId, car);
        return new ResponseEntity<>(updatedCar, HttpStatus.OK);
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<String> deleteCar(@PathVariable String carId) {
        carService.deleteCar(carId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
