package com.uade.microservices.booking.dto.rapidapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Representa los datos crudos de un hotel tal como vienen en el JSON de Booking / RapidAPI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingHotelRawDto {

    @JsonProperty("hotel_id")
    private Long hotelId;

    @JsonProperty("hotel_name")
    private String hotelName;

    @JsonProperty("name")
    private String alternativeName;

    @JsonProperty("star_rating")
    private Integer starRating;

    @JsonProperty("hotel_class")
    private Integer hotelClass;

    @JsonProperty("class")
    private Integer cssClass;

    @JsonProperty("review_score")
    private Double reviewScore;

    @JsonProperty("min_total_price")
    private Double minTotalPrice;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("main_photo_url")
    private String mainPhotoUrl;

    @JsonProperty("url_photo")
    private String urlPhoto;

    @JsonProperty("distance_to_cc")
    private String distanceToCc;

    @JsonProperty("distance")
    private String distance;

    @JsonProperty("has_free_parking")
    private Boolean hasFreeParking;

    @JsonProperty("is_free_cancellable")
    private Boolean isFreeCancellable;

    public BookingHotelRawDto() {
    }

    public BookingHotelRawDto(Long hotelId, String hotelName, Integer starRating,
                             Double minTotalPrice, String mainPhotoUrl) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.starRating = starRating;
        this.minTotalPrice = minTotalPrice;
        this.mainPhotoUrl = mainPhotoUrl;
    }

    /**
     * Resuelve el nombre del hotel buscando en las diferentes variantes de nombres de campo.
     */
    public String resolveNombre() {
        if (hotelName != null && !hotelName.isBlank()) {
            return hotelName.trim();
        }
        if (alternativeName != null && !alternativeName.isBlank()) {
            return alternativeName.trim();
        }
        return "Hotel Destino " + (hotelId != null ? hotelId : "F1");
    }

    /**
     * Resuelve las estrellas buscando en las diferentes claves devueltas por Booking.
     */
    public Integer resolveEstrellas() {
        if (starRating != null && starRating > 0) return starRating;
        if (hotelClass != null && hotelClass > 0) return hotelClass;
        if (cssClass != null && cssClass > 0) return cssClass;
        if (reviewScore != null && reviewScore > 0) {
            int stars = (int) Math.round(reviewScore / 2.0);
            return Math.clamp(stars, 1, 5);
        }
        return 3;
    }

    /**
     * Resuelve el precio por noche aproximado.
     */
    public BigDecimal resolvePrecioBase() {
        if (minTotalPrice != null && minTotalPrice > 0) {
            return BigDecimal.valueOf(minTotalPrice);
        }
        if (price != null && price > 0) {
            return BigDecimal.valueOf(price);
        }
        return BigDecimal.valueOf(120.00);
    }

    /**
     * Resuelve la URL de la imagen principal.
     */
    public String resolveImagenUrl() {
        if (mainPhotoUrl != null && !mainPhotoUrl.isBlank()) {
            return mainPhotoUrl;
        }
        if (urlPhoto != null && !urlPhoto.isBlank()) {
            return urlPhoto;
        }
        return "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80";
    }

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getAlternativeName() {
        return alternativeName;
    }

    public void setAlternativeName(String alternativeName) {
        this.alternativeName = alternativeName;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public Integer getHotelClass() {
        return hotelClass;
    }

    public void setHotelClass(Integer hotelClass) {
        this.hotelClass = hotelClass;
    }

    public Integer getCssClass() {
        return cssClass;
    }

    public void setCssClass(Integer cssClass) {
        this.cssClass = cssClass;
    }

    public Double getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(Double reviewScore) {
        this.reviewScore = reviewScore;
    }

    public Double getMinTotalPrice() {
        return minTotalPrice;
    }

    public void setMinTotalPrice(Double minTotalPrice) {
        this.minTotalPrice = minTotalPrice;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getMainPhotoUrl() {
        return mainPhotoUrl;
    }

    public void setMainPhotoUrl(String mainPhotoUrl) {
        this.mainPhotoUrl = mainPhotoUrl;
    }

    public String getUrlPhoto() {
        return urlPhoto;
    }

    public void setUrlPhoto(String urlPhoto) {
        this.urlPhoto = urlPhoto;
    }

    public String getDistanceToCc() {
        return distanceToCc;
    }

    public void setDistanceToCc(String distanceToCc) {
        this.distanceToCc = distanceToCc;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public Boolean getHasFreeParking() {
        return hasFreeParking;
    }

    public void setHasFreeParking(Boolean hasFreeParking) {
        this.hasFreeParking = hasFreeParking;
    }

    public Boolean getIsFreeCancellable() {
        return isFreeCancellable;
    }

    public void setIsFreeCancellable(Boolean isFreeCancellable) {
        this.isFreeCancellable = isFreeCancellable;
    }
}

