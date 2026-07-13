package com.pronurse.wallet.entity;

import com.pronurse.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "nurse_wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_user_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User nurseUser;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "total_earned", nullable = false)
    private BigDecimal totalEarned;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "negative_limit", nullable = false)
    private BigDecimal negativeLimit = new BigDecimal("500.00"); // Allow up to 500 INR credit

    @Column(name = "is_suspended")
    private boolean isSuspended = false;
}