package edu.cit.abelgas.localloop.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherResponse {
    private String barangay;
    private double temperature;
    private double feelsLike;
    private int humidity;
    private double windSpeed;
    private String condition;    // e.g. "Partly Cloudy"
    private String description;  // e.g. "partly cloudy"
    private String icon;         // optional: weather icon code
}