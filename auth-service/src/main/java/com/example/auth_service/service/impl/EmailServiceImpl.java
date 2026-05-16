package com.example.auth_service.service.impl;

import com.example.auth_service.entity.OtpType;
import com.example.auth_service.exception.EmailDeliveryException;
import com.example.auth_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.brand-name:ResumeAI}")
    private String brandName;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public void sendOtpEmail(String email, String fullName, String otp, OtpType otpType, long expiryMinutes) {
        String subject = otpType == OtpType.VERIFY_EMAIL
                ? brandName + " Email Verification Code"
                : brandName + " OTP Code";
        String heading = otpType == OtpType.VERIFY_EMAIL ? "Verify your email address" : "Your one-time password";
        String body = otpType == OtpType.VERIFY_EMAIL
                ? "Use this code to complete your ResumeAI account verification."
                : "Use this code to continue your request.";
        sendHtmlEmail(email, fullName, subject, buildOtpTemplate(fullName, heading, body, otp, expiryMinutes));
    }

    @Override
    public void sendWelcomeEmail(String email, String fullName) {
        String subject = "Welcome to " + brandName;
        String safeName = sanitizeName(fullName, email);
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>Welcome</title>
                </head>
                <body style="margin:0;padding:0;background:#f6f8fc;font-family:Arial,sans-serif;color:#1a1f4b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e7ecf8;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f1230,#1a1f4b);padding:26px 28px;color:#ffffff;">
                              <h1 style="margin:0;font-size:24px;">Welcome to %s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                              <p style="margin:0 0 12px;font-size:15px;line-height:1.65;">Your account is now verified and ready. You can start building ATS-ready resumes, use AI tools, and track your progress.</p>
                              <p style="margin:0;font-size:15px;line-height:1.65;">If this wasn't you, please contact support immediately.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(brandName), escapeHtml(safeName));
        sendHtmlEmail(email, fullName, subject, html);
    }

    @Override
    public void sendForgotPasswordOtpEmail(String email, String fullName, String otp, long expiryMinutes) {
        String subject = brandName + " Password Reset Code";
        String heading = "Reset your password";
        String body = "Use this OTP to reset your password securely.";
        sendHtmlEmail(email, fullName, subject, buildOtpTemplate(fullName, heading, body, otp, expiryMinutes));
    }

    @Override
    public void sendPasswordResetSuccessEmail(String email, String fullName) {
        String safeName = sanitizeName(fullName, email);
        String subject = brandName + " Password Updated";
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>Password updated</title>
                </head>
                <body style="margin:0;padding:0;background:#f6f8fc;font-family:Arial,sans-serif;color:#1a1f4b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e7ecf8;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f1230,#1a1f4b);padding:26px 28px;color:#ffffff;">
                              <h1 style="margin:0;font-size:24px;">Password changed successfully</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                              <p style="margin:0 0 12px;font-size:15px;line-height:1.65;">Your password has just been updated. You can now login with your new credentials.</p>
                              <p style="margin:0;font-size:15px;line-height:1.65;">If you did not perform this action, reset your password immediately and contact support.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(safeName));
        sendHtmlEmail(email, fullName, subject, html);
    }

    private void sendHtmlEmail(String toEmail, String fullName, String subject, String html) {
        try {
            validateMailConfiguration();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(resolveFromAddress());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent successfully to={} subject={}", toEmail, subject);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send email to={} subject={} error={}", toEmail, subject, ex.getMessage(), ex);
            throw new EmailDeliveryException("Unable to send email. Please try again.", ex);
        }
    }

    private String buildOtpTemplate(String fullName, String heading, String body, String otp, long expiryMinutes) {
        String safeName = sanitizeName(fullName, "there");
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>OTP</title>
                </head>
                <body style="margin:0;padding:0;background:#f6f8fc;font-family:Arial,sans-serif;color:#1a1f4b;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e7ecf8;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f1230,#1a1f4b);padding:26px 28px;color:#ffffff;">
                              <div style="font-size:13px;letter-spacing:0.4px;text-transform:uppercase;color:#f0b429;margin-bottom:6px;">%s security</div>
                              <h1 style="margin:0;font-size:24px;">%s</h1>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                              <p style="margin:0 0 18px;font-size:15px;line-height:1.65;">%s</p>
                              <div style="margin:0 0 16px;padding:16px;border-radius:12px;background:#f2f4fb;border:1px dashed #c3c9e8;text-align:center;">
                                <div style="font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#667085;margin-bottom:8px;">One-time password</div>
                                <div style="font-size:34px;letter-spacing:10px;font-weight:700;color:#0f1230;">%s</div>
                              </div>
                              <p style="margin:0 0 8px;font-size:14px;color:#1f2937;">This OTP expires in <strong>%d minutes</strong>.</p>
                              <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.55;">For your safety, do not share this code with anyone. ResumeAI will never ask for your OTP by phone or chat.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(brandName), escapeHtml(heading), escapeHtml(safeName), escapeHtml(body), escapeHtml(otp), expiryMinutes);
    }

    private String sanitizeName(String fullName, String fallback) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return fallback;
        }
        return fullName.trim();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void validateMailConfiguration() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            throw new EmailDeliveryException(
                    "Email service is not configured. Set MAIL_USERNAME and MAIL_PASSWORD in auth-service.",
                    null
            );
        }
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(fromEmail)) {
            return fromEmail.trim();
        }
        return mailUsername.trim();
    }
}
