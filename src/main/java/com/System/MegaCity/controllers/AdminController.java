package com.System.MegaCity.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.System.MegaCity.model.Admin;
import com.System.MegaCity.service.AdminService;

@RestController
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/admin")
    public List<Admin> getAllAdmins() {
        return adminService.getAllAdmins();
    }

    @GetMapping("/viewAdmin/{adminId}")
    public ResponseEntity<Admin> getAdminById(@PathVariable String adminId) {
        Admin admin = adminService.getAdminById(adminId);
        return ResponseEntity.ok(admin);
    }

    @PostMapping("/createAdmin")
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        Admin createAdmin = adminService.createAdmin(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(createAdmin);
    }

    /*
     * @PutMapping("/updateAdmin/{adminId}")
     * public ResponseEntity<Admin> upadateAdmin(@PathVariable String id, @RequestBody Admin admin) {
     * User updatedUser = userService.updateUser(id, user);
     * return ResponseEntity.ok(updatedAdmin);
     * }
     * 
     * @DeleteMapping("/{adminId}")
     * public ResponseEntity<String> deleteAdmin(@PathVariable String adminId) {
     * adminService.deleteAdmin(adminId);
     * return ResponseEntity.ok("Admin deleted successfully");
     * }
     */
}
