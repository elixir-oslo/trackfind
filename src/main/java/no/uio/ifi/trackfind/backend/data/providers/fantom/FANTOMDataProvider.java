package no.uio.ifi.trackfind.backend.data.providers.fantom;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Fetches data from <a href="http://fantom.gsc.riken.jp/">FANTOM</a>.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class FANTOMDataProvider extends AbstractDataProvider {

    private static final String METADATA_URL = "http://fantom.gsc.riken.jp/5/datafiles/latest/basic/";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchData() throws Exception {
        log.info("Collecting directories...");
        Document root = Jsoup.parse(new URL(METADATA_URL), 10000);
        Set<String> dirs = root.getElementsByTag("a").parallelStream().map(e -> e.attr("href")).filter(s -> s.contains(".") && s.endsWith("/")).collect(Collectors.toSet());
        int size = dirs.size();
        log.info(size + " directories to process");
        CountDownLatch countDownLatch = new CountDownLatch(size);
        Collection<Map> allDatasets = new HashSet<>();
        for (String dir : dirs) {
            executorService.submit(() -> {
                try {
                    Document folder = Jsoup.parse(new URL(METADATA_URL + dir), 10000);
                    Set<String> allFiles = folder.getElementsByTag("a").parallelStream().map(e -> e.attr("href")).collect(Collectors.toSet());
                    Optional<String> metadataFileOptional = allFiles.parallelStream().filter(s -> s.endsWith("_sdrf.txt")).findAny();
                    if (!metadataFileOptional.isPresent()) {
                        return;
                    }
                    URL url = new URL(METADATA_URL + dir + metadataFileOptional.get());
                    try (InputStream inputStream = url.openStream();
                         Reader reader = new InputStreamReader(inputStream);
                         CSVParser parser = new CSVParser(reader, CSVFormat.newFormat('\t').withSkipHeaderRecord())) {
                        List<CSVRecord> records = parser.getRecords();
                        Iterator<CSVRecord> recordIterator = records.iterator();
                        CSVRecord header = recordIterator.next();
                        String[] attributes = parseAttributes(header);
                        while (recordIterator.hasNext()) {
                            Map<String, Object> map = new HashMap<>();
                            CSVRecord next = recordIterator.next();
                            for (int i = 0; i < attributes.length; i++) {
                                if (!attributes[i].startsWith("skip")) {
                                    map.put(attributes[i], next.get(i));
                                }
                            }

                            Set<String> datasetRelatedFiles = allFiles.parallelStream().filter(s -> s.contains(next.get(0))).map(f -> METADATA_URL + dir + f).collect(Collectors.toSet());
                            map.put("files", datasetRelatedFiles);
                            allDatasets.add(map);
                        }
                        log.info("Directory " + dir + " processed.");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        save(allDatasets);
        log.info(size + " releases stored.");
    }

    /**
     * Parses FANTOM5 attributes, skipping recurrent attributes and renaming some others.
     *
     * @param header TSV header
     * @return Array with attributes.
     */
    private String[] parseAttributes(CSVRecord header) {
        String[] attributes = new String[header.size()];
        for (int i = 0; i < header.size(); i++) {
            if (!header.get(i).contains("[")) {
                attributes[i] = "skip" + i;
                continue;
            }
            attributes[i] = header.get(i).replace("Comment [", "").replace("Parameter [", "").replace("]", "");
        }
        return attributes;
    }

    @Autowired
    @Override
    public void setExecutorService(ExecutorService fixedThreadPool) {
        this.executorService = fixedThreadPool;
    }

}
