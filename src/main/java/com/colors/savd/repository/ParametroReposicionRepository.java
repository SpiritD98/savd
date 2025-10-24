package com.colors.savd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.ParametroReposicion;

@Repository
public interface ParametroReposicionRepository extends JpaRepository<ParametroReposicion, Long>{

    Optional<ParametroReposicion> findBySku_Id(Long skuId);
    
    Boolean existsBySku_Id(Long skuId);
}
