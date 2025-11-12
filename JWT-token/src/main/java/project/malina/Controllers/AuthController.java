package project.malina.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.malina.JwtAuthenticationResponse;
import project.malina.Services.AuthenticationService;
import project.malina.SignInRequest;
import project.malina.SignUpRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация")
public class AuthController {
    private static final Logger LOG = LogManager.getLogger(AuthController.class);
    private final AuthenticationService authenticationService;

    @Operation(summary = "Регистрация пользователя")
    @PostMapping("/sign-up")
    public JwtAuthenticationResponse signUp(@RequestBody @Valid final SignUpRequest request) {
        LOG.info("Запрос регистрации для пользователя '{}'", request.getUsername());
        return authenticationService.signUp(request);
    }

    @Operation(summary = "Авторизация пользователя")
    @PostMapping("/sign-in")
    public JwtAuthenticationResponse signIn(@RequestBody @Valid final SignInRequest request) {
        LOG.info("Запрос авторизации для пользователя '{}'", request.getUsername());
        return authenticationService.signIn(request);
    }
}