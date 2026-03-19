package com.library.system.repository;

import com.library.system.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUser_IdOrderByGeneratedTimeDesc(Long userId);
    void deleteByUser_Id(Long userId);
}
