package com.scoreflux.repository;

import com.scoreflux.domain.ValorMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValorMensalRepository extends JpaRepository<ValorMensal, Long> {
    List<ValorMensal> findByItemPlanoId(Long planoId);
    List<ValorMensal> findByItemId(Long itemId);
    Optional<ValorMensal> findByItemIdAndMes(Long itemId, int mes);
}
