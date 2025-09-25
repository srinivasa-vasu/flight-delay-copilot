package io.ai.agent.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WeatherRecord(
		double latitude,
		double longitude,
		@JsonProperty("generationtime_ms")
		double generationtimeMs,
		@JsonProperty("utc_offset_seconds")
		int utcOffsetSeconds,
		String timezone,
		@JsonProperty("timezone_abbreviation")
		String timezoneAbbreviation,
		double elevation,
		@JsonProperty("current_units")
		CurrentUnits currentUnits,
		CurrentWeather current,
		@JsonProperty("hourly_units")
		HourlyUnits hourlyUnits,
		HourlyWeather hourly
) {
	public record CurrentUnits(
			String time,
			String interval,
			@JsonProperty("temperature_2m")
			String temperature2m,
			@JsonProperty("wind_speed_10m")
			String windSpeed10m,
			@JsonProperty("weather_code")
			String weatherCode,
			String visibility,
			String windDirection,
			String conditions,
			boolean severeWeather
	) { }

	public record CurrentWeather(
			String time,
			int interval,
			@JsonProperty("temperature_2m")
			double temperature2m,
			@JsonProperty("wind_speed_10m")
			double windSpeed10m,
			@JsonProperty("weather_code")
			int weatherCode,
			double visibility,
			String windDirection,
			String conditions,
			boolean severeWeather
	) { }

	public record HourlyUnits(
			String time,
			@JsonProperty("temperature_2m")
			String temperature2m,
			@JsonProperty("relative_humidity_2m")
			String relativeHumidity2m,
			@JsonProperty("wind_speed_10m")
			String windSpeed10m,
			@JsonProperty("weather_code")
			String weatherCode,
			String visibility,
			String windDirection,
			String conditions,
			boolean severeWeather
	) { }

	public record HourlyWeather(
			List<String> time,
			@JsonProperty("temperature_2m")
			List<Double> temperature2m,
			@JsonProperty("relative_humidity_2m")
			List<Integer> relativeHumidity2m,
			@JsonProperty("wind_speed_10m")
			List<Double> windSpeed10m,
			@JsonProperty("weather_code")
			List<Integer> weatherCode,
			List<Double> visibility,
			List<String> windDirection,
			List<String> conditions,
			boolean severeWeather
	) { }
}
