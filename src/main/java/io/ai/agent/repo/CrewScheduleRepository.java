package io.ai.agent.repo;

import io.ai.agent.entity.Airline;
import io.ai.agent.entity.CrewSchedule;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrewScheduleRepository extends JpaRepository<CrewSchedule, UUID> {
	List<CrewSchedule> findCrewScheduleByAirline(Airline airline);
}
