package com.System.MegaCity.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Admin;

@Service
public interface AdminService {

    List<Admin> getAllAdmins();

    Admin getAdminById(String adminId);

    ResponseEntity<?> createAdmin(Admin admin);

    // Admin updateAdmin(String adminId, Admin admin);

    // void deleteAdmin(String adminId);
}
