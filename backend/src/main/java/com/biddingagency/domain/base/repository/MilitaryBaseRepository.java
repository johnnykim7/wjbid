package com.biddingagency.domain.base.repository;

import com.biddingagency.domain.base.entity.MilitaryBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilitaryBaseRepository extends JpaRepository<MilitaryBase, UUID> {

    List<MilitaryBase> findAllByOrderBySortOrderAscNameKoAsc();

    boolean existsByNameEnIgnoreCase(String nameEn);

    boolean existsByNameEnIgnoreCaseAndIdNot(String nameEn, UUID id);
}
