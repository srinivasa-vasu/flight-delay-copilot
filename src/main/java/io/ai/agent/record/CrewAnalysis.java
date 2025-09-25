package io.ai.agent.record;

import lombok.Builder;

@Builder
public record CrewAnalysis(boolean crewAvailable, boolean sufficientRest, boolean withinDutyLimits) {
}
