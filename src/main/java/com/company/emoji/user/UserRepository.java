package com.company.emoji.user;

import com.company.emoji.user.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUserEntity, String> {
    Optional<AppUserEntity> findByProviderAndExternalSubject(String provider, String externalSubject);
}
