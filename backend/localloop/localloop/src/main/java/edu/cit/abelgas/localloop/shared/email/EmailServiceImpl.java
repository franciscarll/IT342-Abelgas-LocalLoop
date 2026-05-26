package edu.cit.abelgas.localloop.shared.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async  // ← runs in a background thread; never blocks the request
    public void sendWelcomeEmail(String toName, String toEmail) {
        log.info(">>> [ASYNC] Sending welcome email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to LocalLoop, " + toName + "! 🌿");
            helper.setText(WelcomeEmailTemplate.build(toName), true);

            mailSender.send(message);
            log.info(">>> [ASYNC] Welcome email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error(">>> [ASYNC] Failed to build welcome email for {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error(">>> [ASYNC] Unexpected error sending welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}