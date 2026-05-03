package io.erthiscan.data

import io.erthiscan.api.CreateReportRequest
import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.UpdateReportRequest
import io.erthiscan.api.UserProfile
import io.erthiscan.api.VoteRequest
import io.erthiscan.api.VoteResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REPORTS REPOSITORY
 * 
 * ARCHITECTURAL ROLE:
 * Handles the lifecycle of ethical claims (reports) and counter-claims (challenges).
 * It manages the creation, modification, and deletion of content, as well as 
 * the interactive "crowd-sourced" voting logic.
 */
@Singleton
class ReportsRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    /**
     * Submits a new report or challenge.
     * @param req Contains company context, claim text, and source evidence.
     */
    suspend fun createReport(req: CreateReportRequest) = api.createReport(req)

    /**
     * Updates an existing report.
     * WHY: Users may need to refine their claims or add better sources over time.
     */
    suspend fun updateReport(id: Int, req: UpdateReportRequest) = api.updateReport(id, req)

    /**
     * Removes a report. 
     * NOTE: Backend permissions ensure users can only delete their own content.
     */
    suspend fun deleteReport(id: Int) = api.deleteReport(id)

    /**
     * Records a user's vote on a specific claim.
     * 
     * @param reportId The ID of the report being voted on.
     * @param value 1 for 'Ethical'/'True', -1 for 'Unethical'/'False', 0 to remove vote.
     * @return [VoteResponse] reflecting the new aggregated counts.
     */
    suspend fun vote(reportId: Int, value: Int): VoteResponse = api.vote(reportId, VoteRequest(value))

    /**
     * Retrieves the authenticated user's profile, including their 
     * total contribution count and specific report history.
     */
    suspend fun myProfile(): UserProfile = api.getMyProfile()
}