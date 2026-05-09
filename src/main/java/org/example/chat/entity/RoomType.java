package org.example.chat.entity;

/**
 * Discriminator for chat room types.
 * GROUP rooms are standard multi-user rooms.
 * DIRECT rooms are private one-on-one DM rooms created automatically when a friendship is accepted.
 */
public enum RoomType {
    GROUP,
    DIRECT
}
