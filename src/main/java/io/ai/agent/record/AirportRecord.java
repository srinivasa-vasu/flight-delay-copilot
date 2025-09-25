package io.ai.agent.record;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record AirportRecord(String code, String name, String city, String country) {
}