package com.hackathon.domain.driver.dto;

import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.math.BigDecimal;
import java.util.List;

public record DriverResponse(
        Long driverId,
        String name,
        String plateNumber,
        String vehicleType,
        BigDecimal capacityTon,
        String bodyType,
        BigDecimal rating,
        Integer totalTrips,
        Integer completionRate,
        Integer minAcceptFare,
        RoutePreferences routePreferences
) {
    public record RoutePreferences(List<RegionPoint> origins, List<RegionPoint> destinations) {
    }

    public static DriverResponse from(Driver driver, List<DriverRoutePreference> preferences) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getPlateNumber(),
                driver.getVehicleType(),
                driver.getCapacityTon(),
                driver.getBodyType(),
                driver.getRating(),
                driver.getTotalTrips(),
                driver.getCompletionRate(),
                driver.getMinAcceptFare(),
                new RoutePreferences(points(preferences, Direction.ORIGIN),
                        points(preferences, Direction.DESTINATION))
        );
    }

    private static List<RegionPoint> points(List<DriverRoutePreference> preferences, Direction direction) {
        return preferences.stream()
                .filter(p -> p.getDirection() == direction)
                .map(p -> new RegionPoint(p.getSido(), p.getSigungu()))
                .toList();
    }
}
