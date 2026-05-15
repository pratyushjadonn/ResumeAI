package com.example.template_service;

import com.example.template_service.entity.Template;
import com.example.template_service.repository.TemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class TemplateServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TemplateServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner seedTemplates(TemplateRepository templateRepository) {
		return args -> {
			if (templateRepository.count() > 0) {
				return;
			}

			templateRepository.saveAll(List.of(
				Template.builder()
					.templateKey("modern-professional")
					.name("Modern Professional")
					.category("Professional")
					.description("A clean modern resume design built for tech and product roles.")
					.previewImageUrl("")
					.htmlLayout("<section><h1>Modern Professional</h1></section>")
					.cssStyles("body { font-family: Arial, sans-serif; }")
					.accentColor("#4f46e5")
					.layoutStyle("Modern")
					.premium(false)
					.active(true)
					.featured(true)
					.usageCount(0)
					.tags(List.of("modern", "professional", "tech"))
					.build(),
				Template.builder()
					.templateKey("clean-minimal")
					.name("Clean Minimal")
					.category("Minimal")
					.description("A sharp, minimal layout that highlights skills and achievements.")
					.previewImageUrl("")
					.htmlLayout("<section><h1>Clean Minimal</h1></section>")
					.cssStyles("body { font-family: Helvetica, sans-serif; }")
					.accentColor("#0f766e")
					.layoutStyle("Minimal")
					.premium(false)
					.active(true)
					.featured(true)
					.usageCount(0)
					.tags(List.of("clean", "minimal", "simple"))
					.build(),
				Template.builder()
					.templateKey("classic-executive")
					.name("Classic Executive")
					.category("Executive")
					.description("A polished executive resume format for senior leadership roles.")
					.previewImageUrl("")
					.htmlLayout("<section><h1>Classic Executive</h1></section>")
					.cssStyles("body { font-family: Georgia, serif; }")
					.accentColor("#b45309")
					.layoutStyle("Executive")
					.premium(true)
					.active(true)
					.featured(true)
					.usageCount(0)
					.tags(List.of("executive", "classic", "leadership"))
					.build(),
				Template.builder()
					.templateKey("creative-portfolio")
					.name("Creative Portfolio")
					.category("Creative")
					.description("A stylish creative resume for designers, marketing and product creatives.")
					.previewImageUrl("")
					.htmlLayout("<section><h1>Creative Portfolio</h1></section>")
					.cssStyles("body { font-family: 'Nunito', sans-serif; }")
					.accentColor("#be185d")
					.layoutStyle("Creative")
					.premium(true)
					.active(true)
					.featured(true)
					.usageCount(0)
					.tags(List.of("creative", "portfolio", "designer"))
					.build()
			));
		};
	}
}
