package com.donsang.notification.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/notifications")
public class NotificationHistoryController {

    private final NotificationHistoryRepository repository;

    public NotificationHistoryController(NotificationHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<NotificationHistoryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be non-negative and size must be between 1 and 100");
        }
        return repository.findAllByOrderByReceivedAtDesc(PageRequest.of(page, size))
                .map(NotificationHistoryResponse::from);
    }
}
