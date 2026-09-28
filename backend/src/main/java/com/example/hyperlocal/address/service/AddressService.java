package com.example.hyperlocal.address.service;

import com.example.hyperlocal.address.dto.AddressDto;
import com.example.hyperlocal.address.dto.AddressRequest;
import com.example.hyperlocal.address.entity.Address;
import com.example.hyperlocal.address.repository.AddressRepository;
import com.example.hyperlocal.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressDto getAddressById(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found or does not belong to you", HttpStatus.NOT_FOUND));
        return AddressDto.fromEntity(address);
    }

    @Transactional
    public AddressDto createAddress(Long userId, AddressRequest req) {
        List<Address> existing = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        boolean isDefault = Boolean.TRUE.equals(req.getIsDefault()) || existing.isEmpty();

        if (isDefault) {
            existing.forEach(a -> {
                if (Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            });
        }

        Address address = new Address();
        address.setUserId(userId);
        mapRequestToEntity(req, address);
        address.setIsDefault(isDefault);

        Address saved = addressRepository.save(address);
        return AddressDto.fromEntity(saved);
    }

    @Transactional
    public AddressDto updateAddress(Long id, Long userId, AddressRequest req) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found or does not belong to you", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(req.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).forEach(a -> {
                if (Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            });
            address.setIsDefault(true);
        }

        mapRequestToEntity(req, address);
        Address updated = addressRepository.save(address);
        return AddressDto.fromEntity(updated);
    }

    @Transactional
    public void deleteAddress(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found or does not belong to you", HttpStatus.NOT_FOUND));
        addressRepository.delete(address);
    }

    @Transactional
    public AddressDto setDefaultAddress(Long id, Long userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found or does not belong to you", HttpStatus.NOT_FOUND));

        addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).forEach(a -> {
            if (Boolean.TRUE.equals(a.getIsDefault())) {
                a.setIsDefault(false);
                addressRepository.save(a);
            }
        });

        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        return AddressDto.fromEntity(saved);
    }

    private void mapRequestToEntity(AddressRequest req, Address address) {
        if (req.getLabel() != null) address.setLabel(req.getLabel().trim());
        address.setHouse(req.getHouse().trim());
        address.setStreet(req.getStreet().trim());
        address.setLocality(req.getLocality().trim());
        address.setCity(req.getCity().trim());
        address.setState(req.getState().trim());
        address.setPostalCode(req.getPostalCode().trim());
        address.setLatitude(req.getLatitude());
        address.setLongitude(req.getLongitude());
        address.setDeliveryInstructions(req.getDeliveryInstructions());
    }
}
