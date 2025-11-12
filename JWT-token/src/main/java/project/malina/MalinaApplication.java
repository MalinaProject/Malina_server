package project.malina;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MalinaApplication {
        private static final Logger log = LogManager.getLogger(MalinaApplication.class);

        public static void main(String[] args) {
                log.info("Запуск приложения Malina");
                SpringApplication.run(MalinaApplication.class, args);
        }

}
