package com.scoreflux.repository;

import com.scoreflux.domain.RelatoCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelatoCampoRepository extends JpaRepository<RelatoCampo, Long> {
    List<RelatoCampo> findAllByClienteIdOrderByAtualizadoEmDesc(Long clienteId);
    Optional<RelatoCampo> findFirstByClienteIdOrderByAtualizadoEmDesc(Long clienteId);
}
