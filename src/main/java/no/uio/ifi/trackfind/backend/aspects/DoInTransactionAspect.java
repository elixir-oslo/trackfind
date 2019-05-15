package no.uio.ifi.trackfind.backend.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Aspect
@Configurable
public class DoInTransactionAspect {

    private TransactionTemplate transactionTemplate;

    @Around("@annotation(no.uio.ifi.trackfind.backend.annotations.DoInTransaction)")
    public Object doInTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Object result = transactionTemplate.execute(status -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
                status.setRollbackOnly();
                throwable.set(t);
                return null;
            }
        });
        if (throwable.get() != null) {
            throw throwable.get();
        }
        return result;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

}
