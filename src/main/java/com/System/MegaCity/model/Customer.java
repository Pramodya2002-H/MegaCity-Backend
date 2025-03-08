package com.System.MegaCity.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "customers")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Customer {
    @Id
    private String customerId;

    private String customerName;

    private String address;

    private String phoneNo;

    private String email;

    private String password;

    private String role = "CUSTOMER";

}