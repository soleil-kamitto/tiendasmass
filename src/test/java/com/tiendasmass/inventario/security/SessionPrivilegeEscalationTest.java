package com.tiendasmass.inventario.security;

import com.tiendasmass.inventario.model.Rol;
import com.tiendasmass.inventario.model.Usuario;
import com.tiendasmass.inventario.util.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Pruebas de seguridad — control de acceso por rol (RF12/RNF02, OWASP A01:2021).
 *
 * Hallazgo original: Session.actual() devolvía la referencia mutable real del usuario en
 * sesión. Cualquier código con esa referencia podía hacer
 * Session.actual().setRol(Rol.ADMINISTRADOR) y escalar privilegios en memoria sin volver a
 * autenticarse, porque era el mismo objeto que Session usa internamente para
 * esAdministrador(). Se corrigió devolviendo una copia defensiva (ver Usuario(Usuario) y
 * Session.actual()). Este test prueba que la corrección se sostiene.
 */
class SessionPrivilegeEscalationTest {

    @AfterEach
    void tearDown() {
        Session.cerrar();
    }

    @Test
    void mutarElUsuarioDevueltoPorActualNoEscalaPrivilegiosDeLaSesionReal() {
        Usuario empleado = new Usuario(2, "Empleado Demo", "empleado", "hash", Rol.EMPLEADO, true);
        Session.iniciar(empleado);

        Usuario copia = Session.actual();
        assertNotSame(empleado, copia, "actual() debe devolver una copia, no la instancia real");

        copia.setRol(Rol.ADMINISTRADOR); // intento de escalar privilegios con la referencia obtenida

        assertFalse(Session.esAdministrador(),
                "Mutar la copia no debe afectar el rol real de la sesión");
        assertFalse(Session.actual().getRol() == Rol.ADMINISTRADOR,
                "Una llamada posterior a actual() tampoco debe reflejar la mutación externa");
    }
}
