package com.biddingagency.domain.sitephoto.repository;

import com.biddingagency.domain.sitephoto.entity.SitePhotoSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitePhotoSlotRepository extends JpaRepository<SitePhotoSlot, String> {

    List<SitePhotoSlot> findAllByOrderBySortOrderAsc();
}
