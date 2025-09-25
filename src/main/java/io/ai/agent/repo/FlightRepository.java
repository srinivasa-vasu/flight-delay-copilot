package io.ai.agent.repo;

import io.ai.agent.entity.Flight;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightRepository extends JpaRepository<Flight, UUID> {

	@Query("SELECT f FROM Flight f JOIN FETCH f.airline JOIN FETCH f.origin JOIN FETCH f.destination WHERE f.origin.code = :origin AND f.destination.code = :destination " +
			"AND f.scheduledDeparture BETWEEN :startDate AND :endDate")
	List<Flight> findByRoute(@Param("origin") String origin,
			@Param("destination") String destination,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);


	@Query("SELECT f FROM Flight f JOIN FETCH f.airline JOIN FETCH f.origin JOIN FETCH f.destination WHERE f.origin.code = :origin AND f.destination.code = :destination " +
			"AND f.scheduledDeparture BETWEEN :startDate AND :endDate")
	List<Flight> findByRouteAndDepartureDate(@Param("origin") String origin,
			@Param("destination") String destination,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT f FROM Flight f WHERE f.airline.code = :airlineCode " +
			"AND f.scheduledDeparture BETWEEN :startDate AND :endDate")
	List<Flight> findByAirlineAndDateRange(@Param("airlineCode") String airlineCode,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	@Query("SELECT AVG(f.delayMinutes) FROM Flight f WHERE f.airline.code = :airlineCode")
	Double getAverageDelayByAirline(@Param("airlineCode") String airlineCode);
}