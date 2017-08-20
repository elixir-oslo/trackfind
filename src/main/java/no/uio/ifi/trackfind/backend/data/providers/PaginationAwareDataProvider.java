package no.uio.ifi.trackfind.backend.data.providers;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Extension for AbstractDataProvider that provides some common pagination handling.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class PaginationAwareDataProvider extends AbstractDataProvider {

    protected Gson gson;

    /**
     * Specifies amount of entries to fetch at once.
     *
     * @return Entries per page quantity.
     */
    protected abstract long getEntriesPerPage();

    /**
     * Get total amount of pages for given API endpoint.
     *
     * @param urlWithPagination Paginated API endpoint.
     * @param pageClass         Implementation of {@link Page} interface.
     * @param <T>               Implements {@link Page} interface.
     * @return Total count of pages for specified URL.
     * @throws IOException In case if something goes wrong.
     */
    protected <T extends Page> int getPagesTotal(String urlWithPagination, Class<T> pageClass) throws IOException {
        int pagesTotal;
        try (InputStream inputStream = new URL(urlWithPagination + "from=0&size=" + getEntriesPerPage()).openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            pagesTotal = gson.fromJson(reader, pageClass).getPagesTotal();
        }
        return pagesTotal;
    }

    /**
     * @param urlWithPagination Paginated API endpoint.
     * @param pageClass         Implementation of {@link Page} interface.
     * @param pagesTotal        Total amount of pages for given API endpoint got from {@link #getPagesTotal(String, Class)} method.
     * @param <T>               Implements {@link Page} interface.
     * @return Pagination-aware fetched data.
     * @throws IOException In case if something goes wrong.
     */
    protected <T extends Page> Collection<Map> fetchPaginatedEntries(String urlWithPagination, Class<T> pageClass, int pagesTotal) throws Exception {
        Collection<Map> result = new HashSet<>();
        CountDownLatch countDownLatch = new CountDownLatch(pagesTotal);
        for (int i = 0; i < pagesTotal; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    URL url = new URL(urlWithPagination + "from=" + finalI * getEntriesPerPage() + "&size=" + getEntriesPerPage());
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(30000);
                    try (InputStream inputStream = connection.getInputStream();
                         InputStreamReader reader = new InputStreamReader(inputStream)) {
                        result.addAll(gson.fromJson(reader, pageClass).getEntries());
                        log.info("Page " + finalI + " processed.");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        return result;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
