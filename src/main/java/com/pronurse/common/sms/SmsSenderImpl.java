package com.pronurse.common.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("prod")
public class SmsSenderImpl implements SmsSender {

    @Override
    public void sendOtp(String mobile, String otp) {
        log.info("Sending real SMS OTP token via production vendor gateway network...");
        // TODO: Insert your Third-Party SMS Gateway API Integration (Twilio, Fast2SMS, etc.) here
    }
}