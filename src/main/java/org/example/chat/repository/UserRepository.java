package org.example.chat.repository;

import org.example.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username,
            String displayName
    );
}
