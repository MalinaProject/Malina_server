package project.malina.Services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.malina.JwtAuthenticationResponse;
import project.malina.Security.Role;
import project.malina.Security.User;
import project.malina.SignInRequest;
import project.malina.SignUpRequest;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger log = LogManager.getLogger(AuthenticationService.class);
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация пользователя
     *
     * @param request данные пользователя
     * @return токен
     */
    public JwtAuthenticationResponse signUp(SignUpRequest request) {

        log.info("Регистрация нового пользователя '{}'", request.getUsername());
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        userService.create(user);
        log.debug("Пользователь '{}' успешно сохранен", request.getUsername());

        var jwt = jwtService.generateToken(user);
        log.trace("Сформирован JWT для пользователя '{}'", request.getUsername());
        return new JwtAuthenticationResponse(jwt);
    }

    /**
     * Аутентификация пользователя
     *
     * @param request данные пользователя
     * @return токен
     */
    public JwtAuthenticationResponse signIn(SignInRequest request) {
        log.info("Попытка аутентификации пользователя '{}'", request.getUsername());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            ));
            log.debug("Аутентификация пользователя '{}' прошла успешно", request.getUsername());
        } catch (AuthenticationException ex) {
            log.error("Ошибка аутентификации пользователя '{}'", request.getUsername(), ex);
            throw ex;
        }

        var user = userService
                .userDetailsService()
                .loadUserByUsername(request.getUsername());

        var jwt = jwtService.generateToken(user);
        log.trace("Сформирован JWT для пользователя '{}'", request.getUsername());
        return new JwtAuthenticationResponse(jwt);
    }
}