package com.scoreflux.repository;

import com.scoreflux.domain.DemonstrativoCampoExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemonstrativoCampoExtraRepository extends JpaRepository<DemonstrativoCampoExtra, Long> {
    List<DemonstrativoCampoExtra> findAllByDemonstrativoId(Long demonstrativoId);
}
