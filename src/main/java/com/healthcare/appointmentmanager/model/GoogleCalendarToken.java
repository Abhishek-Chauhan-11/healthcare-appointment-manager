package com.healthcare.appointmentmanager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "google_calendar_tokens")
public class GoogleCalendarToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(nullable = false)
    private String accessToken;

    @JdbcTypeCode(Types.LONGVARCHAR)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String scope;

    public GoogleCalendarToken() {
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
