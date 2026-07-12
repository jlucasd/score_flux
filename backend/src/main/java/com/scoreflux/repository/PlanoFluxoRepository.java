package com.scoreflux.repository;

import com.scoreflux.domain.PlanoFluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoFluxoRepository extends JpaRepository<PlanoFluxo, Long> {
    List<PlanoFluxo> findByUfOrderByAnoDescNomeAsc(String uf);
    List<PlanoFluxo> findAllByOrderByAnoDescNomeAsc();
}
