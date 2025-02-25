package com.System.MegaCity.DTO;

import lombok.Data;

@Data
public class CancellationRequest {
   private String bookingId;
   private String reason; 
}
