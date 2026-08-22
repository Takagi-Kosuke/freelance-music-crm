package com.freelancemusiccrm;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.freelancemusiccrm.dto.auth.LoginRequestDto;
import com.freelancemusiccrm.dto.auth.LoginResponseDto;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.AccountLockedException;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.security.JwtService;
import com.freelancemusiccrm.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;

class AuthServiceUnitTest {

    @Test
    void loginSuccessReturnsJwtAndResetsFailedState() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder, new JwtService("test-secret-key-must-be-long-enough-for-hmac", 86400000));

        Worker worker = buildWorker();
        worker.setFailedLoginCount(2);
        worker.setLockedAt(LocalDateTime.now().minusDays(1));

        when(workerRepository.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("password123", worker.getPasswordHash())).thenReturn(true);
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpServletRequest request = mock(HttpServletRequest.class);

        LoginResponseDto response = authService.login(new LoginRequestDto("worker@example.com", "password123"), request);

        assertThat(response.workerEmail()).isEqualTo("worker@example.com");
        assertThat(response.token()).isNotBlank();
        assertThat(response.message()).isEqualTo("ログインに成功しました");
        assertThat(worker.getFailedLoginCount()).isZero();
        assertThat(worker.getLockedAt()).isNull();
        verify(workerRepository, times(1)).save(worker);
    }

    @Test
    void loginFailureIncrementsFailedCount() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder, new JwtService("test-secret-key-must-be-long-enough-for-hmac", 86400000));

        Worker worker = buildWorker();
        when(workerRepository.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("wrong-password", worker.getPasswordHash())).thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpServletRequest request = mock(HttpServletRequest.class);

        AuthenticationFailedException ex = assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(new LoginRequestDto("worker@example.com", "wrong-password"), request)
        );

        assertThat(ex.getMessage()).contains("メールアドレスまたはパスワードが正しくありません");
        assertThat(worker.getFailedLoginCount()).isEqualTo(1);
        assertThat(worker.isLocked()).isFalse();
    }

    @Test
    void loginFailureLocksAccountAfterFiveAttempts() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder, new JwtService("test-secret-key-must-be-long-enough-for-hmac", 86400000));

        Worker worker = buildWorker();
        worker.setFailedLoginCount(4);
        when(workerRepository.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("wrong-password", worker.getPasswordHash())).thenReturn(false);
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpServletRequest request = mock(HttpServletRequest.class);

        AccountLockedException ex = assertThrows(
                AccountLockedException.class,
                () -> authService.login(new LoginRequestDto("worker@example.com", "wrong-password"), request)
        );

        assertThat(ex.getMessage()).contains("認証に5回失敗したため、アカウントをロックしました");
        assertThat(worker.getFailedLoginCount()).isEqualTo(5);
        assertThat(worker.isLocked()).isTrue();
        assertThat(worker.getLockedAt()).isNotNull();
        verify(workerRepository, times(1)).save(worker);
    }

    @Test
    void logoutClearsAuthenticationWhenAuthenticated() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder, new JwtService("test-secret-key-must-be-long-enough-for-hmac", 86400000));

        HttpServletRequest request = mock(HttpServletRequest.class);
        boolean wasAuthenticated = authService.logout(request);

        assertThat(wasAuthenticated).isFalse();
    }

    private Worker buildWorker() {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setName("Worker");
        worker.setEmail("worker@example.com");
        worker.setPasswordHash("$2a$10$dummyhash");
        worker.setLocked(false);
        worker.setFailedLoginCount(0);
        return worker;
    }
}