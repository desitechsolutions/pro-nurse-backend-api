package com.pronurse.wallet.repository;

import com.pronurse.wallet.entity.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutRepository extends JpaRepository<PayoutRequest, Long> {
}