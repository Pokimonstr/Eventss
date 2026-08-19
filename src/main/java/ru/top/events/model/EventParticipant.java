package ru.top.events.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false, length = 20)
    private RsvpStatus rsvpStatus;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        if (this.role == null) this.role = ParticipantRole.PARTICIPANT;
        if (this.rsvpStatus == null) this.rsvpStatus = RsvpStatus.PENDING;
    }

    public enum ParticipantRole {
        OWNER, CO_ORGANIZER, PARTICIPANT
    }

    public enum RsvpStatus {
        PENDING, CONFIRMED, DECLINED, WAITLIST
    }
}