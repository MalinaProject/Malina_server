package project.malina.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.malina.Services.UserService;

@RestController
@RequestMapping("/example")
@RequiredArgsConstructor
@Tag(name = "Аутентификация")
public class ExampleController {
    private static final Logger LOG = LogManager.getLogger(ExampleController.class);
    private final UserService service;

    @GetMapping
    @Operation(summary = "Доступен только авторизованным пользователям")
    public String example() {
        LOG.debug("Выполнен запрос к открытому примеру");
        return "Hello, world!";
    }

    @GetMapping("/admin")
    @Operation(summary = "Доступен только авторизованным пользователям с ролью ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    public String exampleAdmin() {
        LOG.info("Запрос к ресурсу администратора");
        return "Hello, admin!";
    }

    @GetMapping("/get-admin")
    @Operation(summary = "Получить роль ADMIN (для демонстрации)")
    public void getAdmin() {
        LOG.warn("Назначение роли ADMIN текущему пользователю");
        service.getAdmin();
    }
}