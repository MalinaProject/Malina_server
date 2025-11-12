package project.malina.Services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project.malina.Repository.UserRepository;
import project.malina.Security.Role;
import project.malina.Security.User;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger log = LogManager.getLogger(UserService.class);
    private final UserRepository repository;

    /**
     * Сохранение пользователя
     *
     * @return сохраненный пользователь
     */
    public User save(User user) {
        log.debug("Сохранение пользователя с именем '{}'", user.getUsername());
        return repository.save(user);
    }
    /**
     * Создание пользователя
     *
     * @return созданный пользователь
     */
    public User create(User user) {

        log.trace("Начато создание пользователя '{}'", user.getUsername());

        if (repository.existsByEmail(user.getEmail())) {
            log.warn("Попытка создать пользователя с уже существующим email '{}'", user.getEmail());
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        if (repository.existsByUsername(user.getUsername())) {
            log.warn("Попытка создать пользователя с уже существующим именем '{}'", user.getUsername());
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }

        try {
            User saved = save(user);
            log.info("Пользователь '{}' успешно создан", user.getUsername());
            return saved;
        } catch (DataAccessException e) {
            log.error("Ошибка доступа к данным при создании пользователя '{}'", user.getUsername(), e);
            throw e;
        } catch (RuntimeException e) {
            log.fatal("Фатальная ошибка при создании пользователя '{}'", user.getUsername(), e);
            throw e;
        }
    }

    /**
     * Получение пользователя по имени пользователя
     *
     * @return пользователь
     */
    public User getByUsername(String username) {
        log.debug("Поиск пользователя по имени '{}'", username);
        return repository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Пользователь '{}' не найден", username);
                    return new UsernameNotFoundException("Пользователь не найден");
                });

    }

    /**
     * Получение пользователя по имени пользователя
     * <p>
     * Нужен для Spring Security
     *
     * @return пользователь
     */
    public UserDetailsService userDetailsService() {
        return this::getByUsername;
    }

    /**
     * Получение текущего пользователя
     *
     * @return текущий пользователь
     */
    public User getCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context != null ? context.getAuthentication() : null;

        if (authentication == null || !authentication.isAuthenticated()) {
            log.fatal("Попытка получения текущего пользователя без аутентификации");
            throw new IllegalStateException("Текущий пользователь не аутентифицирован");
        }

        var username = authentication.getName();
        log.info("Получение текущего пользователя '{}'", username);
        return getByUsername(username);
    }


    /**
     * Выдача прав администратора текущему пользователю
     * <p>
     * Нужен для демонстрации
     */
    @Deprecated
    public void getAdmin() {
        var user = getCurrentUser();
        log.warn("Назначение роли ADMIN пользователю '{}'", user.getUsername());
        user.setRole(Role.ROLE_ADMIN);
        save(user);
    }
}