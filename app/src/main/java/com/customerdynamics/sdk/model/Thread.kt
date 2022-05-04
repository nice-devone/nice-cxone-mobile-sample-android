package com.customerdynamics.sdk.model

import java.util.*

data class Thread(
    var id: UUID,
    var username: String,
    var message: String,
    var agentImage: String
)
