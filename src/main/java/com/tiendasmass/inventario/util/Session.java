package com.tiendasmass.inventario.util;

import com.tiendasmass.inventario.model.Usuario;

/** Mantiene el usuario autenticado durante la sesión de escritorio (single-user por instancia). */
public final class Session {

    private static Usuario usuarioActual;

    private Session() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = new Usuario(usuario);
    }

    /**
     * Devuelve una copia del usuario autenticado: quien la reciba no puede mutar la sesión real
     * (p. ej. cambiarse el rol en memoria) porque no tiene la misma referencia.
     */
    public static Usuario actual() {
        return usuarioActual == null ? null : new Usuario(usuarioActual);
    }

    public static boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.getRol() == com.tiendasmass.inventario.model.Rol.ADMINISTRADOR;
    }

    public static void cerrar() {
        usuarioActual = null;
    }
}
