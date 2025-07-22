package com.hertz.hertz_be.global.aop;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class HibernateQueryCountAspect {

    private final EntityManager entityManager;

    @Around("execution(* com.hertz.hertz_be.domain.tuningreport.controller.TuningReportController.*(..))")
    public Object countHibernateQueriesOfTuningReportController(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = entityManager.unwrap(Session.class);
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();

        long beforeCount = stats.getQueryExecutionCount();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // 실제 메서드 실행
        long endTime = System.currentTimeMillis();

        long afterCount = stats.getQueryExecutionCount();
        long executedCount = afterCount - beforeCount;

        log.info("🧠 Hibernate가 총 {}개의 쿼리를 {}ms 동안 실행했습니다 - 메서드: {}", executedCount, (endTime - startTime), joinPoint.getSignature());
        return result;
    }
}
