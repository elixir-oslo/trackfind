package no.uio.ifi.trackfind.backend.lucene;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Factory providing Directories for DataProviders.
 *
 * @author Dmytro Titov
 */
@Component
public class DirectoryFactory {

    /**
     * Gets the Directory for particular DataProvider.
     *
     * @param dataProviderName Name of the DataProvider.
     * @return Lucene Directory (Index). The default one is FSDirectory (stored on hard-drive). Tests use RAMDirectory (inmemory).
     * @throws IOException In case Directory initialization fails.
     */
    public Directory getDirectory(String dataProviderName) throws IOException {
        return FSDirectory.open(new File(dataProviderName).toPath());
    }

}
