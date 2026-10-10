package com.biddingagency.domain.inquiry.repository;

import com.biddingagency.domain.inquiry.entity.InquiryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryTypeRepository extends JpaRepository<InquiryType, String> {

    List<InquiryType> findByActiveTrueOrderBySortOrderAsc();
}
