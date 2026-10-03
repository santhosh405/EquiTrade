package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbol(String symbol);

    Optional<Stock> findBySymbolIgnoreCase(String symbol);

    List<Stock> findBySymbolContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String symbol, String companyName);

    @Query("SELECT DISTINCT s.sector FROM Stock s WHERE s.sector IS NOT NULL AND s.sector != '' ORDER BY s.sector ASC")
    List<String> findDistinctSectors();

    List<Stock> findTop4ByOrderByChangePercentDesc();

    List<Stock> findTop4ByOrderByChangePercentAsc();

    @Query("SELECT s FROM Stock s WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.companyName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:sector IS NULL OR :sector = '' OR s.sector = :sector) AND " +
           "(:minPrice IS NULL OR s.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR s.price <= :maxPrice)")
    Page<Stock> filterStocks(
        @Param("query") String query,
        @Param("sector") String sector,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );
}