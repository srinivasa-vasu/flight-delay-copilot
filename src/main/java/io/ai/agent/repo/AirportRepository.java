package io.ai.agent.repo;

import io.ai.agent.entity.Airport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AirportRepository extends JpaRepository<Airport, UUID> {
	Optional<Airport> findByCode(String code);

	@Query("SELECT a FROM Airport a WHERE a.congestionLevel > :level")
	List<Airport> findCongestedAirports(@Param("level") Integer level);
}