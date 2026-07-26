package com.featureforge.service;

import com.featureforge.domain.Role;
import com.featureforge.domain.User;
import com.featureforge.dto.AuthResponse;
import com.featureforge.dto.LoginRequest;
import com.featureforge.dto.RegisterRequest;
import com.featureforge.exception.EmailAlreadyExistsException;
import com.featureforge.exception.InvalidCredentialsException;
import com.featureforge.repository.UserRepository;
import com.featureforge.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("bhave@example.com", "password123", "Bhave Thrabaabu");
        savedUser = User.builder()
                .email("bhave@example.com")
                .passwordHash("hashed")
                .fullName("Bhave Thrabaabu")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    void register_createsUser_whenEmailNotTaken() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeeds_withValidCredentials() {
        LoginRequest loginRequest = new LoginRequest("bhave@example.com", "password123");
        when(userRepository.findByEmail("bhave@example.com")).thenReturn(Optional.of(savedUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_throwsInvalidCredentials_onBadPassword() {
        LoginRequest loginRequest = new LoginRequest("bhave@example.com", "wrongpassword");
        doThrow(new BadCredentialsException("bad creds"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }
}
