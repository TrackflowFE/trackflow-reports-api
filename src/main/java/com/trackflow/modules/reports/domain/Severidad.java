package com.trackflow.modules.reports.domain;

import java.time.Duration;

/** Qué tan pasado del tiempo esperado va un envío, medido contra ese mismo tiempo. */
public enum Severidad {
    LEVE, MODERADA, CRITICA;

    /**
     * @param exceso tiempo que lleva pasado del máximo esperado
     * @param maximo máximo esperado sin movimiento en ese estado
     */
    public static Severidad de(Duration exceso, Duration maximo) {
        double proporcion = (double) exceso.toMillis() / maximo.toMillis();
        if (proporcion >= 1.0) {
            return CRITICA;
        }
        if (proporcion >= 0.5) {
            return MODERADA;
        }
        return LEVE;
    }
}
