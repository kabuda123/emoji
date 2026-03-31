package com.company.emoji.template;

import com.company.emoji.template.entity.StyleTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<StyleTemplateEntity, String> {
}