package io.ai.agent.service;

import io.ai.agent.entity.Airline;
import io.ai.agent.entity.Airport;
import io.ai.agent.record.AirlineRecord;
import io.ai.agent.record.AirportRecord;
import io.ai.agent.repo.AirlineRepository;
import io.ai.agent.repo.AirportRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

	private final AirportRepository airportRepository;
	private final AirlineRepository airlineRepository;

	public List<AirportRecord> getAllAirports() {
		return airportRepository.findAll().stream()
				.map(this::toAirportRecord)
				.collect(Collectors.toList());
	}

	public List<AirlineRecord> getAllAirlines() {
		return airlineRepository.findAll().stream()
				.map(this::toAirlineRecord)
				.collect(Collectors.toList());
	}

	private AirportRecord toAirportRecord(Airport airport) {
		return AirportRecord.builder()
				.code(airport.getCode())
				.name(airport.getName())
				.city(airport.getCity())
				.country(airport.getCountry())
				.build();
	}

	private AirlineRecord toAirlineRecord(Airline airline) {
		return AirlineRecord.builder()
				.code(airline.getCode())
				.name(airline.getName())
				.build();
	}
}
