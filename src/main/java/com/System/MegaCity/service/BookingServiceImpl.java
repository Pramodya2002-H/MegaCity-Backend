package com.System.MegaCity.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.System.MegaCity.DTO.BookingRequest;
import com.System.MegaCity.DTO.CancellationRequest;
import com.System.MegaCity.exception.InvalidBookingException;
import com.System.MegaCity.exception.InvalidBookingStateException;
import com.System.MegaCity.exception.ResourceNotFoundException;
import com.System.MegaCity.exception.UnauthorizedException;
import com.System.MegaCity.model.Booking;
import com.System.MegaCity.model.BookingStatus;
import com.System.MegaCity.model.Car;
import com.System.MegaCity.model.Customer;
import com.System.MegaCity.model.Driver;
import com.System.MegaCity.repository.BookingRepository;
import com.System.MegaCity.repository.CarRepository;
import com.System.MegaCity.repository.CustomerRepository;
import com.System.MegaCity.repository.DriverRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DriverService driverService;

    private static final int CANCELLATION_WINDOW_HOURS = 24;
    private static final double CANCELLATION_FEE_PERCENTAGE = 0.1;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    @Override
    @Transactional
    public Booking createBooking(BookingRequest request) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

        // Check car availability based on pickup date and time
        if (!isCarAvailableForTime(car, request.getPickupDate(), request.getPickupTime())) {
            throw new InvalidBookingException("Car is not available for requested time");
        }

        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        String currentDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Check if the booking time has already passed
        boolean isPickupTimePassed = false;
        try {
            LocalDate bookingDate = LocalDate.parse(request.getPickupDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime bookingTime = LocalTime.parse(request.getPickupTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate currentDate = LocalDate.parse(currentDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime currentTime = LocalTime.parse(currentTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            isPickupTimePassed = bookingDate.isBefore(currentDate) ||
                    (bookingDate.isEqual(currentDate) && bookingTime.isBefore(currentTime));
        } catch (Exception e) {
            log.error("Error parsing dates for booking time check: {}", e.getMessage());
        }

        // Create booking
        Booking booking = new Booking();
        booking.setCustomerId(request.getCustomerId());
        booking.setCarId(request.getCarId());
        booking.setBookingId(request.getBookingId());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDestination(request.getDestination());
        booking.setPickupDate(request.getPickupDate());
        booking.setPickupTime(request.getPickupTime());
        booking.setBookingDate(LocalDateTime.now().format(DATE_FORMATTER));
        booking.setDriverRequired(request.isDriverRequired());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(calculateBookingAmount(car, request));

        if (request.isDriverRequired()) {
            assignDriverToBooking(booking, car);
        }

        // Mark car as unavailable if pickup time has already passed
        if (isPickupTimePassed) {
            car.setAvailable(false);
            booking.setStatus(BookingStatus.IN_PROGRESS);
            log.info("Booking created with a past pickup time. Car {} marked as unavailable.", car.getCarId());
        }

        carRepository.save(car);
        Booking savedBooking = bookingRepository.save(booking);

        if (booking.isDriverRequired()) {
            Driver driver = driverService.getDriverById(booking.getDriverId());
            savedBooking.setDriverDetails(driver);
        }

        // Send email confirmation to customer
        sendBookingConfirmationEmail(savedBooking);

        log.info("Created new booking with ID: {} for customer: {}",
                booking.getBookingId(), booking.getCustomerId());
        return savedBooking;
    }

    @Override
    public Booking confirmBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking can only be confirmed from PENDING status.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        // Send confirmation email when booking is confirmed
        sendBookingStatusUpdateEmail(confirmedBooking, "Booking Confirmed");

        return confirmedBooking;
    }

    @Override
    public void deleteBooking(String customerId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("Not authorized to delete this booking");
        }

        if (!booking.canBeDeleted()) {
            throw new InvalidBookingStateException("Booking cannot be deleted in current state");
        }

        releaseBookingResource(booking);
        bookingRepository.delete(booking);
        log.info("Deleted booking with ID: {} for customer: {}", bookingId, customerId);
    }

    public boolean hasBookingWithDriver(String customerEmail, String driverId) {
        return bookingRepository.existsByCustomerEmailAndDriverId(customerEmail, driverId);
    }

    private boolean isCarAvailableForTime(Car car, String requestedDate, String requestedTime) {
        if (!car.isAvailable()) {
            return false;
        }

        List<Booking> existingBookings = bookingRepository.findByCarIdAndStatus(
                car.getCarId(),
                BookingStatus.CONFIRMED);

        return existingBookings.stream()
                .noneMatch(booking -> isTimeOverlapping(booking.getPickupDate(), booking.getPickupTime(),
                        requestedDate, requestedTime));
    }

    private boolean isTimeOverlapping(String existingDate, String existingTime,
            String requestedDate, String requestedTime) {
        try {
            LocalDate existingDateObj = LocalDate.parse(existingDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime existingTimeObj = LocalTime.parse(existingTime, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDate requestedDateObj = LocalDate.parse(requestedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime requestedTimeObj = LocalTime.parse(requestedTime, DateTimeFormatter.ofPattern("HH:mm"));

            // If dates are different, no overlap
            if (!existingDateObj.isEqual(requestedDateObj)) {
                return false;
            }

            // If dates are the same, check if times are within 1 hour of each other
            LocalDateTime existingDateTime = LocalDateTime.of(existingDateObj, existingTimeObj);
            LocalDateTime requestedDateTime = LocalDateTime.of(requestedDateObj, requestedTimeObj);

            Duration buffer = Duration.ofHours(1);
            return Math.abs(Duration.between(existingDateTime, requestedDateTime).toHours()) < buffer.toHours();
        } catch (Exception e) {
            log.error("Error parsing dates for time overlap check: {}", e.getMessage());
            return false;
        }
    }

    private double calculateBookingAmount(Car car, BookingRequest request) {
        double baseAmount = car.getBaseRate();
        if (request.isDriverRequired()) {
            baseAmount += car.getDriverRate();
        }
        return baseAmount;
    }

    private void assignDriverToBooking(Booking booking, Car car) {
        Driver driver;
        if (car.getAssignedDriverId() != null) {
            driver = driverRepository.findById(car.getAssignedDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned driver not found"));
            if (!driver.isAvailable()) {
                throw new InvalidBookingException("Car's assigned driver is not available");
            }
        } else {
            driver = driverRepository.findFirstByAvailableAndHasOwnCarFalse(true)
                    .orElseThrow(() -> new ResourceNotFoundException("No available driver"));
        }

        booking.setDriverId(driver.getDriverId());
        driver.setAvailable(false);
        driverRepository.save(driver);
        log.info("Assigned driver {} to booking {}", driver.getDriverId(), booking.getBookingId());
    }

    @Override
    @Transactional
    public Booking cancelBooking(String customerId, CancellationRequest request) {
        log.info("Cancelling booking with ID: {} for customer: {}", request.getBookingId(), customerId);

        if (request.getBookingId() == null || request.getBookingId().isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be null or empty");
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> {
                    log.error("Booking not found with ID: {}", request.getBookingId());
                    return new ResourceNotFoundException("Booking not found or already deleted");
                });

        if (!booking.getCustomerId().equals(customerId)) {
            log.warn("Unauthorized cancellation attempt for booking: {} by customer: {}", request.getBookingId(),
                    customerId);
            throw new UnauthorizedException("Not authorized to cancel this booking");
        }

        if (!booking.canBeCancelled()) {
            log.warn("Invalid cancellation attempt for booking: {} in state: {}", request.getBookingId(),
                    booking.getStatus());
            throw new InvalidBookingStateException("Booking cannot be cancelled in current state");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getReason());
        booking.setCancellationTime(LocalDateTime.now().format(DATE_FORMATTER));

        releaseBookingResource(booking);
        handleCancellationRefund(booking);

        bookingRepository.save(booking);

        // Send cancellation email
        sendBookingStatusUpdateEmail(booking, "Booking Cancelled");

        log.info("Successfully cancelled booking with ID: {} for customer: {}", booking.getBookingId(),
                booking.getCustomerId());
        return booking;
    }

    private void releaseBookingResource(Booking booking) {
        if (booking.getCarId() != null) {
            Car car = carRepository.findById(booking.getCarId()).orElse(null);
            if (car != null && !car.isAvailable()) {
                car.setAvailable(true);
                carRepository.save(car);
                log.info("Released car {} from booking {}", car.getCarId(), booking.getBookingId());
            }
        }

        if (booking.getDriverId() != null) {
            Driver driver = driverRepository.findById(booking.getDriverId()).orElse(null);
            if (driver != null && !driver.isAvailable()) {
                driver.setAvailable(true);
                driverRepository.save(driver);
                log.info("Released driver {} from booking {}", driver.getDriverId(), booking.getBookingId());
            }
        }
    }

    private void handleCancellationRefund(Booking booking) {
        LocalDateTime pickupDateTime = parsePickupDate(booking.getPickupDate());
        LocalDateTime cancellationDeadline = pickupDateTime.minusHours(CANCELLATION_WINDOW_HOURS);
        if (LocalDateTime.now().isBefore(cancellationDeadline)) {
            booking.setRefundAmount(booking.getTotalAmount());
        } else {
            double cancellationFee = booking.getTotalAmount() * CANCELLATION_FEE_PERCENTAGE;
            booking.setRefundAmount(booking.getTotalAmount() - cancellationFee);
        }
        log.info("Processing refund of {} for booking {}", booking.getRefundAmount(), booking.getBookingId());
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndUpdateCarAvailability() {
        ZoneId sriLankaZoneId = ZoneId.of("Asia/Colombo");
        LocalDateTime now = LocalDateTime.now(sriLankaZoneId);

        // Format current date and time as strings
        String currentDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Get bookings with passed pickup time using the new repository method
        List<Booking> passedBookings = bookingRepository.findBookingsWithPassedPickupTime(
                List.of(BookingStatus.PENDING.toString(), BookingStatus.CONFIRMED.toString()),
                currentDateStr,
                currentTimeStr);

        for (Booking booking : passedBookings) {
            String carId = booking.getCarId();
            Optional<Car> car = carRepository.findById(carId);
            car.ifPresent(c -> {
                c.setAvailable(false);
                carRepository.save(c);

            });

            updateBookingStatus(booking);
        }

    }

    private void updateBookingStatus(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);
            log.info("Updated booking {} status to IN_PROGRESS", booking.getBookingId());
        }
    }

    private LocalDateTime parsePickupDate(String pickupDate) {
        try {
            return LocalDateTime.parse(pickupDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                LocalDate date = LocalDate.parse(pickupDate, DATE_ONLY_FORMATTER);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format: " + pickupDate, e2);
            }
        }
    }

    private void sendBookingConfirmationEmail(Booking booking) {
        try {
            Customer customer = customerRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            Car car = carRepository.findById(booking.getCarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

            String subject = "MegaCityCab - Booking Confirmation #" + booking.getBookingId();
            String emailBody = generateBookingEmailBody(booking, customer, car);

            emailService.sendHtmlEmail(customer.getEmail(), subject, emailBody);
            log.info("Booking confirmation email sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email: {}", e.getMessage(), e);
        }
    }

    private void sendBookingStatusUpdateEmail(Booking booking, String statusMessage) {
        try {
            Customer customer = customerRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            String subject = "MegaCityCab - " + statusMessage + " #" + booking.getBookingId();
            StringBuilder emailBody = new StringBuilder();

            emailBody.append("Dear ").append(customer.getCustomerName()).append(",\n\n");
            emailBody.append("Your booking with ID: ").append(booking.getBookingId()).append(" has been ")
                    .append(statusMessage.toLowerCase()).append(".\n\n");

            if (booking.getStatus() == BookingStatus.CANCELLED && booking.getRefundAmount() > 0) {
                emailBody.append("A refund of Rs").append(booking.getRefundAmount())
                        .append(" will be processed to your original payment method.\n\n");
            }

            emailBody.append("If you have any questions, please contact our customer service team.\n");
            emailBody.append("Phone: +94 74 123 4567\n");
            emailBody.append("Email: support@megacitycab.com\n\n");

            emailBody.append("Thank you for choosing MegaCityCab!\n");

            emailService.sendHtmlEmail(customer.getEmail(), subject, emailBody.toString());
            log.info("Booking status update email sent to customer: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send booking status update email: {}", e.getMessage(), e);
        }
    }

    private String generateBookingEmailBody(Booking booking, Customer customer, Car car) {
        StringBuilder emailBody = new StringBuilder();

        emailBody.append("<html>")
                .append("<head>")
                .append("<style>")
                .append("body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f7fa; color: #333333; }")
                .append(".container { max-width: 700px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }")
                .append(".header { background: linear-gradient(135deg,hsl(54, 89.20%, 47.30%) 0%,rgb(240, 225, 13) 100%); padding: 30px; text-align: center; color: white; }")
                .append(".header img { max-width: 180px; height: auto; }")
                .append(".header h1 { margin: 15px 0 0; font-size: 28px; font-weight: 300; }")
                .append(".content { padding: 30px; }")
                .append(".section { margin-bottom: 25px; }")
                .append(".section h2 { color:rgb(238, 216, 14); font-size: 20px; border-bottom: 2px solidrgb(235, 219, 7); padding-bottom: 8px; margin-bottom: 15px; }")
                .append(".info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }")
                .append(".info-row:last-child { border-bottom: none; }")
                .append(".label { font-weight: 600; color: #555; }")
                .append(".value { color: #333; }")
                .append(".invoice { background-color: #f8fafc; padding: 20px; border-radius: 6px; }")
                .append(".invoice table { width: 100%; border-collapse: collapse; }")
                .append(".invoice th { background-color:rgb(235, 219, 7); color: white; padding: 12px; text-align: left; }")
                .append(".invoice td { padding: 12px; }")
                .append(".total { font-weight: 700; background-color: #eef2f7; }")
                .append(".footer { text-align: center; padding: 20px; background-color: #f8fafc; font-size: 13px; color: #777; }")
                .append(".contact-info { margin: 15px 0; line-height: 1.6; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='container'>")
                .append("<div class='header'>")
                .append("<img src='https://www.google.com/search?sca_esv=63fe5d3f66683875&sxsrf=AHTn8zoN0PeL5V8yBMIWn6vRmmt7kUQrhQ:1742915476537&q=cab+service+logo&udm=2&fbs=ABzOT_CWdhQLP1FcmU5B0fn3xuWpA-dk4wpBWOGsoR7DG5zJBpcx8kZB4NRoUjdgt8WwoMs7jebc2P25mD9bLva5PWN4OYIEwevXrfOzU4-NF7lrn5uJPZD1tGLHG9gmQlyHQsaXvrrrWftYES6Ib7itOegsItcoD57bhCnDZxzL3cpdD91ghjQ6GC2qHUaYSYynF3NTJM6yHR9uzC1jfoYKnnWt5Vvnjw&sa=X&ved=2ahUKEwjb2eiOwqWMAxV4dvUHHSv-FscQtKgLegQIERAB&biw=1366&bih=607#vhid=xq54DBzk7u1NqM&vssid=mosaic' alt='MegaCityCab Logo' />")
                .append("<h1>Booking Confirmation</h1>")
                .append("</div>")
                .append("<div class='content'>")
                .append("<p>Dear ").append(customer.getCustomerName()).append(",</p>")
                .append("<p>Thank you for choosing MegaCityCab. Your booking has been confirmed with the following details:</p>")
                .append("<div class='section'>")
                .append("<h2>Booking Details</h2>")
                .append("<div class='info-row'><span class='label'>Booking ID:</span><span class='value'>")
                .append(booking.getBookingId()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Booking Date:</span><span class='value'>")
                .append(booking.getBookingDate()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Pickup Location:</span><span class='value'>")
                .append(booking.getPickupLocation()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Destination:</span><span class='value'>")
                .append(booking.getDestination()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Pickup Date:</span><span class='value'>")
                .append(booking.getPickupDate()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Pickup Time:</span><span class='value'>")
                .append(booking.getPickupTime()).append("</span></div>")
                .append("</div>")
                .append("<div class='section'>")
                .append("<h2>Vehicle Details</h2>")
                .append("<div class='info-row'><span class='label'>Car Model:</span><span class='value'>")
                .append(car.getModel()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>License Plate:</span><span class='value'>")
                .append(car.getLicensePlate()).append("</span></div>")
                .append("<div class='info-row'><span class='label'>Driver Required:</span><span class='value'>")
                .append(booking.isDriverRequired() ? "Yes" : "No").append("</span></div>")
                .append("</div>")
                .append("<div class='section'>")
                .append("<div class='invoice'>")
                .append("<h2>Payment Details</h2>")
                .append("<table>")
                .append("<tr><th>Description</th><th>Amount</th></tr>")
                .append("<tr><td>Base Rate</td><td>Rs.2000 ").append(car.getBaseRate()).append("</td></tr>");
        if (booking.isDriverRequired()) {
            emailBody.append("<tr><td>Driver Rate</td><td>Rs. 500").append(car.getDriverRate()).append("</td></tr>");
        }
        emailBody.append("<tr class='total'><td>Total Amount</td><td>Rs. ").append(booking.getTotalAmount())
                .append("</td></tr>")
                .append("</table>")
                .append("</div>")
                .append("</div>")
                .append("<div class='section'>")
                .append("<h2>Cancellation Policy</h2>")
                .append("<p>- Cancellations made more than 24 hours before the pickup time will receive a full refund.</p>")
                .append("<p>- Cancellations made within 24 hours of the pickup time will incur a 10% cancellation fee.</p>")
                .append("</div>")
                .append("<div class='contact-info'>")
                .append("<p>If you have any questions or need to make changes to your booking, please contact our customer service team:</p>")
                .append("<p><strong>Phone:</strong> +94 11 123 4567</p>")
                .append("<p><strong>Email:</strong> support@megacitycab.com</p>")
                .append("</div>")
                .append("</div>")
                .append("<div class='footer'>")
                .append("<p>Thank you for choosing MegaCityCab!</p>")
                .append("<p>© 2025 MegaCityCab. All rights reserved.</p>")
                .append("</div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return emailBody.toString();
    }

    @Transactional(readOnly = true)
    public List<Booking> getCustomerBookings(String customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Booking getBookingDetails(String customerId, String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("Not authorized to view this booking");
        }

        return booking;
    }

    @Override
    public List<Booking> getAvailableBookings() {
        // Return bookings that are PENDING and have no assigned driver
        return bookingRepository.findByDriverIdIsNullAndStatus(BookingStatus.PENDING);
    }
}