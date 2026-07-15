package com.pronurse.wallet;

import tools.jackson.databind.ObjectMapper;
import com.pronurse.auth.entity.User;
import com.pronurse.auth.model.Role;
import com.pronurse.auth.repository.UserRepository;
import com.pronurse.auth.security.JwtUtil;
import com.pronurse.config.TestConfig;
import com.pronurse.wallet.entity.NurseWallet;
import com.pronurse.wallet.entity.PayoutRequest;
import com.pronurse.wallet.repository.NurseWalletRepository;
import com.pronurse.wallet.repository.PayoutRepository;
import com.pronurse.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
@Transactional
class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NurseWalletRepository walletRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User nurseUser;
    private NurseWallet wallet;
    private String token;

    @BeforeEach
    void setUp() {
        payoutRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        nurseUser = TestDataFactory.createUser("9876543211", Role.NURSE);
        nurseUser = userRepository.save(nurseUser);

        wallet = TestDataFactory.createNurseWallet(nurseUser);
        wallet.setCurrentBalance(BigDecimal.valueOf(1000.00));
        wallet.setTotalEarned(BigDecimal.valueOf(1500.00));
        wallet = walletRepository.save(wallet);

        token = jwtUtil.generateAccessToken(nurseUser);
    }

    @Test
    void testGetWalletSummarySuccess() throws Exception {
        mockMvc.perform(get("/api/nurse/wallet/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.walletBalance").value(1000.00))
                .andExpect(jsonPath("$.data.lifetimeEarnings").value(1500.00));
    }

    @Test
    void testRequestWithdrawalSuccess() throws Exception {
        Map<String, BigDecimal> body = Map.of("amount", BigDecimal.valueOf(500.00));

        mockMvc.perform(post("/api/nurse/wallet/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payout request submitted for admin review."));

        // Verify payout record
        List<PayoutRequest> payouts = payoutRepository.findAll();
        assertEquals(1, payouts.size());
        assertEquals("PENDING", payouts.get(0).getStatus());
        assertEquals(0, BigDecimal.valueOf(500.00).compareTo(payouts.get(0).getAmount()));

        // Balance is locked immediately to prevent double spending
        NurseWallet currentWallet = walletRepository.findByNurseUserMobile(nurseUser.getMobile()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(500.00).compareTo(currentWallet.getCurrentBalance()));
    }

    @Test
    void testRequestWithdrawalInsufficientBalance() throws Exception {
        Map<String, BigDecimal> body = Map.of("amount", BigDecimal.valueOf(2000.00)); // Exceeds 1000.00

        mockMvc.perform(post("/api/nurse/wallet/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient balance for withdrawal."));
    }
}
