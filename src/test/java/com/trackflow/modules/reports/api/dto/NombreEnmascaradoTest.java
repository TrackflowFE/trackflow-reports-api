package com.trackflow.modules.reports.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NombreEnmascaradoTest {

    @Test
    void enmascaraCadaPalabraConSuInicial() {
        assertEquals("L*** G***", NombreEnmascarado.de("Luis Gómez"));
    }

    @Test
    void manejaNombresDeUnaPalabraYEspaciosRepetidos() {
        assertEquals("A***", NombreEnmascarado.de("Ana"));
        assertEquals("A*** B*** C***", NombreEnmascarado.de("  Ana   B  C "));
    }

    @Test
    void valoresVaciosONulosNoRompen() {
        assertEquals("", NombreEnmascarado.de("   "));
        assertEquals("", NombreEnmascarado.de(""));
        assertNull(NombreEnmascarado.de(null));
    }
}
