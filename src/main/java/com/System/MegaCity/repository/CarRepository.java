package com.System.MegaCity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.System.MegaCity.model.Car;

@Repository
public interface CarRepository extends MongoRepository<Car,String>{

        
}
