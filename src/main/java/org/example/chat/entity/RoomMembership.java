package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "room_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "chat_room_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class RoomMembership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @ToString.Exclude
    private ChatRoom chatRoom;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @ToString.Include
    private MemberRole role = MemberRole.MEMBER;
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomMembership that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
