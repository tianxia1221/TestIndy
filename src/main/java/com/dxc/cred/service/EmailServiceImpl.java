package com.dxc.cred.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender sender;
    public String sendMail(String from, String to, String subject, String msg) {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        try {
        	helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(msg);
        } catch (MessagingException e) {
            e.printStackTrace();
            return "Error while sending mail ..";
        }
        sender.send(message);
        return "Mail Sent Successfully!";
    }

}
