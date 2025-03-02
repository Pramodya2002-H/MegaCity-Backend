package com.System.MegaCity.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Admin;
import com.System.MegaCity.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean isEmailTaken(String email) {
        return adminRepository.existsByEmail(email);
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin getAdminById(String adminId) {
        return adminRepository.findById(adminId).orElse(null);
    }

    @Override
    public ResponseEntity<?> createAdmin(Admin admin) {

        if (isEmailTaken(admin.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Email already exists: " + admin.getEmail());
        }
        String encodedPassword = passwordEncoder.encode(admin.getPassword());
        admin.setPassword(encodedPassword);
        return ResponseEntity.ok(adminRepository.save(admin));
    }

    /*
     * @Override
     * public Admin updateAdmin(String adminId, Admin admin) {
     * if (adminRepository.existsById(adminId)) {
     * admin.setId(adminId);
     * return adminRepository.save(admin);
     * } else {
     * throw new RuntimeException("Admin not found for id: " + adminId);
     * }
     * }
     * 
     * @Override
     * public void deleteAdmin(String adminId) {
     * 
     * adminRepository.deleteById(adminId);
     * }
     */

}
