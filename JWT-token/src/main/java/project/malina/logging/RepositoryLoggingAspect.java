package project.malina.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryLoggingAspect {
    private static final Logger log = LogManager.getLogger(RepositoryLoggingAspect.class);

    @Around("within(@org.springframework.stereotype.Repository *)")
    public Object logRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodSignature = joinPoint.getSignature().toShortString();
        log.debug("Вызов метода репозитория {}", methodSignature);
        try {
            Object result = joinPoint.proceed();
            log.trace("Метод репозитория {} выполнен успешно", methodSignature);
            return result;
        } catch (IllegalArgumentException ex) {
            log.warn("Некорректные аргументы при вызове {}", methodSignature, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Ошибка при выполнении {}", methodSignature, ex);
            throw ex;
        }
    }
}
