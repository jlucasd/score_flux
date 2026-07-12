package com.scoreflux.repository;

import com.scoreflux.domain.MovimentoCarteira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoCarteiraRepository extends JpaRepository<MovimentoCarteira, Long> {
    List<MovimentoCarteira> findByClienteIdOrderByDataAscIdAsc(Long clienteId);
}
