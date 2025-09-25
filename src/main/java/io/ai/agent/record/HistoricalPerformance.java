package io.ai.agent.record;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record HistoricalPerformance(Double averageDelay, Double onTimePercentage, Integer totalFlights,
									Double cancellationRate) {
}