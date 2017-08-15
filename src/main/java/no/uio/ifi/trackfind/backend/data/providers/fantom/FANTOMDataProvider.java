package no.uio.ifi.trackfind.backend.data.providers.fantom;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches data from FANTOM (http://fantom.gsc.riken.jp/).
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
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> result = new HashSet<>();
        log.info("Collecting directories...");
        Document root = Jsoup.parse(new URL(METADATA_URL), 10000);
        Set<String> dirs = root.getElementsByTag("a").stream().map(e -> e.attr("href")).filter(s -> s.contains(".") && s.endsWith("/")).collect(Collectors.toSet());
        log.info(dirs.size() + " directories collected");
        for (String dir : dirs) {
            log.info("Processing directory: " + dir);
            Document folder = Jsoup.parse(new URL(METADATA_URL + dir), 10000);
            Set<String> allFiles = folder.getElementsByTag("a").stream().map(e -> e.attr("href")).collect(Collectors.toSet());
            Optional<String> metadataFileOptional = allFiles.stream().filter(s -> s.endsWith("_sdrf.txt")).findAny();
            if (!metadataFileOptional.isPresent()) {
                continue;
            }
            URL url = new URL(METADATA_URL + dir + metadataFileOptional.get());
            try (Reader reader = new InputStreamReader(url.openStream());
                 CSVParser parser = new CSVParser(reader, CSVFormat.newFormat('\t').withSkipHeaderRecord())) {
                List<CSVRecord> records = parser.getRecords();
                Iterator<CSVRecord> recordIterator = records.iterator();
                CSVRecord header = recordIterator.next();
                String[] attributes = parseAttributes(header);
                while (recordIterator.hasNext()) {
                    Map<String, Object> dataset = new HashMap<>();
                    CSVRecord next = recordIterator.next();
                    for (int i = 0; i < attributes.length; i++) {
                        if (!attributes[i].startsWith("skip")) {
                            dataset.put(attributes[i], next.get(i));
                        }
                    }
                    Map<String, Collection<String>> browser = new HashMap<>();
                    Set<String> datasetRelatedFiles = allFiles.stream().filter(s -> s.contains(next.get(0))).collect(Collectors.toSet());
                    for (String datasetRelatedFile : datasetRelatedFiles) {
                        browser.computeIfAbsent(getDataType(datasetRelatedFile), k -> new HashSet<>()).add(METADATA_URL + dir + datasetRelatedFile);
                    }
                    dataset.put(BROWSER, browser);
                    result.add(dataset);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    private String getDataType(String fileName) {
        if (fileName.endsWith(".bam")) {
            return "bam";
        }
        if (fileName.endsWith(".bam.bai")) {
            return "bam_bai";
        }
        if (fileName.endsWith(".ctss.bed.gz")) {
            return "cage_tss";
        }
        if (fileName.endsWith(".rdna.fa.gz")) {
            return "fasta";
        }
        return null;
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

}
