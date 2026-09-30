package com.csm.kitchenguard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Model request dan response untuk API Autentikasi.
 * PRD Section 9 — Authentication & Role Context.
 * Endpoint: POST /api/v1/auth/login
 */

data class LoginRequest(
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("password")    val password: String
)

data class LoginResponse(
    @SerializedName("token")   val token: String,
    @SerializedName("user")    val user: UserDto,
    @SerializedName("station") val station: StationDto
)

data class UserDto(
    @SerializedName("id")          val id: Long,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("name")        val name: String,
    @SerializedName("role")        val role: String
)

data class StationDto(
    @SerializedName("id")   val id: Long,
    @SerializedName("name") val name: String
)
