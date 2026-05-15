package com.example.template_service.repository;

import com.example.template_service.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByIdAndActiveTrue(Long id);

    Optional<Template> findByTemplateKeyIgnoreCase(String templateKey);

    boolean existsByTemplateKeyIgnoreCase(String templateKey);

    List<Template> findByActiveTrueOrderByFeaturedDescNameAsc();

    List<Template> findByActiveTrueAndFeaturedTrueOrderByNameAsc();

    List<Template> findByActiveTrueAndCategoryIgnoreCaseOrderByFeaturedDescNameAsc(String category);

    List<Template> findByActiveTrueAndPremiumFalseOrderByFeaturedDescNameAsc();

    List<Template> findByActiveTrueAndPremiumTrueOrderByFeaturedDescNameAsc();

    List<Template> findByActiveTrueOrderByUsageCountDescNameAsc();
}
