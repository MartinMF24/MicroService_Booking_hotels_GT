package com.uade.microservices.booking.dto.rapidapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Representa los datos crudos de un hotel tal como vienen en el JSON de Booking / RapidAPI.
 * Soporta tanto el formato tradicional plano (hotel_name, star_rating, min_total_price)
 * como el formato anidado moderno de RapidAPI (objeto 'property' y 'accessibilityLabel').
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingHotelRawDto {

    @JsonProperty("hotel_id")
    private Long hotelId;

    @JsonProperty("hotel_name")
    private String hotelName;

    @JsonProperty("name")
    private String alternativeName;

    @JsonProperty("accessibilityLabel")
    private String accessibilityLabel;

    @JsonProperty("property")
    private PropertyRawDto property;

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
     * Resuelve el nombre real del hotel buscando en todas las variantes estructurales de Booking.
     */
    public String resolveNombre() {
        if (hotelName != null && !hotelName.isBlank()) {
            return hotelName.trim();
        }
        if (alternativeName != null && !alternativeName.isBlank()) {
            return alternativeName.trim();
        }
        if (property != null && property.getName() != null && !property.getName().isBlank()) {
            return property.getName().trim();
        }
        if (accessibilityLabel != null && !accessibilityLabel.isBlank()) {
            String firstLine = accessibilityLabel.split("\\r?\\n")[0].replaceAll("\\.+$", "").trim();
            if (!firstLine.isBlank()) {
                return firstLine;
            }
        }
        return "Hotel Destino " + (hotelId != null ? hotelId : "F1");
    }

    /**
     * Resuelve las estrellas buscando en las diferentes claves devueltas por Booking.
     * Siempre devuelve un valor entre 1 y 5 (CHECK hoteles_estrellas_check).
     */
    public Integer resolveEstrellas() {
        if (starRating != null && starRating > 0) return Math.clamp(starRating, 1, 5);
        if (hotelClass != null && hotelClass > 0) return Math.clamp(hotelClass, 1, 5);
        if (cssClass != null && cssClass > 0) return Math.clamp(cssClass, 1, 5);
        if (property != null) {
            if (property.getPropertyClass() != null && property.getPropertyClass() > 0) {
                return Math.clamp(property.getPropertyClass(), 1, 5);
            }
            if (property.getAccuratePropertyClass() != null && property.getAccuratePropertyClass() > 0) {
                return Math.clamp(property.getAccuratePropertyClass(), 1, 5);
            }
            if (property.getReviewScore() != null && property.getReviewScore() > 0) {
                int stars = (int) Math.round(property.getReviewScore() / 2.0);
                return Math.clamp(stars, 1, 5);
            }
        }
        if (reviewScore != null && reviewScore > 0) {
            int stars = (int) Math.round(reviewScore / 2.0);
            return Math.clamp(stars, 1, 5);
        }
        return 3;
    }

    /**
     * Resuelve el precio por noche aproximado a partir de los datos crudos.
     */
    public BigDecimal resolvePrecioBase() {
        if (minTotalPrice != null && minTotalPrice > 0) {
            return BigDecimal.valueOf(minTotalPrice);
        }
        if (price != null && price > 0) {
            return BigDecimal.valueOf(price);
        }
        if (property != null && property.getPriceBreakdown() != null) {
            if (property.getPriceBreakdown().getGrossPrice() != null
                    && property.getPriceBreakdown().getGrossPrice().getValue() != null
                    && property.getPriceBreakdown().getGrossPrice().getValue() > 0) {
                return BigDecimal.valueOf(property.getPriceBreakdown().getGrossPrice().getValue());
            }
            if (property.getPriceBreakdown().getStrikethroughPrice() != null
                    && property.getPriceBreakdown().getStrikethroughPrice().getValue() != null
                    && property.getPriceBreakdown().getStrikethroughPrice().getValue() > 0) {
                return BigDecimal.valueOf(property.getPriceBreakdown().getStrikethroughPrice().getValue());
            }
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
        if (property != null && property.getPhotoUrls() != null && !property.getPhotoUrls().isEmpty()) {
            for (String photo : property.getPhotoUrls()) {
                if (photo != null && !photo.isBlank()) {
                    return photo;
                }
            }
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

    public String getAccessibilityLabel() {
        return accessibilityLabel;
    }

    public void setAccessibilityLabel(String accessibilityLabel) {
        this.accessibilityLabel = accessibilityLabel;
    }

    public PropertyRawDto getProperty() {
        return property;
    }

    public void setProperty(PropertyRawDto property) {
        this.property = property;
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

    // =========================================================================
    // DTOs anidados para soportar la respuesta real de RapidAPI Booking
    // =========================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyRawDto {

        @JsonProperty("name")
        private String name;

        @JsonProperty("propertyClass")
        private Integer propertyClass;

        @JsonProperty("accuratePropertyClass")
        private Integer accuratePropertyClass;

        @JsonProperty("qualityClass")
        private Integer qualityClass;

        @JsonProperty("reviewScore")
        private Double reviewScore;

        @JsonProperty("reviewCount")
        private Integer reviewCount;

        @JsonProperty("priceBreakdown")
        private PriceBreakdownRawDto priceBreakdown;

        @JsonProperty("photoUrls")
        private List<String> photoUrls;

        @JsonProperty("wishlistName")
        private String wishlistName;

        @JsonProperty("latitude")
        private Double latitude;

        @JsonProperty("longitude")
        private Double longitude;

        public PropertyRawDto() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getPropertyClass() {
            return propertyClass;
        }

        public void setPropertyClass(Integer propertyClass) {
            this.propertyClass = propertyClass;
        }

        public Integer getAccuratePropertyClass() {
            return accuratePropertyClass;
        }

        public void setAccuratePropertyClass(Integer accuratePropertyClass) {
            this.accuratePropertyClass = accuratePropertyClass;
        }

        public Integer getQualityClass() {
            return qualityClass;
        }

        public void setQualityClass(Integer qualityClass) {
            this.qualityClass = qualityClass;
        }

        public Double getReviewScore() {
            return reviewScore;
        }

        public void setReviewScore(Double reviewScore) {
            this.reviewScore = reviewScore;
        }

        public Integer getReviewCount() {
            return reviewCount;
        }

        public void setReviewCount(Integer reviewCount) {
            this.reviewCount = reviewCount;
        }

        public PriceBreakdownRawDto getPriceBreakdown() {
            return priceBreakdown;
        }

        public void setPriceBreakdown(PriceBreakdownRawDto priceBreakdown) {
            this.priceBreakdown = priceBreakdown;
        }

        public List<String> getPhotoUrls() {
            return photoUrls;
        }

        public void setPhotoUrls(List<String> photoUrls) {
            this.photoUrls = photoUrls;
        }

        public String getWishlistName() {
            return wishlistName;
        }

        public void setWishlistName(String wishlistName) {
            this.wishlistName = wishlistName;
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceBreakdownRawDto {

        @JsonProperty("grossPrice")
        private PriceValueRawDto grossPrice;

        @JsonProperty("strikethroughPrice")
        private PriceValueRawDto strikethroughPrice;

        public PriceBreakdownRawDto() {
        }

        public PriceValueRawDto getGrossPrice() {
            return grossPrice;
        }

        public void setGrossPrice(PriceValueRawDto grossPrice) {
            this.grossPrice = grossPrice;
        }

        public PriceValueRawDto getStrikethroughPrice() {
            return strikethroughPrice;
        }

        public void setStrikethroughPrice(PriceValueRawDto strikethroughPrice) {
            this.strikethroughPrice = strikethroughPrice;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceValueRawDto {

        @JsonProperty("value")
        private Double value;

        @JsonProperty("currency")
        private String currency;

        public PriceValueRawDto() {
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
