package com.colors.savd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.colors.savd.model.Usuario;
import com.colors.savd.model.enums.EstatusRegistro;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Query("""
            select u from Usuario u
            join fetch u.rol
            where u.email = :email
            """)
    Optional<Usuario> findByEmailWithRol(@Param("email") String email);
    
    boolean existsByEmail(String email);
    
    // Buscar usuarios por el código de su rol
    List<Usuario> findByRol_Codigo(String codigo);
    
    List<Usuario> findByEstatusRegistro(EstatusRegistro estatus);
}
