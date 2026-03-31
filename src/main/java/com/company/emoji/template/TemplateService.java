package com.company.emoji.template;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.template.dto.TemplateDetailResponse;
import com.company.emoji.template.dto.InternalTemplateUpdateRequest;
import com.company.emoji.template.dto.TemplateSummaryResponse;
import com.company.emoji.template.entity.StyleTemplateEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.time.Instant;

@Service
public class TemplateService {
    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<TemplateSummaryResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .map(template -> new TemplateSummaryResponse(
                        template.getId(),
                        template.getName(),
                        template.getStyleCode(),
                        template.getPreviewUrl(),
                        template.getPriceCredits(),
                        template.isEnabled()
                ))
                .toList();
    }

    public TemplateDetailResponse getTemplate(String id) {
        StyleTemplateEntity template = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Template not found"));

        return new TemplateDetailResponse(
                template.getId(),
                template.getName(),
                template.getStyleCode(),
                template.getDescription(),
                template.getPreviewUrl(),
                splitCsv(template.getSampleImages()),
                template.getPriceCredits(),
                template.isEnabled(),
                splitCsv(template.getSupportedAspectRatios())
        );
    }

    @Transactional
    public TemplateDetailResponse updateTemplate(String id, InternalTemplateUpdateRequest request) {
        StyleTemplateEntity template = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Template not found"));

        if (request.enabled() != null) {
            template.setEnabled(request.enabled());
        }
        if (request.priceCredits() != null) {
            template.setPriceCredits(request.priceCredits());
        }
        template.setUpdatedAt(Instant.now());
        return getTemplate(templateRepository.save(template).getId());
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }
}
