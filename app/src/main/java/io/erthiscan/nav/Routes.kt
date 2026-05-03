package io.erthiscan.nav

import kotlinx.serialization.Serializable

/**
 * NAVIGATION ROUTES
 * 
 * ARCHITECTURAL ROLE:
 * Defines the type-safe navigation graph for the entire application. 
 * Using Kotlin Serialization with Navigation Compose ensures that parameters 
 * are correctly encoded/decoded without manual Bundle string manipulation.
 */
sealed interface Route {

    /** Home screen showing the filterable list of companies. */
    @Serializable data object Companies : Route

    /** Barcode scanner screen. */
    @Serializable data object Scan : Route

    /** Main profile overview for the logged-in user. */
    @Serializable data object Profile : Route

    /** Sub-view for the user's primary reports. */
    @Serializable data object ProfileReports : Route

    /** Sub-view for the user's challenges (nested reports). */
    @Serializable data object ProfileChallenges : Route

    /**
     * Company Detail Screen.
     * @param companyId The database ID of the company to display.
     * @param scrollToReportId Optional ID to deep-link directly to a specific claim.
     */
    @Serializable data class Company(val companyId: Int, val scrollToReportId: Int? = null) : Route

    /**
     * Report/Challenge Editor Screen.
     * 
     * WHY MULTI-PURPOSE: 
     * This single route handles creation, challenges, and edits to reduce 
     * code duplication across the UI and ViewModel layers.
     * 
     * @param companyId Contextual company ID.
     * @param companyName Display name for the header.
     * @param parentId If non-null, this is a "Challenge" for a specific report.
     * @param editReportId If non-null, we are editing an existing entry.
     * @param initialText Pre-filled text for Edit flows.
     * @param initialSource Pre-filled URL for Edit flows.
     */
    @Serializable data class CreateReport(
        val companyId: Int,
        val companyName: String,
        val parentId: Int? = null,
        val editReportId: Int? = null,
        val initialText: String = "",
        val initialSource: String = "",
    ) : Route
}