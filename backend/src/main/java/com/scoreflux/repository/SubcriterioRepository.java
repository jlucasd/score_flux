package com.scoreflux.repository;

import com.scoreflux.domain.Subcriterio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcriterioRepository extends JpaRepository<Subcriterio, Long> {
    List<Subcriterio> findByPoliticaIdOrderByOrdemAsc(Long politicaId);
}
