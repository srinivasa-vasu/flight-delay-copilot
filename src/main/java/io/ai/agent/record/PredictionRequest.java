package io.ai.agent.record;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record PredictionRequest(String origin, String destination, LocalDateTime travelDate,
								String preferredAirline, Integer flexibilityDays) {
}
