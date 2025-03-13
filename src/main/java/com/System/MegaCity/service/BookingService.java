package com.System.MegaCity.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.System.MegaCity.DTO.BookingRequest;
import com.System.MegaCity.DTO.CancellationRequest;
import com.System.MegaCity.model.Booking;

@Service
public interface BookingService {
    List<Booking> getAllBookings();
    Booking getBookingById(String bookingId);
    Booking createBooking(BookingRequest request);
    Booking cancelBooking(String customerId, CancellationRequest request);
    List<Booking> getCustomerBookings(String customerId);
    Booking getBookingDetails(String customerId, String bookingId);
    void deleteBooking(String customerId, String bookingId);
}