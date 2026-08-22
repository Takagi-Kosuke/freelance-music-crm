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
import com.freelancemusiccrm.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

class AuthServiceUnitTest {

    @Test
    void loginSuccessCreatesSessionAndReturnsResponse() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder);

        Worker worker = buildWorker();
        worker.setFailedLoginCount(2);
        worker.setLockedAt(LocalDateTime.now().minusDays(1));

        when(workerRepository.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("password123", worker.getPasswordHash())).thenReturn(true);
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);

        LoginResponseDto response = authService.login(new LoginRequestDto("worker@example.com", "password123"), request);

        assertThat(response.workerEmail()).isEqualTo("worker@example.com");
        assertThat(response.message()).isEqualTo("ログインに成功しました");
        assertThat(worker.getFailedLoginCount()).isZero();
        assertThat(worker.getLockedAt()).isNull();
        verify(session).setAttribute(any(String.class), any());
        verify(workerRepository, times(1)).save(worker);
    }

    @Test
    void loginFailureIncrementsFailedCount() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder);

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
        AuthService authService = new AuthService(workerRepository, passwordEncoder);

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
    void logoutInvalidatesSessionAndClearsContext() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        // Simulate authenticated context by first calling login setup quickly.
        Worker worker = buildWorker();
        when(workerRepository.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));
        when(passwordEncoder.matches("password123", worker.getPasswordHash())).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);
        authService.login(new LoginRequestDto("worker@example.com", "password123"), request);

        boolean wasAuthenticated = authService.logout(request);

        assertThat(wasAuthenticated).isTrue();
        verify(session).invalidate();
        verify(session).setAttribute(any(String.class), any());
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