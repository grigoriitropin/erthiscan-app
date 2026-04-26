package io.erthiscan.data

import io.erthiscan.api.CreateReportRequest
import io.erthiscan.api.ErthiscanApi
import io.erthiscan.api.UpdateReportRequest
import io.erthiscan.api.UserProfile
import io.erthiscan.api.VoteRequest
import io.erthiscan.api.VoteResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    suspend fun createReport(req: CreateReportRequest) = api.createReport(req)
    suspend fun updateReport(id: Int, req: UpdateReportRequest) = api.updateReport(id, req)
    suspend fun deleteReport(id: Int) = api.deleteReport(id)
    suspend fun vote(reportId: Int, value: Int): VoteResponse = api.vote(reportId, VoteRequest(value))
    suspend fun myProfile(): UserProfile = api.getMyProfile()
}