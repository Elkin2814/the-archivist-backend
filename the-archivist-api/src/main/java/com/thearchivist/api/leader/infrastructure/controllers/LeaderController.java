package com.thearchivist.api.leader.infrastructure.controllers;

import com.thearchivist.api.leader.domain.enums.Language;
import com.thearchivist.api.leader.domain.models.LeaderCatalog;
import com.thearchivist.api.leader.infrastructure.controllers.dto.LeaderResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/leaders")
public class LeaderController {

    /** Los 16 líderes con nombre y apodo en el idioma pedido (lang=es o lang=en; por defecto es). */
    @GetMapping
    public List<LeaderResponse> list(@RequestParam(defaultValue = "es") String lang) {
        Language language = Language.fromCode(lang);
        return Arrays.stream(LeaderCatalog.values())
                .map(l -> new LeaderResponse(
                        l.getApiId(), l.name(), l.getFaction().name(), l.name(language), l.alias(language)))
                .toList();
    }
}
