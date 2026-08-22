package com.freelancemusiccrm.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.freelancemusiccrm.dto.auth.LoginRequestDto;
import com.freelancemusiccrm.dto.auth.LoginResponseDto;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.AccountLockedException;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.repository.WorkerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {

    private static final int MAX_FAILED_LOGIN_COUNT = 5;

    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(WorkerRepository workerRepository, PasswordEncoder passwordEncoder) {
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
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

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                worker.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_WORKER"))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        httpServletRequest.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return new LoginResponseDto(
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                "ログインに成功しました"
        );
    }

    public boolean logout(HttpServletRequest httpServletRequest) {
        SecurityContext context = SecurityContextHolder.getContext();
        boolean authenticated = context.getAuthentication() != null
                && context.getAuthentication().isAuthenticated()
                && !(context.getAuthentication() instanceof AnonymousAuthenticationToken);

        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
        return authenticated;
    }
}
