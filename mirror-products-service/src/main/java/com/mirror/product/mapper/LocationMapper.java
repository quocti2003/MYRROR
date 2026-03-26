package com.mirror.product.mapper;

import com.mirror.product.dto.LocationRequest;
import com.mirror.product.dto.LocationResponse;
import com.mirror.product.entity.Location;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LocationMapper {

    /**
     * Convert LocationRequest to Location entity for creation
     */
    public Location toEntity(LocationRequest request) {
        if (request == null) {
            return null;
        }

        Location location = new Location();
        location.setName(request.getName());
        location.setType(request.getType());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setHours(request.getHours());
        location.setPhone(request.getPhone());
        location.setStatus(request.getStatus() != null ? request.getStatus() : Location.LocationStatus.ACTIVE);

        return location;
    }

    /**
     * Update existing Location entity with LocationRequest data
     */
    public void updateEntity(Location existing, LocationRequest request) {
        if (existing == null || request == null) {
            return;
        }

        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setAddress(request.getAddress());
        existing.setCity(request.getCity());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());
        existing.setHours(request.getHours());
        existing.setPhone(request.getPhone());
        
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        
    }

    /**
     * Convert Location entity to LocationResponse
     */
    public LocationResponse toResponse(Location location) {
        if (location == null) {
            return null;
        }

        return new LocationResponse(location);
    }

    /**
     * Convert list of Location entities to list of LocationResponse
     */
    public List<LocationResponse> toResponseList(List<Location> locations) {
        if (locations == null) {
            return null;
        }

        return locations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Location entity to a simplified response for listings
     */
    public LocationResponse toSimpleResponse(Location location) {
        if (location == null) {
            return null;
        }

        LocationResponse response = new LocationResponse();
        response.setId(location.getId());
        response.setName(location.getName());
        response.setType(location.getType());
        response.setCity(location.getCity());
        response.setStatus(location.getStatus());
        
        // Include coordinates for map functionality
        response.setCoordinates(new LocationResponse.CoordinatesDto(
                location.getLatitude(), location.getLongitude()));
        
        return response;
    }

    /**
     * Convert list of Location entities to simplified response list
     */
    public List<LocationResponse> toSimpleResponseList(List<Location> locations) {
        if (locations == null) {
            return null;
        }

        return locations.stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }
}