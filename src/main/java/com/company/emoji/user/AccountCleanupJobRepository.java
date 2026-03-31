package com.company.emoji.user;

import com.company.emoji.user.entity.AccountCleanupJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountCleanupJobRepository extends JpaRepository<AccountCleanupJobEntity, String> {
    Optional<AccountCleanupJobEntity> findByUserId(String userId);
}
