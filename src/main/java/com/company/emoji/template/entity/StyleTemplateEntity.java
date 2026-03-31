package com.company.emoji.template.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "style_template")
public class StyleTemplateEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "style_code", nullable = false)
    private String styleCode;

    @Column(nullable = false)
    private String description;

    @Column(name = "preview_url", nullable = false)
    private String previewUrl;

    @Column(name = "sample_images", nullable = false)
    private String sampleImages;

    @Column(name = "supported_aspect_ratios", nullable = false)
    private String supportedAspectRatios;

    @Column(name = "price_credits", nullable = false)
    private int priceCredits;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStyleCode() { return styleCode; }
    public void setStyleCode(String styleCode) { this.styleCode = styleCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public String getSampleImages() { return sampleImages; }
    public void setSampleImages(String sampleImages) { this.sampleImages = sampleImages; }
    public String getSupportedAspectRatios() { return supportedAspectRatios; }
    public void setSupportedAspectRatios(String supportedAspectRatios) { this.supportedAspectRatios = supportedAspectRatios; }
    public int getPriceCredits() { return priceCredits; }
    public void setPriceCredits(int priceCredits) { this.priceCredits = priceCredits; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}