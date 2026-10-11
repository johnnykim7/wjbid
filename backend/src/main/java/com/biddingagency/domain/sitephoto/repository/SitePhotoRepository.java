package com.biddingagency.domain.sitephoto.repository;

import com.biddingagency.domain.sitephoto.entity.SitePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SitePhotoRepository extends JpaRepository<SitePhoto, UUID> {

    List<SitePhoto> findAllByVisibleTrue();
}
