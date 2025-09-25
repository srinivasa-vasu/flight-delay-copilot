package io.ai.agent.service;

import io.ai.agent.entity.Airline;
import io.ai.agent.entity.CrewSchedule;
import io.ai.agent.record.CrewAnalysis;
import io.ai.agent.repo.CrewScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrewService {

	private final CrewScheduleRepository crewScheduleRepository;

	@Tool
	public CrewAnalysis crewAvailability(Airline airline, LocalDateTime departureTime) {
		// This is a simplified analysis. A real-world implementation would be much more complex.

		List<CrewSchedule> availableCrew = crewScheduleRepository.findCrewScheduleByAirline(airline).stream()
				.filter(cs -> departureTime.isAfter(cs.getDutyStart()) && departureTime.isBefore(cs.getDutyEnd()))
				.toList();

		boolean crewAvailable = !availableCrew.isEmpty();
		boolean sufficientRest = true; // Assuming sufficient rest for simplicity
		boolean withinDutyLimits = true; // Assuming within duty limits for simplicity

		return CrewAnalysis.builder()
				.crewAvailable(crewAvailable)
				.sufficientRest(sufficientRest)
				.withinDutyLimits(withinDutyLimits)
				.build();
	}
}
