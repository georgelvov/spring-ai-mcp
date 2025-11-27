package com.glvov.springaimcpserver.functional;

import com.glvov.springaimcpserver.model.OpenMeteoResponse;
import com.glvov.springaimcpserver.model.WeatherInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Gateway component for interacting with the OpenMeteo API to fetch weather data.
 */
@Component
@Slf4j
public class OpenMeteoGateway {

    private static final String OPEN_METEO_FORECAST_URL = "https://api.open-meteo.com/v1/forecast";


    public WeatherInfo getWeather(double latitude, double longitude) {
        String uri = UriComponentsBuilder.fromUriString(OPEN_METEO_FORECAST_URL)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m")
                .toUriString();

        log.info("Requesting OpenMeteo forecast for location: {}", uri);

        OpenMeteoResponse response = RestClient.create()
                .get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoResponse.class);

        log.info("Response from OpenMeteo: {}", response);

        return new WeatherInfo(latitude, longitude, response.current().temperature_2m());
    }
}
