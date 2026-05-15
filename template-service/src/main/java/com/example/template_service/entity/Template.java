package com.example.template_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "templates",
        indexes = {
                @Index(name = "idx_template_key", columnList = "template_key", unique = true),
                @Index(name = "idx_template_category_active", columnList = "category,active")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "template_key", nullable = false, unique = true, length = 60)
    private String templateKey;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "preview_image_url", length = 500)
    private String previewImageUrl;

    @Lob
    @Column(name = "html_layout")
    private String htmlLayout;

    @Lob
    @Column(name = "css_styles")
    private String cssStyles;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Column(name = "layout_style", length = 50)
    private String layoutStyle;

    @Column(nullable = false)
    private boolean premium;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
