package io.ai.agent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Table(name = "weather_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class WeatherData {
	@Id
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "airport_id")
	private Airport airport;

	private LocalDateTime dateTime;
	private Double temperature;
	private Double windSpeed;
	private String windDirection;
	private Double visibility;
	private String conditions;
	private String description;
	private Boolean severeWeather;
	private Integer precipitationProbability;
}