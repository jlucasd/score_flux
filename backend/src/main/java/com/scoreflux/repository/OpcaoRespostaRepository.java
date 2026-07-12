package com.scoreflux.repository;

import com.scoreflux.domain.OpcaoResposta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcaoRespostaRepository extends JpaRepository<OpcaoResposta, Long> {
    List<OpcaoResposta> findBySubcriterioPoliticaIdOrderBySubcriterioOrdemAscOrdemAsc(Long politicaId);
    List<OpcaoResposta> findBySubcriterioId(Long subcriterioId);
}
