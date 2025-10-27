package com.colors.savd.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.VarianteSku;

@Repository
public interface VarianteSkuRepository extends JpaRepository<VarianteSku, Long>{

    Optional<VarianteSku> findBySku(String sku);

    Optional<VarianteSku> findByProducto_IdAndTalla_IdAndColor_Id(Long productoId, Long tallaId, Long colorId);

    List<VarianteSku> findByIdIn(Collection<Long> ids);

    List<VarianteSku> findBySkuIn(Collection<String> skus); // para “batch resolve” por SKU de Excel
    
    List<VarianteSku> findByProducto_Id(Long productoId);   // si filtras por producto en UI
    
    List<VarianteSku> findByActivoTrue(); // Útil para catálogos activos en UI
}
