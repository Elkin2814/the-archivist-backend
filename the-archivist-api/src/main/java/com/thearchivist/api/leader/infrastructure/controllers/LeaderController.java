package com.thearchivist.api.leader.infrastructure.controllers;

import com.thearchivist.api.leader.domain.enums.Language;
import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.leader.infrastructure.controllers.dto.LeaderResponse;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.ports.in.GetMetadataUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leaders")
public class LeaderController {

    private final GetMetadataUseCase getMetadataUseCase;

    public LeaderController(GetMetadataUseCase getMetadataUseCase) {
        this.getMetadataUseCase = getMetadataUseCase;
    }

    /** Los 16 líderes con nombre, apodo e imagen en el idioma pedido (lang=es o lang=en; por defecto es). */
    @GetMapping
    public List<LeaderResponse> list(@RequestParam(defaultValue = "es") String lang) {
        Language language = Language.fromCode(lang);

        Map<Integer, String> images = new HashMap<>();
        for (LeaderMetadataInfo info : getMetadataUseCase.getLeaders()) {
            if (info.imageUrl() != null) images.put(info.id(), info.imageUrl());
        }

        return Arrays.stream(LeaderCatalog.values())
                .map(l -> new LeaderResponse(
                        l.getApiId(), l.name(), l.getFaction().name(), l.name(language), l.alias(language),
                        l.getApiId() == null ? null : images.get(l.getApiId())))
                .toList();
    }
}
