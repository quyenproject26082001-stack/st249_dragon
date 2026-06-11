package com.dragon.oc.avatar.creator.core.service
import com.dragon.oc.avatar.creator.data.model.PartAPI
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("/api/app/ST266_School_Girl_Creator_2")
    suspend fun getAllData(): Response<Map<String, List<PartAPI>>>
}