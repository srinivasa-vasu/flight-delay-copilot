package io.ai.agent.repo;

import io.ai.agent.entity.Airline;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, UUID> {
	Optional<Airline> findByCode(String code);

	@Query("SELECT a FROM Airline a ORDER BY a.onTimePerformance DESC")
	List<Airline> findTopPerformers();

	List<Airline> getAirlinesByCode(String code);

	Airline getAirlineByCode(String code);
}
