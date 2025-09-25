package io.ai.agent.service;

import io.ai.agent.entity.Airport;
import io.ai.agent.entity.WeatherData;
import io.ai.agent.record.WeatherRecord;
import io.ai.agent.repo.AirportRepository;
import io.ai.agent.repo.WeatherDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

	private final WeatherDataRepository weatherDataRepository;
	private final AirportRepository airportRepository;
	private final RestTemplate restTemplate;

	@Tool(name = "weather-forecast", description = "Get the weather forecast for a given airport code and date")
	@McpTool(name = "weather-forecast", description = "Get the weather forecast for a given airport code and date")
	public WeatherData getWeatherForecast(@McpToolParam(description = "input the airport code") String airportCode, @McpToolParam(description = "current datetime") LocalDateTime dateTime) {
		List<WeatherData> weatherData = weatherDataRepository.findByAirportAndTimeRange(
				airportCode,
				dateTime.minusHours(1),
				dateTime.plusHours(1)
		);

		if (!weatherData.isEmpty()) {
			return weatherData.getFirst();
		}

		return fetchAndSaveExternalWeatherData(airportCode, dateTime);
	}

	private WeatherData fetchAndSaveExternalWeatherData(String airportCode, LocalDateTime dateTime) {
		Airport airport = airportRepository.findByCode(airportCode)
				.orElseThrow(() -> new IllegalArgumentException("Invalid airport code: " + airportCode));

		String url = "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m,weather_code,visibility&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,visibility";
		var response = restTemplate.getForObject(String.format(url, airport.getLatitude(), airport.getLongitude()), WeatherRecord.class);

		if (response != null) {
			WeatherData newWeatherData = WeatherData.builder()
					.airport(airport)
					.dateTime(dateTime)
					.temperature(response.current().temperature2m())
					.windSpeed(response.current().windSpeed10m())
					.visibility(response.current().visibility())
					.conditions(String.valueOf(response.current().conditions()))
					.severeWeather(response.current().severeWeather())
					.windDirection(response.current().windDirection())
					.build();
			return weatherDataRepository.save(newWeatherData);
		}
		return WeatherData.builder().build();
	}
}
