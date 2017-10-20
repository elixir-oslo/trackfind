package no.uio.ifi.trackfind.backend.data.providers;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Extension for AbstractDataProvider that provides some common pagination handling.
 *
 * @author Dmytro Titov
 */
@Slf4j
public abstract class PaginationAwareDataProvider extends AbstractDataProvider {

    /**
     * Specifies amount of entries to fetch at once.
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
     * Fetches all pages.
     *
     * @param indexWriter       Handler to write to te Lucene Index.
     * @param urlWithPagination Paginated API endpoint.
     * @param urlWithPage       Paginated API endpoint with actual data.
     * @param pageClass         Implementation of {@link Page} interface.
     * @param <T>               Implements {@link Page} interface.
     * @throws Exception In case if something goes wrong.
     */
    protected <T extends Page> void fetchPages(IndexWriter indexWriter, String urlWithPagination, String urlWithPage, Class<T> pageClass) throws Exception {
        int pagesTotal = getPagesTotal(urlWithPagination, pageClass);
        if (pagesTotal == 0) {
            return;
        }
        log.info(pagesTotal * getEntriesPerPage() + " entries available.");
        log.info("Pages total: " + pagesTotal);
        CountDownLatch countDownLatch = new CountDownLatch(pagesTotal);
        for (int i = 0; i < pagesTotal; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    Collection<Map> page = fetchPage(urlWithPage, pageClass, finalI);
                    indexWriter.addDocuments(page.parallelStream().map(this::postprocessDataset).map(mapToDocumentConverter).collect(Collectors.toSet()));
                    log.info("Page " + finalI + " processed.");
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
    }

    /**
     * Fetches particular page.
     *
     * @param urlWithPage Paginated API endpoint.
     * @param pageClass   Implementation of {@link Page} interface.
     * @param pageNumber  Which page to fetch.
     * @param <T>         Implements {@link Page} interface.
     * @return Pagination-aware fetched data.
     * @throws IOException In case if something goes wrong.
     */
    protected <T extends Page> Collection<Map> fetchPage(String urlWithPage, Class<T> pageClass, int pageNumber) throws Exception {
        Collection<Map> page = new HashSet<>();
        try {
            URL url = new URL(urlWithPage + "from=" + pageNumber * getEntriesPerPage() + "&size=" + getEntriesPerPage());
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000);
            try (InputStream inputStream = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                page.addAll(gson.fromJson(reader, pageClass).getEntries());
            }
            postProcessPage(page);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return page;
    }

    /**
     * Page post-processing.
     *
     * @param page Fetched page to process.
     */
    protected abstract void postProcessPage(Collection<Map> page);

}
