package com.pronurse.common.sms;

public interface SmsSender {
    void sendOtp(String mobile, String otp);
}