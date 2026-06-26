package com.aitutor.service;

import com.aitutor.model.AuditLog;
import com.aitutor.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String username, String action, String details, HttpServletRequest request) {
        String ip = request != null ? request.getRemoteAddr() : "unknown";
        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .details(details)
                .ipAddress(ip)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        repository.save(log);
    }

    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return repository.findAllByOrderByTimestampDesc(pageable);
    }

    public Page<AuditLog> getLogsByUser(String username, Pageable pageable) {
        return repository.findByUsernameOrderByTimestampDesc(username, pageable);
    }
}
