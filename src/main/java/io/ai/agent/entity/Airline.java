package io.ai.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "airline")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {
	@Id
	@UuidGenerator
	private UUID id;

	@Column(unique = true, nullable = false)
	private String code;

	@Column(nullable = false)
	private String name;

	private Double onTimePerformance;
	private Integer totalFlights;
	private Integer delayedFlights;
	private Double averageDelay;
}
