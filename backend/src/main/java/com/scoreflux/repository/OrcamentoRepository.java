package com.scoreflux.repository;

import com.scoreflux.domain.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    List<Orcamento> findByPlanoIdOrderByIdAsc(Long planoId);
}
