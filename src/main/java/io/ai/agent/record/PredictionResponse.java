package io.ai.agent.record;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record PredictionResponse(String origin, String destination, LocalDateTime travelDate,
								 List<AirlinePrediction> predictions, LocalDateTime generatedAt) {
}
