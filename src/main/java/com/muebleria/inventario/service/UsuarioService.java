package com.muebleria.inventario.service;

import com.muebleria.inventario.entidad.Rol;
import com.muebleria.inventario.entidad.Usuario;
import com.muebleria.inventario.repository.UsuarioRepository;
import com.muebleria.inventario.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService  implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

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
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }
        // Asignar rol por defecto si no viene asignado
        if (usuario.getRol() == null) {
            usuario.setRol(Rol.USUARIO);  // Asumiendo que Rol es un enum y USUARIO es el valor por defecto
        }
        String passwordEncryptada = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(passwordEncryptada);
        return usuarioRepository.save(usuario);
    }
    public Usuario editar(Usuario usuario) {
        Usuario usuarioActual = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Optional<Usuario> usuarioConMismoNombre = usuarioRepository.findByNombre(usuario.getNombre());
        if (usuarioConMismoNombre.isPresent() &&
                !usuarioConMismoNombre.get().getId().equals(usuario.getId())) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        usuarioActual.setNombre(usuario.getNombre());

        if (!usuario.getPassword().equals(usuarioActual.getPassword())) {
            String passwordEncryptada = passwordEncoder.encode(usuario.getPassword());
            usuarioActual.setPassword(passwordEncryptada);
        }

        if (usuario.getRol() == null) {
            usuarioActual.setRol(Rol.USUARIO);
        } else {
            usuarioActual.setRol(usuario.getRol());
        }

        return usuarioRepository.save(usuarioActual);
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
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombre(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return new org.springframework.security.core.userdetails.User(
                usuario.getNombre(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }
}
