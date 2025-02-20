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
        return carRepository.findById(carId).orElse(null);
    }

    @Override
    public Car createCar(Car car) {
        return carRepository.save(car);
    }

    @Override
    public void deleteCar(String carId) {

        carRepository.deleteById(carId);
    }

    @Override
    public Car updateCar(String carId, Car car) {

        Car existingCar = getCarById(carId);

        existingCar.setBrand(car.getBrand());
        existingCar.setCapacity(car.getCapacity());
        existingCar.setLicensePlate(car.getLicensePlate());
        existingCar.setModel(car.getModel());

        return carRepository.save(existingCar);

    }

}
