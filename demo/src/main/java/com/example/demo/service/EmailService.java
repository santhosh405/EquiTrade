package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.model.User;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${admin.notify.email:admin@equitrade.com}")
    private String adminNotifyEmail;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    public void sendAdminApprovalRequestEmail(User applicant, String approveToken, String declineToken) {
        String subject = "New User Access Request: " + applicant.getUsername() + " (" + applicant.getEmail() + ")";
        String approveUrl = baseUrl + "/admin/access-requests.html?token=" + approveToken + "&action=APPROVE";
        String declineUrl = baseUrl + "/admin/access-requests.html?token=" + declineToken + "&action=DECLINE";

        String textContent = String.format("""
            EquiTrade Admin Console - New User Access Request
            --------------------------------------------------
            Applicant Details:
            Name: %s
            Email: %s
            Phone: %s
            Registration Time: %s

            Links (Single-use, expiring in 48 hours. Requires Admin Login):
            APPROVE: %s
            DECLINE: %s

            You may also approve or decline directly from the Admin Panel:
            %s/admin/access-requests.html
            """,
            applicant.getUsername(),
            applicant.getEmail(),
            applicant.getPhone() != null ? applicant.getPhone() : "Not provided",
            applicant.getCreatedAt(),
            approveUrl,
            declineUrl,
            baseUrl
        );

        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                <h2 style="color: #2563eb; margin-top: 0;">EquiTrade Access Request Notification</h2>
                <p>A new applicant has registered on EquiTrade and is awaiting admin approval.</p>
                <table style="width: 100%%; margin-bottom: 20px; border-collapse: collapse;">
                    <tr><td style="padding: 8px; font-weight: bold; width: 140px;">Applicant Name:</td><td style="padding: 8px;">%s</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">Email Address:</td><td style="padding: 8px;">%s</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">Phone:</td><td style="padding: 8px;">%s</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">Registered At:</td><td style="padding: 8px;">%s</td></tr>
                </table>
                <p>Click below to approve or decline this request (Requires admin credentials):</p>
                <div style="margin: 25px 0;">
                    <a href="%s" style="background-color: #16a34a; color: white; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-weight: bold; margin-right: 15px; display: inline-block;">Approve Access</a>
                    <a href="%s" style="background-color: #dc2626; color: white; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Decline Access</a>
                </div>
                <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;" />
                <p style="font-size: 12px; color: #64748b;">Or visit the admin panel directly: <a href="%s/admin/access-requests.html">%s/admin/access-requests.html</a></p>
            </div>
            """,
            applicant.getUsername(),
            applicant.getEmail(),
            applicant.getPhone() != null ? applicant.getPhone() : "Not provided",
            applicant.getCreatedAt(),
            approveUrl,
            declineUrl,
            baseUrl,
            baseUrl
        );

        sendEmail(adminNotifyEmail, subject, textContent, htmlContent);
    }

    public void sendUserApprovalStatusEmail(User user, boolean approved, String reason) {
        String subject = approved
                ? "Welcome to EquiTrade! Your Account Access is Approved"
                : "EquiTrade Account Access Request Update";

        String textContent = approved
                ? String.format("Hello %s,\n\nYour account access request for EquiTrade has been APPROVED by the administrator.\nYou can now login and start trading at: %s/login.html\n\nBest regards,\nEquiTrade Team",
                    user.getUsername(), baseUrl)
                : String.format("Hello %s,\n\nYour account access request for EquiTrade was DECLINED.\nReason: %s\n\nBest regards,\nEquiTrade Team",
                    user.getUsername(), (reason != null && !reason.isBlank() ? reason : "Eligibility criteria not met"));

        String htmlContent = approved
                ? String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                        <h2 style="color: #16a34a; margin-top: 0;">Account Access Approved!</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your EquiTrade account access request has been verified and approved by our administrative team.</p>
                        <p>You can now sign in to access market data, trade simulations, and your portfolio.</p>
                        <div style="margin: 25px 0;">
                            <a href="%s/login.html" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">Login to EquiTrade</a>
                        </div>
                        <p style="font-size: 13px; color: #64748b;">Thank you for choosing EquiTrade.</p>
                    </div>
                    """, user.getUsername(), baseUrl)
                : String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                        <h2 style="color: #dc2626; margin-top: 0;">Access Request Declined</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>We regret to inform you that your request to access the EquiTrade trading platform was not approved.</p>
                        <p><strong>Reason:</strong> %s</p>
                        <p style="font-size: 13px; color: #64748b;">If you believe this is an error, please reach out to our support desk.</p>
                    </div>
                    """, user.getUsername(), (reason != null && !reason.isBlank() ? reason : "Eligibility criteria not met"));

        sendEmail(user.getEmail(), subject, textContent, htmlContent);
    }

    private void sendEmail(String to, String subject, String textContent, String htmlContent) {
        if (mailSender == null) {
            logger.info("[EMAIL SIMULATION] To: {} | Subject: {}\n{}", to, subject, textContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            mailSender.send(message);
            logger.info("Sent email successfully to {}", to);
        } catch (Exception ex) {
            logger.warn("Could not dispatch email via SMTP (using fallback logging): {}. Content:\n{}", ex.getMessage(), textContent);
        }
    }
}
