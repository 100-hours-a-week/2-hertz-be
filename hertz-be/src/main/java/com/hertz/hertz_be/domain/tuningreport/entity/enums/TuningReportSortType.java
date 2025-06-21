package com.hertz.hertz_be.domain.tuningreport.entity.enums;

import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public enum TuningReportSortType {
    LATEST {
        @Override
        public Page<TuningReport> fetch(Pageable pageable, TuningReportRepository repository) {
            return repository.findAllNotDeletedOrderByCreatedAtDesc(pageable);
        }
    },
    POPULAR {
        @Override
        public Page<TuningReport> fetch(Pageable pageable, TuningReportRepository repository) {
            return repository.findAllNotDeletedOrderByTotalReactionDesc(pageable);
        }
    };

    public abstract Page<TuningReport> fetch(Pageable pageable, TuningReportRepository repository);
}
