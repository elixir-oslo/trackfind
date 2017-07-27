package no.uio.ifi.trackfind.backend.data.providers.gwas;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fetches data from GWAS Catalog (https://www.ebi.ac.uk/gwas/).
 *
 * @author Dmytro Titov
 */
@Slf4j
@Component
public class GWASDataProvider extends AbstractDataProvider {

    private static final String METADATA_URL = "https://www.ebi.ac.uk/gwas/api/search/downloads/full";

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> datasets = new HashSet<>();
        URL url = new URL(METADATA_URL);
        Reader reader = new InputStreamReader(url.openStream());
        CSVParser parser = new CSVParser(reader, CSVFormat.newFormat('\t').withSkipHeaderRecord());
        List<CSVRecord> records = parser.getRecords();
        Iterator<CSVRecord> recordIterator = records.iterator();
        CSVRecord header = recordIterator.next();
        Map<String, AtomicInteger> index = new HashMap<>();
        while (recordIterator.hasNext()) {
            Map<String, String> dataset = new HashMap<>();
            CSVRecord next = recordIterator.next();
            for (int i = 0; i < header.size(); i++) {
                String attribute = header.get(i);
                String value = next.get(i);
                dataset.put(attribute, value);
                if ("SNPS".equals(attribute)) {
                    int seq = index.computeIfAbsent(value, k -> new AtomicInteger(0)).getAndIncrement();
                    dataset.put("big_data_url", "gwas://" + value + "-" + seq + ".gtrack");
                }
            }
            datasets.add(dataset);
        }
        return datasets;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getUrlFromDataset(Map dataset) {
        return String.valueOf(dataset.get("big_data_url"));
    }

}
