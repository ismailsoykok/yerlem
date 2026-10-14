
package com.example.backend.user.repository;

import com.example.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByPhone(String phone);

    boolean existsByIdentityNumber(String identityNumber);

    Optional<User> findByPhone(String phone);
}
