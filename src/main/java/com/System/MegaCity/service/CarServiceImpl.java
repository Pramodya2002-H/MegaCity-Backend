package com.System.MegaCity.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Car;
import com.System.MegaCity.repository.CarRepository;


@Service
public class CarServiceImpl implements CarService {

    @Autowired
    private CarRepository carRepository;

    @Override
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Override
    public Car getCarById(String carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found with ID: " + carId));
    }

    @Override
    public Car createCar(Car car) {
        return carRepository.save(car);
    }

    @Override
    public Car updateCar(String carId, Car car) {
        Car existingCar = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found"));
        existingCar.setBrand(car.getBrand());
        existingCar.setModel(car.getModel());
        existingCar.setLicensePlate(car.getLicensePlate());
        return carRepository.save(existingCar);
    }

    @Override
    public void deleteCar(String carId) {
        carRepository.deleteById(carId);
    }

}
