package no.uio.ifi.trackfind.backend.data.providers;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

import static no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider.JSON_KEY;

@Aspect
@Component
public class DataProviderJsonKeyAspect {

    @SuppressWarnings("unchecked")
    @Around("execution(* no.uio.ifi.trackfind.backend.data.providers.DataProvider.fetchData())")
    public Object addDataProviderJsonKey(ProceedingJoinPoint jointPoint) throws Throwable {
        Collection<Map> result = (Collection<Map>) jointPoint.proceed();
        for (Map map : result) {
            map.put(JSON_KEY, jointPoint.getThis().getClass().getSimpleName());
        }
        return result;
    }

}
