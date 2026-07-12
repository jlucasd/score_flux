package com.scoreflux.repository;

import com.scoreflux.domain.LancamentoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LancamentoCaixaRepository extends JpaRepository<LancamentoCaixa, Long> {
    List<LancamentoCaixa> findAllByOrderByDataAscIdAsc();
    List<LancamentoCaixa> findByStatusOrderByDataAscIdAsc(LancamentoCaixa.Status status);
}
