package no.uio.ifi.trackfind.backend.services;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VersioningService {

    private Git git;

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

    public enum Operation {
        CRAWLING, REMAPPING
    }

    @Autowired
    public void setGit(Git git) {
        this.git = git;
    }

}
