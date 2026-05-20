package com.paginass.job;

import com.paginass.entity.Advertisement;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvertisementExpirationCleanupJobService implements JobService {

    private final AdvertisementRepository advertisementRepository;
    private final StorageService storageService;

    @Lazy
    @Autowired
    private AdvertisementExpirationCleanupJobService self;

    @Override
    public String jobName() {
        return "AdvertisementExpirationCleanup";
    }

    @Override
    public void execute() {
        List<String> pathsToDelete = self.demoteExpiredAds();
        int deleted = 0;
        for (String path : pathsToDelete) {
            try {
                storageService.deleteFile(path);
                deleted++;
            } catch (IOException e) {
                log.warn("Failed to delete image from storage: {} — {}", path, e.getMessage());
            }
        }
        log.info("Cleared {} image(s) from storage for expired advertisements", deleted);
    }

    @Transactional
    public List<String> demoteExpiredAds() {
        List<Advertisement> expired = advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(Instant.now());
        List<String> paths = new ArrayList<>();
        for (Advertisement ad : expired) {
            ad.setActive(false);
            if (!ad.getImageUrls().isEmpty()) {
                paths.addAll(ad.getImageUrls());
                ad.setImageUrls(new ArrayList<>());
            }
        }
        log.info("Demoted {} expired advertisement(s)", expired.size());
        return paths;
    }
}
