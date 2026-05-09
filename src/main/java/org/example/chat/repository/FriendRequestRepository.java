package org.example.chat.repository;

import org.example.chat.entity.FriendRequest;
import org.example.chat.entity.FriendRequestStatus;
import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findByRequesterAndRecipient(User requester, User recipient);

    List<FriendRequest> findByRecipientAndStatus(User recipient, FriendRequestStatus status);

    List<FriendRequest> findByRequesterAndStatus(User requester, FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndRecipient(Long id, User recipient);

    List<FriendRequest> findByRequesterOrRecipient(User requester, User recipient);
}
