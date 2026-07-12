package com.scoreflux.repository;

import com.scoreflux.domain.RespostaAnalise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RespostaAnaliseRepository extends JpaRepository<RespostaAnalise, Long> {
    List<RespostaAnalise> findByAnaliseId(Long analiseId);
    Optional<RespostaAnalise> findByAnaliseIdAndSubcriterioId(Long analiseId, Long subcriterioId);
}
