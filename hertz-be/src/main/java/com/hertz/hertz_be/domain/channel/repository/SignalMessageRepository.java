package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalMessageRepository extends JpaRepository<SignalMessage, Long> {
}
