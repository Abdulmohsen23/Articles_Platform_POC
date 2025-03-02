package com.example.first.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<ArticleEntity> articles = new ArrayList<>();

    // Enum for user roles
    public enum Role {
        USER,
        ADMIN
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<ArticleEntity> getArticles() {
        return articles;
    }

    public void setArticles(List<ArticleEntity> articles) {
        this.articles = articles;
    }
}

