package io.erthiscan.data

import io.erthiscan.api.CompaniesResponse
import io.erthiscan.api.CompanyDetail
import io.erthiscan.api.ErthiscanApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * COMPANIES REPOSITORY
 * 
 * ARCHITECTURAL ROLE:
 * Manages the retrieval of company-level data. It acts as an abstraction layer over 
 * the [ErthiscanApi], allowing UI components to fetch company lists and details 
 * without knowing the specifics of the underlying network implementation.
 */
@Singleton
class CompaniesRepository @Inject constructor(
    private val api: ErthiscanApi,
) {
    /**
     * Fetches a paginated list of companies.
     * 
     * @param search Optional query string to filter companies by name or category.
     * @param sort Sorting criteria (e.g., "ethical_score", "name").
     * @param page The current page index for infinite scrolling or pagination.
     * @return [CompaniesResponse] containing the list of items and pagination metadata.
     */
    suspend fun list(search: String, sort: String, page: Int): CompaniesResponse =
        api.getCompanies(search = search, sort = sort, page = page)

    /**
     * Retrieves the complete profile of a single company, including its 
     * ethical score, report history, and nested challenges.
     * 
     * @param id The unique database ID of the company.
     * @return [CompanyDetail] containing the full entity graph.
     */
    suspend fun detail(id: Int): CompanyDetail = api.getCompany(id)
}