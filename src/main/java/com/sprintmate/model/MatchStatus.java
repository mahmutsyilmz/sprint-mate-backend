package com.sprintmate.model;

/**
 * Enum defining the possible states of a match.
 */
public enum MatchStatus {
    /** Match has been created but not yet started */
    CREATED,
    /** Match is currently active and in progress */
    ACTIVE,
    /** Match has been completed */
    COMPLETED
}
