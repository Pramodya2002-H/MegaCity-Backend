package com.System.MegaCity.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.System.MegaCity.model.Customer;
import com.System.MegaCity.service.CustomerService;

import io.jsonwebtoken.io.IOException;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/auth/customer")

public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/getallCustomers")
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/getcustomer/{customerId}")
    public Customer getCustomerById(@PathVariable String customerId) {
        return customerService.getCustomerById(customerId);
    }

    
    @PostMapping("/createcustomer")
    public ResponseEntity<?> createCustomer(@RequestParam("customerName") String customerName,
                                            @RequestParam("customerEmail") String customerEmail,
                                            @RequestParam("address") String address,
                                            @RequestParam("phoneNo") String phoneNo,
                                            @RequestParam("password") String password) {
        try {

            Customer customer = new Customer();
            customer.setCustomerName(customerName);
            customer.setEmail(customerEmail);
            customer.setAddress(address);
            customer.setPhoneNo(phoneNo);
            customer.setPassword(password);

            return customerService.createCustomer(customer);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error uploading image: " + e.getMessage());
        }
    
    }
}