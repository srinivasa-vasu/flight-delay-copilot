package io.ai.agent.record;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record WeatherAnalysis(String summary, List<String> alerts) {
	public boolean isSevereWeather() {
		return alerts != null && !alerts.isEmpty() && alerts.size() > 1;
	}
}
