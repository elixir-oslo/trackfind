package no.uio.ifi.trackfind.backend.configuration;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.uio.ifi.trackfind.TrackFindApplication.INDICES_FOLDER;

@Configuration
public class BeanDefinitions {

    @Bean
    public Git git() throws IOException, GitAPIException {
        Git git = Git.init().setDirectory(new File(INDICES_FOLDER)).call();
        try {
            git.log().call();
        } catch (NoHeadException e) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit.").call();
        }
        return git;
    }

    @Bean
    public ExecutorService workStealingPool() {
        return Executors.newWorkStealingPool(10);
    }

    @Bean
    public ExecutorService singleThreadExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(4);
    }

}
