package com.company.emoji.common.bootstrap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "bootstrap_config_override")
public class BootstrapConfigOverrideEntity {
    @Id
    private String id;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "ios_review_mode")
    private Boolean iosReviewMode;

    @Column(name = "iap_enabled")
    private Boolean iapEnabled;

    @Column(name = "supported_login_methods")
    private String supportedLoginMethods;

    @Column(name = "privacy_url")
    private String privacyUrl;

    @Column(name = "terms_url")
    private String termsUrl;

    @Column(name = "ai_auth_url")
    private String aiAuthUrl;

    @Column(name = "generation_min_images")
    private Integer generationMinImages;

    @Column(name = "generation_max_images")
    private Integer generationMaxImages;

    @Column(name = "generation_poll_seconds")
    private Integer generationPollSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Boolean getIosReviewMode() { return iosReviewMode; }
    public void setIosReviewMode(Boolean iosReviewMode) { this.iosReviewMode = iosReviewMode; }
    public Boolean getIapEnabled() { return iapEnabled; }
    public void setIapEnabled(Boolean iapEnabled) { this.iapEnabled = iapEnabled; }
    public String getSupportedLoginMethods() { return supportedLoginMethods; }
    public void setSupportedLoginMethods(String supportedLoginMethods) { this.supportedLoginMethods = supportedLoginMethods; }
    public String getPrivacyUrl() { return privacyUrl; }
    public void setPrivacyUrl(String privacyUrl) { this.privacyUrl = privacyUrl; }
    public String getTermsUrl() { return termsUrl; }
    public void setTermsUrl(String termsUrl) { this.termsUrl = termsUrl; }
    public String getAiAuthUrl() { return aiAuthUrl; }
    public void setAiAuthUrl(String aiAuthUrl) { this.aiAuthUrl = aiAuthUrl; }
    public Integer getGenerationMinImages() { return generationMinImages; }
    public void setGenerationMinImages(Integer generationMinImages) { this.generationMinImages = generationMinImages; }
    public Integer getGenerationMaxImages() { return generationMaxImages; }
    public void setGenerationMaxImages(Integer generationMaxImages) { this.generationMaxImages = generationMaxImages; }
    public Integer getGenerationPollSeconds() { return generationPollSeconds; }
    public void setGenerationPollSeconds(Integer generationPollSeconds) { this.generationPollSeconds = generationPollSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
