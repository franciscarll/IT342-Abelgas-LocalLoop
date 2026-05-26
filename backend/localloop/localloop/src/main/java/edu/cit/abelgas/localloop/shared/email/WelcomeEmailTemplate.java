package edu.cit.abelgas.localloop.shared.email;

public class WelcomeEmailTemplate {

    /**
     * Builds the HTML body for the welcome email.
     * Kept separate from EmailServiceImpl so HTML changes
     * never require touching sending logic.
     */
    public static String build(String name) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Welcome to LocalLoop</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;
                                  box-shadow:0 2px 8px rgba(0,0,0,0.08);
                                  overflow:hidden;max-width:600px;width:100%%;">

                      <!-- Header -->
                      <tr>
                        <td style="background:#16a34a;padding:36px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:28px;
                                     font-weight:700;letter-spacing:0.5px;">
                            🌿 LocalLoop
                          </h1>
                          <p style="margin:8px 0 0;color:#bbf7d0;font-size:14px;">
                            Your barangay community platform
                          </p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 40px 24px;">
                          <h2 style="margin:0 0 12px;color:#111827;font-size:22px;font-weight:600;">
                            Maligayang pagdating, %s! 👋
                          </h2>
                          <p style="margin:0 0 20px;color:#4b5563;font-size:15px;line-height:1.7;">
                            You've successfully joined <strong>LocalLoop</strong> — a community platform
                            built for your barangay. Here's what you can do to get started:
                          </p>

                          <!-- Feature list -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#f0fdf4;border-radius:8px;
                                        border:1px solid #bbf7d0;margin-bottom:28px;">
                            <tr>
                              <td style="padding:20px 24px;">
                                <p style="margin:0 0 10px;color:#15803d;font-weight:600;font-size:14px;">
                                  WHAT YOU CAN DO
                                </p>
                                <ul style="margin:0;padding-left:20px;color:#374151;
                                           font-size:14px;line-height:2;">
                                  <li>📋 Post and claim <strong>favors</strong> in your barangay</li>
                                  <li>📢 View <strong>announcements</strong> from your community</li>
                                  <li>⭐ Build your <strong>reputation score</strong> by helping neighbors</li>
                                  <li>👤 Manage your <strong>profile</strong> and barangay info</li>
                                </ul>
                              </td>
                            </tr>
                          </table>

                          <!-- CTA Button -->
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center" style="padding-bottom:28px;">
                                <a href="http://localhost:3000"
                                   style="display:inline-block;background:#16a34a;color:#ffffff;
                                          text-decoration:none;font-weight:600;font-size:15px;
                                          padding:14px 36px;border-radius:8px;
                                          letter-spacing:0.3px;">
                                  Go to LocalLoop →
                                </a>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#6b7280;font-size:13px;line-height:1.6;">
                            If you didn't create this account, you can safely ignore this email.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f9fafb;border-top:1px solid #e5e7eb;
                                   padding:20px 40px;text-align:center;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;">
                            © 2026 LocalLoop · Built for barangay communities 🇵🇭
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(name);
    }
}