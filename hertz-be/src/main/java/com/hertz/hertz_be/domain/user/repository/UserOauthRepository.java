package com.hertz.hertz_be.domain.user.repository;


import com.hertz.hertz_be.domain.user.entity.UserOauth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOauthRepository extends JpaRepository<UserOauth, Long> {

    Optional<UserOauth> findByProviderIdAndProvider(String providerId, String provider);
    boolean existsByProviderIdAndProvider(String providerId, String provider);

}
