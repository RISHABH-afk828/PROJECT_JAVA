package com.example.hyperlocal.location.service;

import com.example.hyperlocal.common.exception.ApiException;
import com.example.hyperlocal.common.util.HaversineUtil;
import com.example.hyperlocal.vendor.entity.Vendor;
import com.example.hyperlocal.vendor.entity.VendorStatus;
import com.example.hyperlocal.vendor.repository.VendorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.*;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final VendorRepository vendorRepository;
    private RestTemplate restTemplate;

    @Value("${app.geoapify.api-key:}")
    private String geoapifyApiKey;

    public LocationService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> searchLocations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (geoapifyApiKey == null || geoapifyApiKey.isBlank()) {
            log.warn("Geoapify API key is not configured in environment (GEOAPIFY_API_KEY)");
            throw new ApiException("GEOAPIFY_KEY_NOT_CONFIGURED",
                    "Geoapify API key is not configured. Please set the GEOAPIFY_API_KEY environment variable.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://api.geoapify.com/v1/geocode/search")
                .queryParam("text", query.trim())
                .queryParam("format", "json")
                .queryParam("limit", 10)
                .queryParam("apiKey", geoapifyApiKey)
                .build()
                .toUriString();

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode results = response.getBody().path("results");
                if (results.isArray()) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (JsonNode r : results) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        String formatted = r.path("formatted").asText("");
                        item.put("label", formatted);
                        item.put("formattedAddress", formatted);
                        item.put("lat", r.path("lat").asDouble());
                        item.put("lng", r.path("lon").asDouble());

                        String locality = "";
                        if (r.hasNonNull("suburb")) {
                            locality = r.path("suburb").asText();
                        } else if (r.hasNonNull("neighbourhood")) {
                            locality = r.path("neighbourhood").asText();
                        } else if (r.hasNonNull("district")) {
                            locality = r.path("district").asText();
                        } else if (r.hasNonNull("city")) {
                            locality = r.path("city").asText();
                        }
                        item.put("locality", locality);
                        item.put("city", r.path("city").asText(""));
                        item.put("state", r.path("state").asText(""));
                        item.put("postalCode", r.path("postcode").asText(""));
                        item.put("street", r.path("street").asText(""));
                        item.put("house", r.path("housenumber").asText(""));
                        list.add(item);
                    }
                    return list;
                }
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Geoapify search failed with status: {}", e.getStatusCode());
            throw new ApiException("LOCATION_SERVICE_ERROR", "Geoapify location search failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException e) {
            log.error("Geoapify search timed out: {}", e.getMessage());
            throw new ApiException("LOCATION_SERVICE_TIMEOUT", "Geoapify request timed out", HttpStatus.GATEWAY_TIMEOUT);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in Geoapify search: ", e);
            throw new ApiException("LOCATION_SERVICE_ERROR", "Failed to search location: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public Map<String, Object> reverseGeocode(double lat, double lng) {
        if (geoapifyApiKey == null || geoapifyApiKey.isBlank()) {
            log.warn("Geoapify API key is not configured in environment (GEOAPIFY_API_KEY)");
            throw new ApiException("GEOAPIFY_KEY_NOT_CONFIGURED",
                    "Geoapify API key is not configured. Please set the GEOAPIFY_API_KEY environment variable.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://api.geoapify.com/v1/geocode/reverse")
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("apiKey", geoapifyApiKey)
                .build()
                .toUriString();

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode results = response.getBody().path("results");
                if (results.isArray() && !results.isEmpty()) {
                    JsonNode r = results.get(0);
                    Map<String, Object> map = new LinkedHashMap<>();
                    double resolvedLat = r.path("lat").asDouble(lat);
                    double resolvedLng = r.path("lon").asDouble(lng);
                    map.put("latitude", resolvedLat);
                    map.put("longitude", resolvedLng);
                    map.put("lat", resolvedLat);
                    map.put("lng", resolvedLng);
                    String formatted = r.path("formatted").asText("");
                    map.put("formattedAddress", formatted);
                    map.put("label", formatted);

                    String locality = "";
                    if (r.hasNonNull("suburb")) {
                        locality = r.path("suburb").asText();
                    } else if (r.hasNonNull("neighbourhood")) {
                        locality = r.path("neighbourhood").asText();
                    } else if (r.hasNonNull("district")) {
                        locality = r.path("district").asText();
                    } else if (r.hasNonNull("city")) {
                        locality = r.path("city").asText();
                    }
                    map.put("locality", locality);
                    map.put("city", r.path("city").asText(""));
                    map.put("state", r.path("state").asText(""));
                    map.put("postalCode", r.path("postcode").asText(""));
                    map.put("street", r.path("street").asText(""));
                    map.put("house", r.path("housenumber").asText(""));
                    return map;
                }
            }
            throw new ApiException("LOCATION_NOT_FOUND", "No address found for specified coordinates", HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Geoapify reverse-geocode failed with status: {}", e.getStatusCode());
            throw new ApiException("LOCATION_SERVICE_ERROR", "Geoapify reverse geocode failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException e) {
            log.error("Geoapify reverse-geocode timed out: {}", e.getMessage());
            throw new ApiException("LOCATION_SERVICE_TIMEOUT", "Geoapify request timed out", HttpStatus.GATEWAY_TIMEOUT);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in Geoapify reverse-geocode: ", e);
            throw new ApiException("LOCATION_SERVICE_ERROR", "Failed to reverse geocode: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public Map<String, Object> checkServiceability(double lat, double lng) {
        List<Vendor> activeVendors = vendorRepository.findByStatus(VendorStatus.ACTIVE);

        long serviceableVendors = activeVendors.stream()
                .filter(v -> {
                    double dist = HaversineUtil.distance(lat, lng, v.getLatitude(), v.getLongitude());
                    return dist <= v.getDeliveryRadiusKm();
                })
                .count();

        boolean isServiceable = serviceableVendors > 0;

        return Map.of(
                "serviceable", isServiceable,
                "vendorsNearby", serviceableVendors,
                "latitude", lat,
                "longitude", lng,
                "message", isServiceable
                        ? "Service available in your area with " + serviceableVendors + " stores!"
                        : "No participating local stores are currently delivering to this exact location."
        );
    }
}
