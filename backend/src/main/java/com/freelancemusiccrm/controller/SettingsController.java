package com.freelancemusiccrm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.freelancemusiccrm.dto.settings.SettingsResponseDto;
import com.freelancemusiccrm.dto.settings.SettingsUpdateDto;
import com.freelancemusiccrm.service.SettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<SettingsResponseDto> getCurrentSettings() {
        return ResponseEntity.ok(settingsService.getCurrentSettings());
    }

    @PutMapping
    public ResponseEntity<SettingsResponseDto> updateCurrentSettings(
            @Valid @RequestBody SettingsUpdateDto request
    ) {
        return ResponseEntity.ok(settingsService.updateCurrentSettings(request));
    }
}
