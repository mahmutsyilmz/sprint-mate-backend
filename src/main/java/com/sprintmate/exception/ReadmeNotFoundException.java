package com.sprintmate.exception;

/**
 * Exception thrown when a README.md cannot be found in a GitHub repository.
 *
 * Business Intent:
 * Indicates that the sprint review cannot be completed because the
 * submitted repository either doesn't have a README.md or is not accessible.
 */
public class ReadmeNotFoundException extends RuntimeException {

    private final String owner;
    private final String repo;

    public ReadmeNotFoundException(String owner, String repo) {
        super(String.format("README.md not found in repository %s/%s. " +
                "Please ensure the repository is public and contains a README.md file.", owner, repo));
        this.owner = owner;
        this.repo = repo;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }
}
