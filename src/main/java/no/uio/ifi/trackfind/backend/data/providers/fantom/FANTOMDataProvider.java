package no.uio.ifi.trackfind.backend.data.providers.fantom;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches data from FANTOM (http://fantom.gsc.riken.jp/).
 */
@Slf4j
@Component
public class FANTOMDataProvider extends AbstractDataProvider {

    private static final String METADATA_URL = "http://fantom.gsc.riken.jp/5/datafiles/latest/basic/";

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> datasets = new HashSet<>();
        Document root = Jsoup.parse(new URL(METADATA_URL), 5000);
        Set<String> dirs = root.getElementsByTag("a").stream().map(e -> e.attr("href")).filter(s -> s.startsWith("human.") && s.endsWith("/")).collect(Collectors.toSet());
        for (String dir : dirs) {
            Document folder = Jsoup.parse(new URL(METADATA_URL + dir), 5000);
            Optional<String> any = folder.getElementsByTag("a").stream().map(e -> e.attr("href")).filter(s -> s.endsWith("_sdrf.txt")).findAny();
            if (!any.isPresent()) {
                continue;
            }
            URL url = new URL(METADATA_URL + dir + any.get());
            Reader reader = new InputStreamReader(url.openStream());
            CSVParser parser = new CSVParser(reader, CSVFormat.newFormat('\t').withSkipHeaderRecord());
            List<CSVRecord> records = parser.getRecords();
            Iterator<CSVRecord> recordIterator = records.iterator();
            CSVRecord header = recordIterator.next();
            String[] attributes = parseAttributes(header);
            while (recordIterator.hasNext()) {
                Map<String, String> dataset = new HashMap<>();
                CSVRecord next = recordIterator.next();
                for (int i = 0; i < attributes.length; i++) {
                    if (!attributes[i].startsWith("skip")) {
                        dataset.put(attributes[i], next.get(i));
                    }
                }
                dataset.put("big_data_url", METADATA_URL + dir + URLEncoder.encode(next.get(attributes.length - 1), Charsets.UTF_8.name()));
                datasets.add(dataset);
            }
        }
        return datasets;
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

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getUrlFromDataset(Map dataset) {
        return String.valueOf(dataset.get("big_data_url"));
    }

}
