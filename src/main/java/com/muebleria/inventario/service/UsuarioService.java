package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Usuario;
import com.muebleria.inventario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    UsuarioRepository usuarioRepository;

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> findByNombre(String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }

    public Usuario guardar(Usuario usuario) {

        if (usuarioRepository.existsByNombre(usuario.getNombre())) {
            throw new RuntimeException("El de usuario ya esta en uso");
        }

        String passwordEncryptada = new BCryptPasswordEncoder().encode(usuario.getPassword());
        usuario.setPassword(passwordEncryptada);

        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }
}
