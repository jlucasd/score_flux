package com.scoreflux.repository;

import com.scoreflux.domain.Demonstrativo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemonstrativoRepository extends JpaRepository<Demonstrativo, Long> {
    List<Demonstrativo> findByClienteIdOrderByExercicioAsc(Long clienteId);
    Optional<Demonstrativo> findByClienteIdAndExercicio(Long clienteId, int exercicio);
}
