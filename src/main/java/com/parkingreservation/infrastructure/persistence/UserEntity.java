package com.parkingreservation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_electric", nullable = false)
    private boolean electricVehicleOwner;

    @Column(name = "has_handicapped_permit", nullable = false)
    private boolean handicappedPermitHolder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String name, boolean electricVehicleOwner, boolean handicappedPermitHolder) {
        this.name = name;
        this.electricVehicleOwner = electricVehicleOwner;
        this.handicappedPermitHolder = handicappedPermitHolder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isElectricVehicleOwner() {
        return electricVehicleOwner;
    }

    public boolean isHandicappedPermitHolder() {
        return handicappedPermitHolder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
