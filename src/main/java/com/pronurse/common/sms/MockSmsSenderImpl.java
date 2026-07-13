package com.pronurse.common.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "test"}) // Active for local development and testing
public class MockSmsSenderImpl implements SmsSender {

    @Override
    public void sendOtp(String mobile, String otp) {
        log.info("==========================================================");
        log.info("   [MOCK SMS GATEWAY] Outbound Transaction Initiated      ");
        log.info("   Target Mobile : {}", mobile);
        log.info("   Generated OTP : {}", otp);
        log.info("   Message       : Your Pro-Nurse verification code is: {}", otp);
        log.info("==========================================================");
    }
}