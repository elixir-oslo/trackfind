package no.uio.ifi.trackfind.backend.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.EntryLoader;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import no.uio.ifi.trackfind.backend.configuration.TrackFindProperties;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service for maintaining versioning of indices. Currently backed by Git.
 *
 * @author Dmytro Titov
 */
@Slf4j
@Service
public class VersioningService {

    private TrackFindProperties properties;
    private TrackFindService trackFindService;
    private Git git;

    /**
     * Cache for Git revisions.
     * Each entry lives specified amount of time after last access.
     * After expiration we remove the entry from cache, close IndexReader and delete temp folders.
     */
    private Map<String, Revision> cache = ExpiringMap.builder().
            expirationPolicy(ExpirationPolicy.ACCESSED).
            expiration(10, TimeUnit.SECONDS).
            expirationListener(new RevisionCacheExpirationListener()).
            entryLoader(new RevisionCacheEntryLoader()).
            build();

    /**
     * Gets IndexSearcher instance for specified Git revision and DataProvider.
     *
     * @param revision         Revision to checkout.
     * @param dataProviderName DataProvider to return IndexSearcher for.
     * @return Apache Lucene IndexSearcher.
     * @throws ExecutionException In case of some error.
     */
    public IndexSearcher getIndexSearcher(String revision, String dataProviderName) throws ExecutionException {
        return cache.get(revision).index.get(dataProviderName).getValue();
    }

    /**
     * Checks out revision from local Git repo, puts it to temporary folder.
     *
     * @param revision Hash of revision to checkout.
     * @return Path to created temp folder.
     * @throws IOException In case of error during folder creation.
     */
    private String checkout(String revision) throws IOException {
        String revisionFolderPath = properties.getArchiveFolder() + revision + File.separator;
        File revisionFolder = new File(revisionFolderPath);
        FileUtils.copyDirectory(new File(properties.getIndicesFolder()), revisionFolder);
        try {
            Git git = Git.init().setDirectory(revisionFolder).call();
            git.checkout().setName(revision).call();
        } catch (GitAPIException e) {
            FileUtils.deleteDirectory(revisionFolder);
            throw new IOException("No such revision: " + revision);
        }
        return revisionFolderPath;
    }

    /**
     * Gets the last committed revision of the current branch.
     *
     * @return The last committed revision of the current branch
     * @throws GitAPIException In case of Git error.
     */
    public String getCurrentRevision() throws GitAPIException {
        Iterable<RevCommit> log = git.log().call();
        Iterator<RevCommit> iterator = log.iterator();
        RevCommit lastRevCommit = null;
        while (iterator.hasNext()) {
            lastRevCommit = iterator.next();
        }
        return lastRevCommit == null ? null : lastRevCommit.getId().getName();
    }

    /**
     * Commit everything.
     *
     * @param operation      Operation to commit.
     * @param repositoryName Name of changed repo to use in commit message.
     * @throws GitAPIException In case of Git error.
     */
    public void commit(Operation operation, String repositoryName) throws GitAPIException {
        log.info("Committing changes. Git repo folder: properties.getIndicesFolder()");
        git.add().addFilepattern(properties.getIndicesFolder() + repositoryName).call();
        git.commit().setAll(true).setMessage(operation.name() + ": " + repositoryName).call();
        push(false);
    }

    /**
     * Tag current revision (HEAD, hopefully).
     *
     * @param repositoryName Name of changed repo to use in tag name.
     * @throws GitAPIException In case of Git error.
     */
    public void tag(String repositoryName) throws GitAPIException {
        List<Ref> tags = git.tagList().call();
        git.tag().setName(CollectionUtils.size(tags) + "." + repositoryName).call();
        push(true);
    }

    /**
     * Pulls all indices from the remote.
     *
     * @throws GitAPIException In case of Git error.
     */
    public void pull() throws GitAPIException {
        git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", properties.getGitToken())).call();
    }

    /**
     * Pushes changes to remote regardless depending on 'autopush' flag.
     *
     * @param pushTags Flag specifying whether to push tags or not.
     * @throws GitAPIException In case of Git error.
     */
    private void push(boolean pushTags) throws GitAPIException {
        if (properties.isGitAutopush()) {
            forcePush(pushTags);
        }
    }

    /**
     * Pushes changes to remote regardless of 'autopush' flag.
     *
     * @param pushTags Flag specifying whether to push tags or not.
     * @throws GitAPIException In case of Git error.
     */
    private void forcePush(boolean pushTags) throws GitAPIException {
        PushCommand pushCommand = git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", properties.getGitToken()));
        if (pushTags) {
            pushCommand.setPushTags();
        }
        pushCommand.call();
    }

    /**
     * Operation definition.
     */
    public enum Operation {
        CRAWLING, REMAPPING
    }

    /**
     * Listener to expiration event of revisions cache. Closes IndexReaders and removes temp folders.
     */
    private class RevisionCacheExpirationListener implements ExpirationListener<String, Revision> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void expired(String key, Revision revision) {
            try {
                for (Pair<IndexReader, IndexSearcher> pair : revision.index.values()) {
                    pair.getKey().close();
                }
                FileUtils.deleteDirectory(new File(properties.getArchiveFolder() + key));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Lazy-loader for populating revisions cache.
     */
    private class RevisionCacheEntryLoader implements EntryLoader<String, Revision> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Revision load(String key) {
            Revision revision = new Revision();
            try {
                for (DataProvider dataProvider : trackFindService.getDataProviders()) {
                    String dataProviderName = dataProvider.getName();
                    Directory directory = FSDirectory.open(new File(checkout(key) + dataProviderName).toPath());
                    IndexReader indexReader = DirectoryReader.open(directory);
                    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
                    revision.index.put(dataProviderName, new ImmutablePair<>(indexReader, indexSearcher));
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return revision;
        }
    }

    /**
     * POJO for describing map: commit name -> IndexReader & IndexSearcher.
     */
    @Data
    private class Revision {
        private Map<String, Pair<IndexReader, IndexSearcher>> index = new HashMap<>();
    }

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

    @Autowired
    public void setTrackFindService(TrackFindService trackFindService) {
        this.trackFindService = trackFindService;
    }

    @Autowired
    public void setGit(Git git) {
        this.git = git;
    }

}
