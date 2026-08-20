package com.scoreflux.repository;

import com.scoreflux.domain.PesoAtribuido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PesoAtribuidoRepository extends JpaRepository<PesoAtribuido, Long> {
    List<PesoAtribuido> findByClienteId(Long clienteId);
    void deleteByClienteId(Long clienteId);
}
