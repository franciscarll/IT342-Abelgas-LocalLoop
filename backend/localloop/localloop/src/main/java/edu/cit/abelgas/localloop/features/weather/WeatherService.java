package edu.cit.abelgas.localloop.service;

import edu.cit.abelgas.localloop.dto.response.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // OpenWeatherMap free-tier endpoint
    private static final String OWM_URL = "https://api.openweathermap.org/data/2.5/weather";

    @SuppressWarnings("unchecked")
    public WeatherResponse getWeather(String barangay) {
        // If no API key configured, return a placeholder
        if (apiKey == null || apiKey.isBlank()) {
            return WeatherResponse.builder()
                    .barangay(barangay)
                    .temperature(30)
                    .feelsLike(33)
                    .humidity(72)
                    .windSpeed(12)
                    .condition("Partly Cloudy")
                    .description("partly cloudy")
                    .build();
        }

        try {
            // Append "Philippines" to improve geocoding accuracy
            String query = barangay + ", Philippines";
            String url = UriComponentsBuilder.fromHttpUrl(OWM_URL)
                    .queryParam("q", query)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .toUriString();

            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) throw new RuntimeException("Empty response from weather API");

            Map<String, Object> main = (Map<String, Object>) resp.get("main");
            Map<String, Object> wind = (Map<String, Object>) resp.get("wind");
            java.util.List<Map<String, Object>> weatherList =
                    (java.util.List<Map<String, Object>>) resp.get("weather");

            double temp      = toDouble(main.get("temp"));
            double feelsLike = toDouble(main.get("feels_like"));
            int    humidity  = ((Number) main.get("humidity")).intValue();
            double windSpeed = toDouble(wind.get("speed"));

            String condition   = "";
            String description = "";
            if (weatherList != null && !weatherList.isEmpty()) {
                condition   = capitalize((String) weatherList.get(0).get("main"));
                description = capitalize((String) weatherList.get(0).get("description"));
            }

            return WeatherResponse.builder()
                    .barangay(barangay)
                    .temperature(temp)
                    .feelsLike(feelsLike)
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .condition(condition)
                    .description(description)
                    .build();

        } catch (Exception e) {
            // Graceful fallback — never crash the dashboard over weather
            return WeatherResponse.builder()
                    .barangay(barangay)
                    .temperature(0)
                    .feelsLike(0)
                    .humidity(0)
                    .windSpeed(0)
                    .condition("Unavailable")
                    .description("Weather data unavailable")
                    .build();
        }
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}