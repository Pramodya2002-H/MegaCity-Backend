package com.System.MegaCity.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.System.MegaCity.model.Admin;
import com.System.MegaCity.repository.AdminRepository;

@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminRepository adminRepository;

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin getAdminById(String adminId) {
        return adminRepository.findById(adminId).orElse(null);
    }

    @Override
    public Admin createAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    /*@Override
    public Admin updateAdmin(String adminId, Admin admin) {
        if (adminRepository.existsById(adminId)) {
            admin.setId(adminId);
            return adminRepository.save(admin);
        } else {
            throw new RuntimeException("Admin not found for id: " + adminId);
        }
    }

    @Override
    public void deleteAdmin(String adminId) {

        adminRepository.deleteById(adminId);
    }*/

}
