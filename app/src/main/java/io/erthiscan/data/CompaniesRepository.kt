package io.erthiscan.data

import io.erthiscan.api.CompaniesResponse
import io.erthiscan.api.CompanyDetail
import io.erthiscan.api.ErthiscanApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompaniesRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    suspend fun list(search: String, sort: String, page: Int): CompaniesResponse =
        api.getCompanies(search = search, sort = sort, page = page)

    suspend fun detail(id: Int): CompanyDetail = api.getCompany(id)
}