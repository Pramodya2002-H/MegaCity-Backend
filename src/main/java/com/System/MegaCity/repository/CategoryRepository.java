package com.System.MegaCity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.System.MegaCity.model.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

}
