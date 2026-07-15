package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.UsuarioDAO;
import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/** RF11/RNF01: autenticación mediante usuario y contraseña. RF12/RNF02: control de acceso por rol. */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Optional<Usuario> login(String usuario, String password) {
        Optional<Usuario> encontrado = usuarioDAO.buscarPorUsuario(usuario);
        if (encontrado.isEmpty()) {
            log.warn("Login fallido: usuario '{}' no existe", usuario);
            return Optional.empty();
        }
        Usuario u = encontrado.get();
        if (!u.isActivo()) {
            log.warn("Login fallido: usuario '{}' está desactivado", usuario);
            return Optional.empty();
        }
        if (!PasswordUtil.matches(password, u.getPasswordHash())) {
            log.warn("Login fallido: contraseña incorrecta para usuario '{}'", usuario);
            return Optional.empty();
        }
        log.info("Login exitoso: usuario '{}' (rol {})", usuario, u.getRol());
        return Optional.of(u);
    }
}
