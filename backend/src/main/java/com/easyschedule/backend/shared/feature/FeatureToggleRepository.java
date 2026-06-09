package com.easyschedule.backend.shared.feature;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, Long> {
    List<FeatureToggle> findAllByOrderByNameAsc();
    Optional<FeatureToggle> findByKey(String key);
    boolean existsByKey(String key);
}
