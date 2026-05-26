package edu.cit.abelgas.localloop.features.auth;

import edu.cit.abelgas.localloop.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/mobile")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    public MobileAuthController(MobileAuthService mobileAuthService) {
        this.mobileAuthService = mobileAuthService;
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleSignIn(
            @RequestBody Map<String, String> body) {

        String idToken = body.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("AUTH-001", "idToken is required", null));
        }

        AuthResponse data = mobileAuthService.googleSignIn(idToken);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}