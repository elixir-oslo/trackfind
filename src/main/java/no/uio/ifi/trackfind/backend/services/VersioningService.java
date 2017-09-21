package no.uio.ifi.trackfind.backend.services;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * Service for maintaining versioning of indices. Currently backed by Git.
 *
 * @author Dmytro Titov
 */
@Service
public class VersioningService {

    private Git git;

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
    public void commitAllChanges(Operation operation, String repositoryName) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage(operation.name() + ": " + repositoryName).call();
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
    }

    /**
     * Operation definition.
     */
    public enum Operation {
        CRAWLING, REMAPPING
    }

    @Autowired
    public void setGit(Git git) {
        this.git = git;
    }

}
