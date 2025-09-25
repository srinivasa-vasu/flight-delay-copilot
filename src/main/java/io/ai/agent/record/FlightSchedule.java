package io.ai.agent.record;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record FlightSchedule(String flightNumber, LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival,
							 String aircraftType) {
}
