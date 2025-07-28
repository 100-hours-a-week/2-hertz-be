package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.projection.SignalRoomRepositoryCustom;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SignalRoomRepositoryImpl implements SignalRoomRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Long> findSignalRoomIdsOrderByLastMessageTime(Long userId, int offset, int limit) {
        return em.createQuery("""
                select sr.id
                from SignalRoom sr
                left join sr.messages m
                where sr.senderUser.id = :userId or sr.receiverUser.id = :userId
                group by sr.id
                order by max(m.sendAt) desc
            """, Long.class)
                .setParameter("userId", userId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<SignalRoom> findAllWithUsersByIds(List<Long> ids) {
        return em.createQuery("""
        select distinct sr from SignalRoom sr
        left join fetch sr.senderUser su
        left join fetch su.userOauth
        left join fetch sr.receiverUser ru
        left join fetch ru.userOauth
        left join fetch sr.tuningReport tr
        where sr.id in :ids
        """, SignalRoom.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public Long countSignalRoomsByUser(Long userId) {
        return em.createQuery("""
        select count(sr)
        from SignalRoom sr
        where sr.senderUser.id = :userId or sr.receiverUser.id = :userId
        """, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public Page<SignalRoom> findAllOrderByLastMessageTimeWithUsers(Long userId, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();

        List<Long> ids = findSignalRoomIdsOrderByLastMessageTime(userId, offset, limit);

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<SignalRoom> rooms = findAllWithUsersByIds(ids);
        Long total = countSignalRoomsByUser(userId);

        // 정렬 보존을 위해 id 순서를 기준으로 다시 정렬
        rooms.sort(Comparator.comparingInt(room -> ids.indexOf(room.getId())));

        return new PageImpl<>(rooms, pageable, total);
    }

}
