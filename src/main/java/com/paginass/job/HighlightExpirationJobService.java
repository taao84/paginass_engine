package com.paginass.job;

import com.paginass.service.HighlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightExpirationJobService implements JobService {

    private final HighlightService highlightService;

    @Override
    public String jobName() {
        return "HighlightExpiration";
    }

    @Override
    public void execute() {
        int count = highlightService.expireHighlights();
        log.info("HighlightExpiration: {} highlight(s) disabled", count);
    }
}
