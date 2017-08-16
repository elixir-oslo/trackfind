package no.uio.ifi.trackfind.backend.data.providers;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Extension for AbstractDataProvider that provides some common pagination handling methods and POJOs.
 */
@Slf4j
public abstract class PaginationAwareDataProvider extends AbstractDataProvider {

    protected Gson gson;

    /**
     * Returns how many entries to fetch at once.
     *
     * @return Entries per page quantity.
     */
    protected long getEntriesPerPage() {
        return 100;
    }

    /**
     * Get total amount of pages for given API endpoint.
     *
     * @param urlWithPagination Paginated API endpoint.
     * @param pageClass         Implementation of {@link Page} interface.
     * @param <T>               <T> Class implementing {@link Page} interface.
     * @return Total count of pages for specified URL.
     * @throws IOException
     */
    protected <T extends Page> long getPagesTotal(String urlWithPagination, Class<T> pageClass) throws IOException {
        long pagesTotal;
        try (InputStreamReader reader = new InputStreamReader(new URL(urlWithPagination + "from=0&size=" + getEntriesPerPage()).openStream())) {
            pagesTotal = gson.fromJson(reader, pageClass).getPagesTotal();
        }
        return pagesTotal;
    }

    /**
     * @param urlWithPagination Paginated API endpoint.
     * @param pageClass         Implementation of {@link Page} interface.
     * @param pagesTotal        Total amount of pages for given API endpoint got from {@link #getPagesTotal(String, Class)} method.
     * @param <T>               Class implementing {@link Page} interface.
     * @return Pagination-aware fetched data.
     * @throws IOException
     */
    protected <T extends Page> Collection<Map> fetchPaginatedEntries(String urlWithPagination, Class<T> pageClass, long pagesTotal) throws IOException {
        Collection<Map> result = new HashSet<>();
        for (int i = 0; i < pagesTotal; i++) {
            log.info("Processing page: " + i);
            try (InputStreamReader reader = new InputStreamReader(new URL(urlWithPagination +
                    "from=" + i * getEntriesPerPage() +
                    "&size=" + getEntriesPerPage()).openStream())) {
                result.addAll(gson.fromJson(reader, pageClass).getEntries());
            }
        }
        return result;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}
