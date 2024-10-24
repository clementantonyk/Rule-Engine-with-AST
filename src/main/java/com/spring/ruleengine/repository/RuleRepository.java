package com.spring.ruleengine.repository;

import com.spring.ruleengine.model.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, Long> {
}
