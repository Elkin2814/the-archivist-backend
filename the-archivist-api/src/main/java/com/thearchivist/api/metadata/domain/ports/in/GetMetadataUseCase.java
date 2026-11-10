package com.thearchivist.api.metadata.domain.ports.in;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;

import java.util.List;

public interface GetMetadataUseCase {
    List<LeaderMetadataInfo> getLeaders();

    List<MapInfo> getMaps();

    List<PlaylistInfo> getPlaylists();

    List<SeasonInfo> getSeasons();

    List<CsrDesignationInfo> getCsrDesignations();
}
