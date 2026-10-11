package com.biddingagency.domain.federal.repository;

import com.biddingagency.domain.federal.entity.FederalAwardType;
import com.biddingagency.domain.federal.entity.FederalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FederalContractRepository extends JpaRepository<FederalContract, UUID> {

    Optional<FederalContract> findByAwardKey(String awardKey);

    List<FederalContract> findByAwardKeyIn(Collection<String> awardKeys);

    List<FederalContract> findByPiid(String piid);

    /** 옵션 끝날짜 상세를 받을 계약(BID-MKT-026). 상위 계약 없는 계약 중 상세를 아직 받지 않은 것 */
    @Query("SELECT c FROM FederalContract c WHERE c.awardType IN :types AND c.detailFetchedAt IS NULL "
            + "AND c.endDate BETWEEN :from AND :to ORDER BY c.endDate")
    List<FederalContract> findDetailTargets(@Param("types") Collection<FederalAwardType> types,
                                            @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<FederalContract> findByNaicsCode(String naicsCode);

    /** 업종 고르기 목록: code, name, 계약 수 */
    @Query("SELECT c.naicsCode, MAX(c.naicsName), COUNT(c) FROM FederalContract c WHERE c.naicsCode IS NOT NULL "
            + "AND (:q IS NULL OR LOWER(c.naicsCode) LIKE CONCAT('%', :q, '%') OR LOWER(c.naicsName) LIKE CONCAT('%', :q, '%')) "
            + "GROUP BY c.naicsCode ORDER BY COUNT(c) DESC, c.naicsCode")
    List<Object[]> countByNaics(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT MAX(c.naicsName) FROM FederalContract c WHERE c.naicsCode = :code")
    Optional<String> findNaicsName(@Param("code") String code);
}
