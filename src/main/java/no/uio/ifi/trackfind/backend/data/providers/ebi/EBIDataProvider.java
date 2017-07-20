package no.uio.ifi.trackfind.backend.data.providers.ebi;

import lombok.extern.slf4j.Slf4j;
import no.uio.ifi.trackfind.backend.data.providers.AbstractDataProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Slf4j
@Component
public class EBIDataProvider extends AbstractDataProvider {

    private static final String METADATA_URL = "ftp://ftp.ebi.ac.uk/pub/databases/blueprint/releases/20150128/homo_sapiens/hub/hg19/tracksDb.txt";

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Map> fetchData() throws IOException {
        Collection<Map> datasets = new HashSet<>();
        try (Scanner scanner = new Scanner(new URL(METADATA_URL).openStream()).useDelimiter("\n\n")) {
            while (scanner.hasNext()) {
                String entry = scanner.next();
                Optional<String> metadataOptional = Arrays.stream(entry.split("\n")).filter(s -> s.startsWith("    metadata")).findAny();
                if (!metadataOptional.isPresent()) {
                    continue;
                }
                Optional<String> bigDataUrlOptional = Arrays.stream(entry.split("\n")).filter(s -> s.startsWith("    bigDataUrl")).findAny();
                if (!bigDataUrlOptional.isPresent()) {
                    continue;
                }
                String bigDataUrl = bigDataUrlOptional.get().substring(15);
                String metadata = metadataOptional.get().substring(13);
                Map<String, String> dataset = new HashMap<>();
                String[] keyValuePairs = metadata.split(" ");
                for (String keyValuePair : keyValuePairs) {
                    String[] keyValue = keyValuePair.split("=");
                    dataset.put(keyValue[0], keyValue[1]);
                }
                dataset.put(JSON_KEY, this.getClass().getSimpleName());
                dataset.put("bigDataUrl", bigDataUrl);
                datasets.add(dataset);
            }
        }
        return datasets;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getUrlFromDataset(Map dataset) {
        return String.valueOf(dataset.get("bigDataUrl"));
    }

}
