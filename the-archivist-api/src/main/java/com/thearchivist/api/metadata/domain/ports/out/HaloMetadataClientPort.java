package com.thearchivist.api.metadata.domain.ports.out;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;

import java.util.List;

/** Salida hacia la Metadata de Halo Wars 2 (solo /metadata/hw2/...). */
public interface HaloMetadataClientPort {
    List<LeaderMetadataInfo> fetchLeaders();

    List<MapInfo> fetchMaps();

    List<PlaylistInfo> fetchPlaylists();

    List<SeasonInfo> fetchSeasons();

    List<CsrDesignationInfo> fetchCsrDesignations();
}
