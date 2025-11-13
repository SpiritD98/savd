package com.colors.savd.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colors.savd.dto.ParametroReposicionDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.ParametroReposicion;
import com.colors.savd.repository.ParametroReposicionRepository;
import com.colors.savd.repository.VarianteSkuRepository;
import com.colors.savd.service.ParametroReposicionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParametroReposicionServiceImpl implements ParametroReposicionService{

    private final ParametroReposicionRepository repo;
    private final VarianteSkuRepository skuRepo;

    @Override @Transactional
    public ParametroReposicionDTO crear(ParametroReposicionDTO dto) {
        var sku = skuRepo.findById(dto.getSkuId())
                .orElseThrow(() -> new BusinessException("SKU no encontrado: " + dto.getSkuId()));
        if (repo.existsBySku_Id(sku.getId())) {
            throw new BusinessException("Ya existe un parámetro para este SKU.");
        }
        var p = new ParametroReposicion();
        p.setSku(sku);
        p.setMinStock(dto.getMinStock());
        p.setLeadTimeDias(dto.getLeadTimeDias());
        p.setStockSeguridad(dto.getStockSeguridad());
        p.setUltimaActualizacion(LocalDateTime.now());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p = repo.save(p);
        return toDTO(p);
    }

    @Override @Transactional
    public ParametroReposicionDTO actualizar(Long id, ParametroReposicionDTO dto) {
        var p = repo.findById(id).orElseThrow(() -> new BusinessException("Parámetro no encontrado id=" + id));
        if (!p.getSku().getId().equals(dto.getSkuId())) {
            throw new BusinessException("No se puede cambiar el SKU del parámetro.");
        }
        p.setMinStock(dto.getMinStock());
        p.setLeadTimeDias(dto.getLeadTimeDias());
        p.setStockSeguridad(dto.getStockSeguridad());
        p.setUltimaActualizacion(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        repo.save(p);
        return toDTO(p);
    }

    @Override @Transactional
    public void eliminar(Long id) {
        var p = repo.findById(id).orElseThrow(() -> new BusinessException("Parámetro no encontrado id=" + id));
        repo.delete(p);
    }

    @Override @Transactional(readOnly = true)
    public ParametroReposicionDTO obtener(Long id) {
        var p = repo.findById(id).orElseThrow(() -> new BusinessException("Parámetro no encontrado id=" + id));
        return toDTO(p);
    }

    @Override @Transactional(readOnly = true)
    public List<ParametroReposicionDTO> listar() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    private ParametroReposicionDTO toDTO(ParametroReposicion p) {
        return ParametroReposicionDTO.builder()
                .id(p.getId())
                .skuId(p.getSku().getId())
                .minStock(p.getMinStock())
                .leadTimeDias(p.getLeadTimeDias())
                .stockSeguridad(p.getStockSeguridad())
                .build();
    }
}   
