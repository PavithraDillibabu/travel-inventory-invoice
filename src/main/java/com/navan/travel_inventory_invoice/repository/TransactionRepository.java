
package com.navan.travel_inventory_invoice.repository;

import com.navan.travel_inventory_invoice.entity.transaction.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, String> {

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.externalId = :externalId
        """)
    Optional<Transaction> findByExternalId(
            @Param("externalId") String externalId
    );

    @Query("""
        SELECT COUNT(t) > 0
        FROM Transaction t
        WHERE t.externalId = :externalId
        """)
    boolean existsByExternalId(
            @Param("externalId") String externalId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(
            @Param("id") String id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id IN :ids ORDER BY t.id")
    List<Transaction> findAllByIdForUpdate(@Param("ids") Collection<String> ids);

}
