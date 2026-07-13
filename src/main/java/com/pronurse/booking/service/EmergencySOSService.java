package com.pronurse.booking.service;

import com.pronurse.booking.dto.EmergencySOSRequest;

public interface EmergencySOSService {
    
    /**
     * Creates an emergency SOS booking with priority dispatch to 5 nearest nurses
     */
    String createEmergencyBooking(String patientMobile, EmergencySOSRequest request);
    
    /**
     * Triggers priority dispatch to multiple nurses simultaneously
     */
    void triggerEmergencyDispatch(Long bookingId);
}


