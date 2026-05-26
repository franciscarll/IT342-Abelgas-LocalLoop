package edu.cit.abelgas.localloop.shared.email;

public interface EmailService {

    /**
     * Sends a welcome email to a newly registered user.
     *
     * @param toName  the user's display name
     * @param toEmail the user's email address
     */
    void sendWelcomeEmail(String toName, String toEmail);
}