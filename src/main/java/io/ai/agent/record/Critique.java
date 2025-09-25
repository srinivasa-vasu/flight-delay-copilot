package io.ai.agent.record;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record Critique(boolean consistent, List<String> flaws, String suggestion) {
}
