package com.biddingagency.controller.admin;

import com.biddingagency.domain.base.entity.MilitaryBase;
import com.biddingagency.domain.base.service.MilitaryBaseService;
import com.biddingagency.dto.BaseRequest;
import com.biddingagency.dto.BaseResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 기지 관리 (CR-136, BID-MKT-010)
 */
@RestController
@RequestMapping("/admin/bases")
@RequiredArgsConstructor
@Tag(name = "Admin - Bases", description = "Military base list used to group opportunities by title")
@PreAuthorize("hasRole('ADMIN')")
public class BaseAdminController {

    private final MilitaryBaseService baseService;

    @GetMapping
    @Operation(summary = "List bases with matched opportunity counts")
    public ResponseEntity<List<BaseResponses.AdminBase>> list() {
        return ResponseEntity.ok(baseService.listForAdmin());
    }

    @PostMapping
    @Operation(summary = "Add a base")
    public ResponseEntity<UUID> create(@RequestBody BaseRequest request) {
        MilitaryBase saved = baseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a base")
    public ResponseEntity<UUID> update(@PathVariable UUID id, @RequestBody BaseRequest request) {
        return ResponseEntity.ok(baseService.update(id, request).getId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a base")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        baseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unmatched")
    @Operation(summary = "Opportunities that match no base")
    public ResponseEntity<BaseResponses.Unmatched> unmatched() {
        return ResponseEntity.ok(baseService.unmatched());
    }
}
