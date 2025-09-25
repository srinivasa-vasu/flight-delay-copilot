package io.ai.agent.record;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record AirlineRecord(String code, String name) {
}
