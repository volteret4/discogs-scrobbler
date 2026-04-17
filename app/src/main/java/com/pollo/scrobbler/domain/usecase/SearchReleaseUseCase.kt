package com.pollo.scrobbler.domain.usecase

import com.pollo.scrobbler.domain.model.Release
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import javax.inject.Inject

class SearchReleaseUseCase @Inject constructor(
    private val repository: DiscogsRepository
) {
    suspend fun byBarcode(barcode: String): List<Release> = repository.searchByBarcode(barcode)
    suspend fun byQuery(query: String): List<Release> = repository.searchByQuery(query)
    suspend fun getDetail(releaseId: Int): Release = repository.getReleaseDetail(releaseId)
}
