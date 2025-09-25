package io.ai.agent.repo;

import io.ai.agent.entity.WeatherData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, UUID> {

	@Query("SELECT w FROM WeatherData w JOIN FETCH w.airport WHERE w.airport.code = :airportCode " +
			"AND w.dateTime BETWEEN :startTime AND :endTime")
	List<WeatherData> findByAirportAndTimeRange(@Param("airportCode") String airportCode,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);
}
