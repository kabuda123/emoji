package com.company.emoji.template;

import com.company.emoji.common.api.ApiErrorCode;
import com.company.emoji.common.api.ApiException;
import com.company.emoji.template.dto.TemplateDetailResponse;
import com.company.emoji.template.dto.TemplateSummaryResponse;
import com.company.emoji.template.entity.StyleTemplateEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }
}