package com.paginass.job;

import com.paginass.service.HighlightService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighlightExpirationJobServiceTest {

    @Mock private HighlightService highlightService;

    @InjectMocks
    private HighlightExpirationJobService job;

    @Test
    void jobName_returnsHighlightExpiration() {
        assertThat(job.jobName()).isEqualTo("HighlightExpiration");
    }

    @Test
    void execute_delegatesToHighlightServiceAndLogsCount() {
        when(highlightService.expireHighlights()).thenReturn(3);

        job.execute();

        verify(highlightService).expireHighlights();
    }

    @Test
    void execute_whenNoneExpired_completesWithoutError() {
        when(highlightService.expireHighlights()).thenReturn(0);

        job.execute();

        verify(highlightService).expireHighlights();
    }
}
