package com.scoreflux.repository;

import com.scoreflux.domain.ItemFluxo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemFluxoRepository extends JpaRepository<ItemFluxo, Long> {
    List<ItemFluxo> findByPlanoIdOrderByOrdemAscIdAsc(Long planoId);
}
