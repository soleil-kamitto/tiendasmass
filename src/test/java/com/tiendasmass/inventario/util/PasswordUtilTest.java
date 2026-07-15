package com.tiendasmass.inventario.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RNF03: las contraseñas nunca se guardan en texto plano. */
class PasswordUtilTest {

    @Test
    void hashesDeLaMismaClaveSonDistintosPorElSalt() {
        String hash1 = PasswordUtil.hash("clave123");
        String hash2 = PasswordUtil.hash("clave123");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void matchesEsVerdaderoConLaClaveCorrecta() {
        String hash = PasswordUtil.hash("clave123");

        assertTrue(PasswordUtil.matches("clave123", hash));
    }

    @Test
    void matchesEsFalsoConLaClaveIncorrecta() {
        String hash = PasswordUtil.hash("clave123");

        assertFalse(PasswordUtil.matches("otra-clave", hash));
    }

    @Test
    void matchesEsFalsoSiElHashAlmacenadoTieneFormatoInvalido() {
        assertFalse(PasswordUtil.matches("clave123", "esto-no-es-un-hash-valido"));
    }
}
