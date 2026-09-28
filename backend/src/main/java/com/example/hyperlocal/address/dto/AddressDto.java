package com.example.hyperlocal.address.dto;

import com.example.hyperlocal.address.entity.Address;

import java.time.Instant;

public class AddressDto {
    private Long id;
    private Long userId;
    private String label;
    private String house;
    private String street;
    private String locality;
    private String city;
    private String state;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String deliveryInstructions;
    private Boolean isDefault;
    private String formattedAddress;
    private Instant createdAt;

    public static AddressDto fromEntity(Address address) {
        AddressDto dto = new AddressDto();
        dto.setId(address.getId());
        dto.setUserId(address.getUserId());
        dto.setLabel(address.getLabel());
        dto.setHouse(address.getHouse());
        dto.setStreet(address.getStreet());
        dto.setLocality(address.getLocality());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setLatitude(address.getLatitude());
        dto.setLongitude(address.getLongitude());
        dto.setDeliveryInstructions(address.getDeliveryInstructions());
        dto.setIsDefault(address.getIsDefault());
        dto.setFormattedAddress(address.toFormattedString());
        dto.setCreatedAt(address.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
