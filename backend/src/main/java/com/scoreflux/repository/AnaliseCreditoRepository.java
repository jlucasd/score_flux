package com.scoreflux.repository;

import com.scoreflux.domain.AnaliseCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnaliseCreditoRepository extends JpaRepository<AnaliseCredito, Long> {
    List<AnaliseCredito> findByClienteIdOrderByCriadaEmDesc(Long clienteId);
}
