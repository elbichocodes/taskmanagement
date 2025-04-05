package com.taskmanager.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
}