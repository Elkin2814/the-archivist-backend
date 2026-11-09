package com.thearchivist.api.leader.domain.enums;

/** Idiomas en los que The Archivist muestra nombres propios (líderes, etc.). */
public enum Language {
    ES,
    EN;

    /** "en" (o "EN") da inglés; cualquier otro valor, nulo o vacío da español. */
    public static Language fromCode(String code) {
        return "en".equalsIgnoreCase(code == null ? "" : code.trim()) ? EN : ES;
    }
}
