package com.freelancemusiccrm;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.freelancemusiccrm.dto.auth.LoginRequestDto;
import com.freelancemusiccrm.dto.auth.LoginResponseDto;
import com.freelancemusiccrm.entity.Worker;
import com.freelancemusiccrm.exception.AuthenticationFailedException;
import com.freelancemusiccrm.repository.WorkerRepository;
import com.freelancemusiccrm.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;

@SuppressWarnings("null")
class AuthServicePropertiesTest {

    @Property(tries = 100)
    @Tag("Feature: freelance-music-crm, Property 1: 認証結果の正確性")
    void authenticationResultMatchesCredentialValidity(
            @ForAll boolean validCredentials,
            @ForAll @StringLength(min = 1, max = 40) String localPart,
            @ForAll @StringLength(min = 1, max = 30) String password
    ) {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(workerRepository, passwordEncoder);

        String email = localPart + "@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);

        Worker worker = buildWorker(email);

        if (validCredentials) {
            when(workerRepository.findByEmail(email)).thenReturn(Optional.of(worker));
            when(passwordEncoder.matches(password, worker.getPasswordHash())).thenReturn(true);
            when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoginResponseDto response = authService.login(new LoginRequestDto(email, password), request);
            assertThat(response.workerEmail()).isEqualTo(email);
        } else {
            boolean missingUser = localPart.length() % 2 == 0;
            if (missingUser) {
                when(workerRepository.findByEmail(email)).thenReturn(Optional.empty());
            } else {
                when(workerRepository.findByEmail(email)).thenReturn(Optional.of(worker));
                when(passwordEncoder.matches(password, worker.getPasswordHash())).thenReturn(false);
                when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));
            }

            AuthenticationFailedException ex = assertThrows(
                    AuthenticationFailedException.class,
                    () -> authService.login(new LoginRequestDto(email, password), request)
            );
            assertThat(ex.getMessage()).contains("メールアドレスまたはパスワードが正しくありません");
        }
    }

    private Worker buildWorker(String email) {
        Worker worker = new Worker();
        worker.setId(1L);
        worker.setName("Worker");
        worker.setEmail(email);
        worker.setPasswordHash("$2a$10$dummyhash");
        worker.setLocked(false);
        worker.setFailedLoginCount(0);
        return worker;
    }
}
