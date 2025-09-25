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

@Table(name = "crew_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class CrewSchedule {
	@Id
	@UuidGenerator
	private UUID id;

	private String crewId;
	private LocalDateTime dutyStart;
	private LocalDateTime dutyEnd;
	private Integer flightHours;
	private Integer restHours;
	private String base;
	private Boolean available;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "airline_id")
	private Airline airline;
}
