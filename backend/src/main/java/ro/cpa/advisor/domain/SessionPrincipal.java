package ro.cpa.advisor.domain;

import java.util.UUID;

/** Vizitatorul autentificat al cererii curente: doar id-ul sesiunii, fara date personale (nu exista). */
public record SessionPrincipal(UUID sessionId) {}
