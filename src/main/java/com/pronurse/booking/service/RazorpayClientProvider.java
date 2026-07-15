package com.pronurse.booking.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.stereotype.Component;

@Component
public class RazorpayClientProvider {

    public RazorpayClient getClient(String key, String secret) throws RazorpayException {
        return new RazorpayClient(key, secret);
    }
}
