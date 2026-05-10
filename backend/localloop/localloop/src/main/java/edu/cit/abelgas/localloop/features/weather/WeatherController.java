package edu.cit.abelgas.localloop.features.weather;

import edu.cit.abelgas.localloop.shared.dto.ApiResponse;
import edu.cit.abelgas.localloop.features.auth.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * GET /api/weather
     * Returns live weather for the authenticated user's barangay.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @AuthenticationPrincipal User user) {

        WeatherResponse data = weatherService.getWeather(user.getBarangay());
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}