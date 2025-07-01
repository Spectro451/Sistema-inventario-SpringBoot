package com.muebleria.inventario.entidad;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoMaterial {
    MADERA,
    ADHESIVO,
    FIJACION,
    HERRAJES,
    PINTURA,
    VIDRIO,
    METAL,
    PLASTICO,
    ACABADO;


    @JsonCreator
    public static TipoMaterial fromString(String key) {
        return key == null ? null : TipoMaterial.valueOf(key.toUpperCase());
    }
}
