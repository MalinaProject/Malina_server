package project.malina.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import project.malina.Repository.UserRepository;
import project.malina.Security.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Сохранение пользователя делегирует вызов репозиторию")
    void saveDelegatesToRepository() {
        User user = User.builder().username("john").build();
        when(repository.save(user)).thenReturn(user);

        User saved = userService.save(user);

        assertThat(saved).isEqualTo(user);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Создание нового пользователя сохраняет сущность при отсутствии дубликатов")
    void createUserWhenUnique() {
        User user = User.builder()
                .username("unique")
                .email("unique@example.com")
                .build();

        when(repository.existsByEmail(user.getEmail())).thenReturn(false);
        when(repository.existsByUsername(user.getUsername())).thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        User created = userService.create(user);

        assertThat(created).isEqualTo(user);
        verify(repository).save(user);
    }

    @ParameterizedTest
    @CsvSource({
        "duplicate@example.com, uniqueUser",
        "duplicate@example.com, anotherUser"
    })
    @DisplayName("Создание пользователя с существующим email выбрасывает исключение")
    void createUserWhenEmailExists(final String email, final String username) {
        User user = User.builder()
                .username(username)
                .email(email)
                .build();

        when(repository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пользователь с таким email уже существует");

        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Создание пользователя с существующим именем выбрасывает исключение")
    void createUserWhenUsernameExists() {
        User user = User.builder()
                .username("duplicate")
                .email("unique@example.com")
                .build();

        when(repository.existsByEmail(user.getEmail())).thenReturn(false);
        when(repository.existsByUsername(user.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пользователь с таким именем уже существует");

        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Получение пользователя по имени возвращает найденного пользователя")
    void getByUsernameWhenFound() {
        User user = User.builder().username("john").build();
        when(repository.findByUsername("john")).thenReturn(Optional.of(user));

        User result = userService.getByUsername("john");

        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("Получение пользователя по имени выбрасывает исключение, если пользователь не найден")
    void getByUsernameWhenNotFound() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("missing"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .hasMessage("Пользователь не найден");
    }

    @Nested
    class CurrentUser {

        @Test
        @DisplayName("Получение текущего пользователя возвращает пользователя при наличии аутентификации")
        void getCurrentUserWhenAuthenticated() {
            User user = User.builder().username("authUser").build();
            SecurityContext context = Mockito.mock(SecurityContext.class);
            TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken("authUser", "password");
            authenticationToken.setAuthenticated(true);

            when(context.getAuthentication()).thenReturn(authenticationToken);
            SecurityContextHolder.setContext(context);
            when(repository.findByUsername("authUser")).thenReturn(Optional.of(user));

            User currentUser = userService.getCurrentUser();

            assertThat(currentUser).isEqualTo(user);
        }

        @Test
        @DisplayName("Получение текущего пользователя без аутентификации выбрасывает исключение")
        void getCurrentUserWithoutAuthentication() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Текущий пользователь не аутентифицирован");
        }
    }
}
