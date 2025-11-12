package project.malina;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public final class MalinaApplication {
    private static final Logger LOG = LogManager.getLogger(MalinaApplication.class);

    private MalinaApplication() {
        // Utility class constructor
    }

    public static void main(final String[] args) {
        LOG.info("Запуск приложения Malina");
        SpringApplication.run(MalinaApplication.class, args);
    }
}
