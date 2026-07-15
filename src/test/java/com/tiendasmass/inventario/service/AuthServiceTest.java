package com.tiendasmass.inventario.service;

import com.tiendasmass.inventario.dao.UsuarioDAO;
import com.tiendasmass.inventario.model.Rol;
import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.support.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF11/RNF01: autenticación. RF12/RNF02: control de acceso por rol. */
class AuthServiceTest {

    private Path dbFile;
    private final AuthService authService = new AuthService();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @BeforeEach
    void setUp() throws IOException {
        dbFile = TestDb.crear();
    }

    @AfterEach
    void tearDown() throws IOException {
        TestDb.limpiar(dbFile);
    }

    @Test
    void loginConCredencialesValidasDevuelveElUsuarioConSuRol() {
        Optional<Usuario> resultado = authService.login("admin", "admin123");

        assertTrue(resultado.isPresent());
        assertEquals(Rol.ADMINISTRADOR, resultado.get().getRol());
    }

    @Test
    void loginConPasswordIncorrectaNoAutentica() {
        Optional<Usuario> resultado = authService.login("admin", "clave-equivocada");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void loginConUsuarioInexistenteNoAutentica() {
        Optional<Usuario> resultado = authService.login("no-existe", "cualquiera");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void loginConUsuarioDesactivadoNoAutenticaAunqueLaClaveSeaCorrecta() {
        Usuario empleado = usuarioDAO.buscarPorUsuario("empleado").orElseThrow();
        usuarioDAO.actualizarRolYEstado(empleado.getId(), empleado.getRol(), false);

        Optional<Usuario> resultado = authService.login("empleado", "empleado123");

        assertTrue(resultado.isEmpty());
    }
}
