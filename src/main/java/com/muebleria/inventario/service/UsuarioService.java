package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Usuario;
import com.muebleria.inventario.repository.UsuarioRepository;
import com.muebleria.inventario.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

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
            throw new RuntimeException("El nombre de usuario ya esta en uso");
        }
        String passwordEncryptada = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(passwordEncryptada);
        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario con id " + id + " no existe.");
        }
        usuarioRepository.deleteById(id);
    }

    public Map<String, String> login(String nombre, String password) {
        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        String token = jwtUtil.generateToken(usuario.getNombre(), usuario.getRol().name());

        return Map.of(
                "token", token,
                "rol", usuario.getRol().name()
        );
    }
}
