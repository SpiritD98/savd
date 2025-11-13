package com.colors.savd.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.colors.savd.dto.MovimientoInventarioDTO;
import com.colors.savd.exception.BusinessException;
import com.colors.savd.model.CanalVenta;
import com.colors.savd.model.KardexMovimiento;
import com.colors.savd.model.TipoMovimiento;
import com.colors.savd.repository.CanalVentaRepository;
import com.colors.savd.repository.KardexRepository;
import com.colors.savd.repository.TipoMovimientoRepository;
import com.colors.savd.repository.UsuarioRepository;
import com.colors.savd.repository.VarianteSkuRepository;
import com.colors.savd.service.InventarioService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService{

    private final VarianteSkuRepository skuRepo;
    private final KardexRepository kardexRepo;
    private final TipoMovimientoRepository tipoMovRepo;
    private final CanalVentaRepository canalRepo;
    private final UsuarioRepository usuarioRepo;

    @Override @Transactional
    public Long registrarStockInicial(MovimientoInventarioDTO dto, Long usuarioId) {
        if (dto.getCantidad() == null || dto.getCantidad() <= 0)
            throw new BusinessException("Cantidad inválida para stock inicial.");

        var sku = skuRepo.findById(dto.getSkuId())
                .orElseThrow(() -> new BusinessException("SKU no encontrado: " + dto.getSkuId()));

        TipoMovimiento tipo = tipoMovRepo.findByCodigo("INICIAL")
                .orElseThrow(() -> new BusinessException("TipoMovimiento 'INICIAL' no configurado"));

        var userRef = usuarioRepo.getReferenceById(usuarioId);
        LocalDateTime fecha = dto.getFechaHora() != null ? dto.getFechaHora() : LocalDateTime.now();

        var k = new KardexMovimiento();
        k.setFechaHora(fecha);
        k.setTipo(tipo);
        k.setSku(sku);
        k.setCantidad(dto.getCantidad());
        k.setSigno(+1); // stock inicial suma
        k.setReferencia(StringUtils.trimToNull(dto.getReferencia()));
        k.setObservacion(StringUtils.defaultIfBlank(dto.getObservacion(), "Stock inicial"));
        k.setUsuario(userRef);
        k.setCreatedAt(LocalDateTime.now());
        k.setIdempotencyKey(generarIdemKey(sku.getId(), fecha, "INICIAL", dto.getReferencia()));
        kardexRepo.save(k);

        return k.getId();
    }

    @Override @Transactional
    public Long registrarIngreso(MovimientoInventarioDTO dto, Long usuarioId) {
        if (dto.getCantidad() == null || dto.getCantidad() <= 0)
            throw new BusinessException("Cantidad inválida para ingreso.");

        var sku = skuRepo.findById(dto.getSkuId())
                .orElseThrow(() -> new BusinessException("SKU no encontrado: " + dto.getSkuId()));

        TipoMovimiento tipo = tipoMovRepo.findByCodigo("INGRESO")
                .orElseThrow(() -> new BusinessException("TipoMovimiento 'INGRESO' no configurado"));

        var userRef = usuarioRepo.getReferenceById(usuarioId);
        LocalDateTime fecha = dto.getFechaHora() != null ? dto.getFechaHora() : LocalDateTime.now();

        CanalVenta canal = null;
        if (dto.getCanalId() != null) {
            canal = canalRepo.findById(dto.getCanalId())
                    .orElseThrow(() -> new BusinessException("Canal no encontrado id=" + dto.getCanalId()));
        }

        var k = new KardexMovimiento();
        k.setFechaHora(fecha);
        k.setTipo(tipo);
        k.setSku(sku);
        k.setCantidad(dto.getCantidad());
        k.setSigno(+1); // ingreso suma
        k.setCanal(canal);
        k.setReferencia(StringUtils.trimToNull(dto.getReferencia()));
        k.setObservacion(StringUtils.defaultIfBlank(dto.getObservacion(), "Ingreso de inventario"));
        k.setUsuario(userRef);
        k.setCreatedAt(LocalDateTime.now());
        k.setIdempotencyKey(generarIdemKey(sku.getId(), fecha, "INGRESO", dto.getReferencia()));
        kardexRepo.save(k);

        return k.getId();
    }

    private String generarIdemKey(Long skuId, LocalDateTime fecha, String tipo, String referencia) {
        String base = skuId + "|" + fecha.toString() + "|" + tipo + "|" + Objects.toString(referencia, "");
        return UUID.nameUUIDFromBytes(base.getBytes()).toString();
    }
}
