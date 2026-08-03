package com.mock.controllers;

import com.mock.dto.AuditEvent;
import com.mock.services.AuditJournal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/v1")
public class AdminController {
    private final AuditJournal journal;

    public AdminController(AuditJournal journal) {
        this.journal = journal;
    }

    @GetMapping("/auditoria")
    public List<AuditEvent> audit() {
        return journal.events();
    }
}
