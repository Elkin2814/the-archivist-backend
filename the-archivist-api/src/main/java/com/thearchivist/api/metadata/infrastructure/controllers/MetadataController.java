package com.thearchivist.api.metadata.infrastructure.controllers;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.in.GetMetadataUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Datos de referencia de Halo Wars 2 guardados en la base (se llenan desde la Metadata API). */
@RestController
@RequestMapping("/metadata")
public class MetadataController {

    private final GetMetadataUseCase getMetadataUseCase;

    public MetadataController(GetMetadataUseCase getMetadataUseCase) {
        this.getMetadataUseCase = getMetadataUseCase;
    }

    /** Líderes tal como los publica la Metadata (útil para verificar IDs e imágenes). */
    @GetMapping("/leaders")
    public List<LeaderMetadataInfo> leaders() {
        return getMetadataUseCase.getLeaders();
    }

    @GetMapping("/maps")
    public List<MapInfo> maps() {
        return getMetadataUseCase.getMaps();
    }

    @GetMapping("/playlists")
    public List<PlaylistInfo> playlists() {
        return getMetadataUseCase.getPlaylists();
    }

    @GetMapping("/seasons")
    public List<SeasonInfo> seasons() {
        return getMetadataUseCase.getSeasons();
    }

    @GetMapping("/csr-designations")
    public List<CsrDesignationInfo> csrDesignations() {
        return getMetadataUseCase.getCsrDesignations();
    }
}
