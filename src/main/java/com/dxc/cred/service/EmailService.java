package com.dxc.cred.service;

public interface EmailService {
	public String sendMail(String from, String to, String subject, String msg);
}
