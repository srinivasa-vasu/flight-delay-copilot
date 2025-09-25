package io.ai.agent.record;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record FlightHistoricalData(String flightNumber, String airline, String origin, String destination,
								   String date, String scheduledTime, String actualTime, Integer delayMinutes,
								   String status, String aircraftType, String weatherCondition, String delayReason,
								   String historicalPattern) {
}
