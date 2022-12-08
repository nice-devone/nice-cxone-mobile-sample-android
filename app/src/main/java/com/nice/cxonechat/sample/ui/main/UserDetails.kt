package com.nice.cxonechat.sample.ui.main

data class UserDetails(
    val firstName: String,
    val lastName: String,
    private val customFields: Map<String, String> = emptyMap(),
    val threadCustomFields: Map<String, String> = emptyMap(),
) {
    val chatCustomFields = mapOf(
        "firstname" to firstName,
        "lastname" to lastName,
    ) + customFields
}
