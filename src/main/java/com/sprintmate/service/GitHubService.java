package com.sprintmate.service;

import com.sprintmate.exception.ReadmeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for fetching content from GitHub repositories.
 *
 * Business Intent:
 * Fetches README.md files from public GitHub repositories for AI sprint review.
 * Tries main branch first, then falls back to master branch.
 */
@Service
@Slf4j
public class GitHubService {

    private final RestClient restClient;

    private static final String RAW_GITHUB_URL = "https://raw.githubusercontent.com";
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https?://github\\.com/([^/]+)/([^/]+)/?.*$"
    );

    public GitHubService() {
        this.restClient = RestClient.builder()
                .defaultHeader("Accept", "text/plain")
                .defaultHeader("User-Agent", "SprintMate/1.0")
                .build();
    }

    /**
     * Fetches the README.md content from a GitHub repository.
     * Tries main branch first, then falls back to master branch.
     *
     * @param repoUrl The GitHub repository URL (e.g., https://github.com/user/repo)
     * @return The README.md content as a string
     * @throws ReadmeNotFoundException if README cannot be fetched from either branch
     * @throws IllegalArgumentException if the URL format is invalid
     */
    @Retryable(
            retryFor = {HttpClientErrorException.TooManyRequests.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String fetchReadme(String repoUrl) {
        log.info("Fetching README from repository: {}", repoUrl);

        // Parse the repository URL
        Matcher matcher = GITHUB_URL_PATTERN.matcher(repoUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + repoUrl);
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        // Remove .git suffix if present
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }

        // Try main branch first
        String readme = tryFetchReadme(owner, repo, "main");
        if (readme != null) {
            log.info("Successfully fetched README from main branch for {}/{}", owner, repo);
            return readme;
        }

        // Fallback to master branch
        readme = tryFetchReadme(owner, repo, "master");
        if (readme != null) {
            log.info("Successfully fetched README from master branch for {}/{}", owner, repo);
            return readme;
        }

        log.warn("README.md not found in repository {}/{} on main or master branch", owner, repo);
        throw new ReadmeNotFoundException(owner, repo);
    }

    /**
     * Attempts to fetch README.md from a specific branch.
     *
     * @param owner  Repository owner
     * @param repo   Repository name
     * @param branch Branch name
     * @return README content or null if not found
     */
    private String tryFetchReadme(String owner, String repo, String branch) {
        String url = String.format("%s/%s/%s/%s/README.md", RAW_GITHUB_URL, owner, repo, branch);

        try {
            String content = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            return content;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("README.md not found at {} for {}/{}", branch, owner, repo);
            return null;
        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Access forbidden for {}/{} - repository may be private", owner, repo);
            return null;
        } catch (Exception e) {
            log.error("Error fetching README from {} branch for {}/{}: {}", branch, owner, repo, e.getMessage());
            return null;
        }
    }
}
