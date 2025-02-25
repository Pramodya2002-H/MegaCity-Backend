package com.System.MegaCity.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.System.MegaCity.model.Car;

@Repository
public interface CarRepository extends MongoRepository<Car, String> {

    List<Car> findByAvailable(boolean available);

    List<Car> findByAssignedDriverId(String assignedDriverId);
}
