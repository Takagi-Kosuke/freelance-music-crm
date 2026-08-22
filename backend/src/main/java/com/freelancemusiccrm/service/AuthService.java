package com.freelancemusiccrm.service;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.auth.LoginRequestDto;
import com.freelancemusiccrm.dto.auth.LoginResponseDto;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.AccountLockedException;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(WorkerRepository workerRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpServletRequest) {
        Worker worker = workerRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationFailedException("メールアドレスまたはパスワードが正しくありません"));

        if (worker.isLocked()) {
            throw new AccountLockedException("アカウントがロックされています。管理者にお問い合わせください");
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), worker.getPasswordHash());
        if (!passwordMatches) {
            int failedCount = worker.getFailedLoginCount() + 1;
            worker.setFailedLoginCount(failedCount);

            if (failedCount >= MAX_FAILED_LOGIN_COUNT) {
                worker.setLocked(true);
                worker.setLockedAt(LocalDateTime.now());
                workerRepository.save(worker);
                throw new AccountLockedException("認証に5回失敗したため、アカウントをロックしました");
            }

            workerRepository.save(worker);
            throw new AuthenticationFailedException("メールアドレスまたはパスワードが正しくありません");
        }

        if (worker.getFailedLoginCount() > 0 || worker.getLockedAt() != null) {
            worker.setFailedLoginCount(0);
            worker.setLockedAt(null);
            workerRepository.save(worker);
        }

        String token = jwtService.generateToken(worker.getEmail());

        return new LoginResponseDto(
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                token,
                "ログインに成功しました"
        );
    }

    public boolean logout(HttpServletRequest httpServletRequest) {
        SecurityContext context = SecurityContextHolder.getContext();
        boolean authenticated = context.getAuthentication() != null
                && context.getAuthentication().isAuthenticated()
                && !(context.getAuthentication() instanceof AnonymousAuthenticationToken);

        SecurityContextHolder.clearContext();
        return authenticated;
    }
}
