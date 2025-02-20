package com.System.MegaCity.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.System.MegaCity.model.Booking;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByCustomerId(String customerId);
    List<Booking> findByDriverId(String driverId);
    List<Booking> findByStatusAndPickupDateBefore(BookingStatus status , String dateTime);
    List<Booking> findByCarIdAndStatus(String carId, BookingsStatus status);

    @Query("{'carId':?0, 'pickupdate':($gte:?1, $lte:?2),'status':($in:['CONFIRMED','IN_PROCESS'])}")
    List<Booking> findOverlapp
}
