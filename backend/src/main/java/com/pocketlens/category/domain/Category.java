package com.pocketlens.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Spending category. Default categories have no owner ({@code userId == null}) and a stable {@code systemKey};
 * custom categories belong to one user (US-2.4).
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "system_key", length = 40)
    private String systemKey;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategoryKind kind;

    protected Category() {
    }

    public Category(UUID userId, String systemKey, String name, String color, CategoryKind kind) {
        this.userId = userId;
        this.systemKey = systemKey;
        this.name = name;
        this.color = color;
        this.kind = kind;
    }

    public boolean isDefault() {
        return userId == null;
    }

    public boolean isVisibleTo(UUID user) {
        return userId == null || userId.equals(user);
    }

    public void update(String newName, String newColor, CategoryKind newKind) {
        this.name = newName;
        this.color = newColor;
        this.kind = newKind;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSystemKey() {
        return systemKey;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public CategoryKind getKind() {
        return kind;
    }
}
