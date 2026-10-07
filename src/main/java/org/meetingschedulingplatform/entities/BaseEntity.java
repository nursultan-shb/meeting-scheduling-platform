package org.meetingschedulingplatform.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 9137532219675659531L;

    @Column(name = "version")
    @Version
    @NotNull
    private Integer version;

    @Column(name = "Created_At")
    @NotNull
    private Instant createdAt;

    @PrePersist
    private void setCreatedAt() {
        this.createdAt = Instant.now();
    }
}