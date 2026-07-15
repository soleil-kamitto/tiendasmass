package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.UsuarioDAO;
import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.util.PasswordUtil;

import java.util.Optional;

/** RF11/RNF01: autenticación mediante usuario y contraseña. RF12/RNF02: control de acceso por rol. */
public class AuthService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Optional<Usuario> login(String usuario, String password) {
        Optional<Usuario> encontrado = usuarioDAO.buscarPorUsuario(usuario);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }
        Usuario u = encontrado.get();
        if (!u.isActivo()) {
            return Optional.empty();
        }
        if (!PasswordUtil.matches(password, u.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(u);
    }
}
