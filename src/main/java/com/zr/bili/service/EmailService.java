package com.zr.bili.service;

public interface EmailService {

     boolean sendCodeEmail(String toEmail,String code);

     boolean cacheCode(String email,String code);

     boolean verifyCode(String email,String code);


}
