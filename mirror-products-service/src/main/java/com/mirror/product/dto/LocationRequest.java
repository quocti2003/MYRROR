package com.mirror.product.dto;

import com.mirror.product.entity.Location;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class LocationRequest {

    @NotBlank(message = "Location name is required")
    @Size(max = 500, message = "Location name must not exceed 500 characters")
    private String name;

    @NotNull(message = "Location type is required")
    private Location.LocationType type;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @NotBlank(message = "Operating hours are required")
    @Size(max = 255, message = "Hours must not exceed 255 characters")
    private String hours;

    @NotBlank(message = "Phone number is required")
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone;

    private Location.LocationStatus status = Location.LocationStatus.ACTIVE;

    // Constructors
    public LocationRequest() {}

    public LocationRequest(String name, Location.LocationType type, String address, String city,
                          BigDecimal latitude, BigDecimal longitude, String hours, String phone) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hours = hours;
        this.phone = phone;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location.LocationType getType() {
        return type;
    }

    public void setType(Location.LocationType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getHours() {
        return hours;
    }

    public void setHours(String hours) {
        this.hours = hours;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Location.LocationStatus getStatus() {
        return status;
    }

    public void setStatus(Location.LocationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LocationRequest{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", city='" + city + '\'' +
                ", status=" + status +
                '}';
    }
}