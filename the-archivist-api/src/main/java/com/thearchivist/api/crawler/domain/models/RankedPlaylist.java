package com.thearchivist.api.crawler.domain.models;

import java.util.UUID;

/** Cola con clasificación disponible y cuántos puestos hay guardados. */
public record RankedPlaylist(UUID playlistId, String name, UUID seasonId, long players) {}
