package com.System.MegaCity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.System.MegaCity.model.Driver;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {



}
