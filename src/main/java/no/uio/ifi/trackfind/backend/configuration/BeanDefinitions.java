package no.uio.ifi.trackfind.backend.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class BeanDefinitions {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private TrackFindProperties properties;

    @Bean
    public Gson gson() {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().setDateFormat(DATE_FORMAT).create();
    }

    @Bean
    public Git git() throws IOException, GitAPIException, InterruptedException {
        FileUtils.deleteDirectory(new File(properties.getArchiveFolder()));

        String gitRemote = properties.getGitRemote();
        File indicesFolder = new File(properties.getIndicesFolder());
        if (!indicesFolder.exists() && StringUtils.isNotEmpty(gitRemote)) {
            log.info("Cloning indices from specified clone-source via Git LFS.");
            log.info("Note: this may take a while depending on the indices size.");
            clone(gitRemote);
            return Git.init().setDirectory(indicesFolder).call();
        }
        log.info("Loading indices Git repo.");
        Git git = Git.init().setDirectory(indicesFolder).call();
        try {
            git.log().call();
            log.info("Git repo loaded successfully.");
        } catch (NoHeadException e) {
            log.info("Indices Git repo doesn't exist.");
            log.info("Tracking all existing indices (if any) and performing initial commit.");
            initRepo(git);
        }
        if (StringUtils.isNotEmpty(gitRemote)) {
            log.info("Adding specified remote.");
            addRemote(git, gitRemote);
            if (properties.isGitAutopush()) {
                log.info("Pushing changes to remote (if any).");
                git.push().setPushTags().setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", properties.getGitToken())).call();
            }
        }
        log.info("Loading complete.");
        return git;
    }

    /**
     * Clones specified remote.
     *
     * @param gitRemote Remote to clone.
     * @throws IOException In case of some filesystem-related error.
     */
    private void clone(String gitRemote) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("git-lfs clone " + gitRemote + " " + properties.getIndicesFolder());
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output;
        while ((output = stdInput.readLine()) != null) {
            log.info(output);
        }
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new InterruptedException("An error occurred during 'git clone'! Check the remote accessibility and/or credentials.");
        }
    }

    /**
     * Initialize new Git repository for storing and versioning indices.
     *
     * @param git JGit instance.
     * @throws IOException     In case of some filesystem-related error.
     * @throws GitAPIException In case of some Git-related error.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initRepo(Git git) throws IOException, GitAPIException {
        try {
            // Create and fill .gitattributes
            File gitAttributesFile = new File(properties.getIndicesFolder() + ".gitattributes");
            FileUtils.write(gitAttributesFile, "*/* filter=lfs diff=lfs merge=lfs -text\n", Charset.defaultCharset());

            // Add all and commit.
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit.").call();
        } catch (Exception e) {
            FileUtils.deleteDirectory(git.getRepository().getDirectory());
            throw e;
        }
    }

    /**
     * Adds remote to the repo.
     *
     * @param git       JGit instance.
     * @param gitRemote URL of remote to add.
     * @throws IOException     In case of some filesystem-related error.
     * @throws GitAPIException In case of some Git-related error.
     */
    private void addRemote(Git git, String gitRemote) throws IOException, GitAPIException {
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", gitRemote);
        config.save();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).
                useDefaultResponseMessages(false).
                select().
                apis(RequestHandlerSelectors.basePackage("no.uio.ifi.trackfind.backend.rest.controllers")).
                paths(PathSelectors.any()).
                build().
                apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("TrackFind",
                "Search engine for finding genomic tracks",
                String.valueOf(getClass().getPackage().getImplementationVersion()),
                null,
                new Contact(null, "https://github.com/elixir-no-nels/trackfind", null),
                null,
                null,
                Collections.emptyList());
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

    @Autowired
    public void setProperties(TrackFindProperties properties) {
        this.properties = properties;
    }

}
