package io.ai.agent.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Flight {
	@Id
	@UuidGenerator
	private UUID id;

	private String flightNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "airline_id")
	private Airline airline;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "origin_id")
	private Airport origin;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_id")
	private Airport destination;

	private LocalDateTime scheduledDeparture;
	private LocalDateTime actualDeparture;
	private LocalDateTime scheduledArrival;
	private LocalDateTime actualArrival;

	private Integer delayMinutes;

	@Enumerated(EnumType.STRING)
	private FlightStatus status;

	private String delayReason;
	private String aircraftType;
	private String weatherCondition;
	private Boolean crewIssue;
	private Boolean maintenanceIssue;
	private Integer gateDelay;
	private Integer taxiDelay;
}
