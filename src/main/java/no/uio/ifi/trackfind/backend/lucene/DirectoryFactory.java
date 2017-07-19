package no.uio.ifi.trackfind.backend.lucene;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class DirectoryFactory {

    public Directory getDirectory(String dataProviderName) throws IOException {
        return FSDirectory.open(new File(dataProviderName).toPath());
    }

}
