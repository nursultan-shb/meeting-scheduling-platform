package org.meetingschedulingplatform.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "Users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(generator = "use-or-generate")
    UUID id;
    String email;
    String name;
}