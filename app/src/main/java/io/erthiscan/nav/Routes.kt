package io.erthiscan.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Companies : Route
    @Serializable data object Scan : Route
    @Serializable data object Profile : Route
    @Serializable data object ProfileReports : Route
    @Serializable data object ProfileChallenges : Route
    @Serializable data class Company(val companyId: Int, val scrollToReportId: Int? = null) : Route
    @Serializable data class CreateReport(
        val companyId: Int,
        val companyName: String,
        val parentId: Int? = null,
        val editReportId: Int? = null,
        val initialText: String = "",
        val initialSource: String = "",
    ) : Route
}