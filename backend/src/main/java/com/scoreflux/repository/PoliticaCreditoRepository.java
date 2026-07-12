package com.scoreflux.repository;

import com.scoreflux.domain.PoliticaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PoliticaCreditoRepository extends JpaRepository<PoliticaCredito, Long> {
    Optional<PoliticaCredito> findFirstByVigenteTrueOrderByVersaoDesc();
}
