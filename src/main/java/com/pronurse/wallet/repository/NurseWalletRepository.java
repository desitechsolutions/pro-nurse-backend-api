package com.pronurse.wallet.repository;

import com.pronurse.wallet.entity.NurseWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface NurseWalletRepository extends JpaRepository<NurseWallet, Long> {

    Optional<NurseWallet> findByNurseUserMobile(String mobile);

    // Find all nurses whose current balance has dropped below a specific debt threshold
    List<NurseWallet> findByCurrentBalanceLessThanAndIsSuspendedFalse(BigDecimal threshold);

    // Optional: Count active vs suspended for your Admin Dashboard
    long countByIsSuspendedTrue();
}