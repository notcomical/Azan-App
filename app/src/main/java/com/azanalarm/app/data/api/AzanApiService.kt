package com.azanalarm.app.data.api

import com.azanalarm.app.data.model.AzanTimesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AzanApiService {
    
    @GET("timings")
    suspend fun getPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2
    ): Response<AzanTimesResponse>
    
    @GET("timings")
    suspend fun getPrayerTimesByDate(
        @Query("timestamp") timestamp: Long,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2
    ): Response<AzanTimesResponse>
}
