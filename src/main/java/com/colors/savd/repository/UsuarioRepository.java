package com.colors.savd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.Usuario;
import com.colors.savd.model.enums.EstatusRegistro;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // Buscar usuarios por el código de su rol
    List<Usuario> findByRol_Codigo(String codigo);
    
    List<Usuario> findByEstatusRegistro(EstatusRegistro estatus);
}
