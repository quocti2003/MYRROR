package com.mirror.product.service;

import com.mirror.product.dto.LocationRequest;
import com.mirror.product.dto.LocationResponse;
import com.mirror.product.entity.Location;
import com.mirror.product.mapper.LocationMapper;
import com.mirror.product.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationMapper locationMapper;

    /**
     * Get all active locations
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllActiveLocations() {
        List<Location> locations = locationRepository.findByStatusOrderByCreatedAtDesc(
                Location.LocationStatus.ACTIVE);
        return locationMapper.toResponseList(locations);
    }

    /**
     * Get all locations (including inactive)
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        List<Location> locations = locationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return locationMapper.toResponseList(locations);
    }

    /**
     * Get location by ID
     */
    @Transactional(readOnly = true)
    public Optional<LocationResponse> getLocationById(String id) {
        return locationRepository.findById(id)
                .map(locationMapper::toResponse);
    }

    /**
     * Create a new location
     */
    public LocationResponse createLocation(LocationRequest request) {
        Location location = locationMapper.toEntity(request);
        Location savedLocation = locationRepository.save(location);
        return locationMapper.toResponse(savedLocation);
    }

    /**
     * Update an existing location
     */
    public Optional<LocationResponse> updateLocation(String id, LocationRequest request) {
        return locationRepository.findById(id)
                .map(existingLocation -> {
                    locationMapper.updateEntity(existingLocation, request);
                    Location updatedLocation = locationRepository.save(existingLocation);
                    return locationMapper.toResponse(updatedLocation);
                });
    }

    /**
     * Delete a location (soft delete by setting status to INACTIVE)
     */
    public boolean deleteLocation(String id) {
        return locationRepository.findById(id)
                .map(location -> {
                    location.setStatus(Location.LocationStatus.INACTIVE);
                    locationRepository.save(location);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Hard delete a location (permanent removal)
     */
    public boolean hardDeleteLocation(String id) {
        if (locationRepository.existsById(id)) {
            locationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Filter locations by city
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationsByCity(String city) {
        List<Location> locations = locationRepository.findByCityAndStatusOrderByCreatedAtDesc(
                city, Location.LocationStatus.ACTIVE);
        return locationMapper.toResponseList(locations);
    }

    /**
     * Filter locations by type
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationsByType(Location.LocationType type) {
        List<Location> locations = locationRepository.findByTypeAndStatusOrderByCreatedAtDesc(
                type, Location.LocationStatus.ACTIVE);
        return locationMapper.toResponseList(locations);
    }

    /**
     * Filter locations by city and type
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationsByCityAndType(String city, Location.LocationType type) {
        List<Location> locations = locationRepository.findByCityAndTypeAndStatusOrderByCreatedAtDesc(
                city, type, Location.LocationStatus.ACTIVE);
        return locationMapper.toResponseList(locations);
    }

    /**
     * Search locations by text (name, city, or address)
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> searchLocations(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveLocations();
        }
        
        List<Location> locations = locationRepository.searchByTextAndStatus(
                searchTerm.trim(), Location.LocationStatus.ACTIVE);
        return locationMapper.toResponseList(locations);
    }

    /**
     * Get available filter options
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> filters = new HashMap<>();
        
        // Get distinct cities
        List<String> cities = locationRepository.findDistinctCitiesByStatus(Location.LocationStatus.ACTIVE);
        filters.put("cities", cities);
        
        // Get distinct types
        List<Location.LocationType> types = locationRepository.findDistinctTypesByStatus(Location.LocationStatus.ACTIVE);
        filters.put("types", types);
        
        return filters;
    }

    /**
     * Get location statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLocationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total active locations
        long totalActive = locationRepository.countByStatus(Location.LocationStatus.ACTIVE);
        stats.put("totalActiveLocations", totalActive);
        
        // Count by type
        Map<String, Long> byType = new HashMap<>();
        for (Location.LocationType type : Location.LocationType.values()) {
            long count = locationRepository.countByTypeAndStatus(type, Location.LocationStatus.ACTIVE);
            byType.put(type.name(), count);
        }
        stats.put("locationsByType", byType);
        
        // Count by city
        List<String> cities = locationRepository.findDistinctCitiesByStatus(Location.LocationStatus.ACTIVE);
        Map<String, Long> byCity = new HashMap<>();
        for (String city : cities) {
            long count = locationRepository.countByCityAndStatus(city, Location.LocationStatus.ACTIVE);
            byCity.put(city, count);
        }
        stats.put("locationsByCity", byCity);
        
        return stats;
    }

    /**
     * Activate a location
     */
    public boolean activateLocation(String id) {
        return locationRepository.findById(id)
                .map(location -> {
                    location.setStatus(Location.LocationStatus.ACTIVE);
                    locationRepository.save(location);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Deactivate a location
     */
    public boolean deactivateLocation(String id) {
        return locationRepository.findById(id)
                .map(location -> {
                    location.setStatus(Location.LocationStatus.INACTIVE);
                    locationRepository.save(location);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Check if location exists
     */
    @Transactional(readOnly = true)
    public boolean locationExists(String id) {
        return locationRepository.existsById(id);
    }
}