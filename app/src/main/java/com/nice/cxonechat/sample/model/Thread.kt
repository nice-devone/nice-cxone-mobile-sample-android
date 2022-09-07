package com.nice.cxonechat.sample.model

import java.util.*

data class Thread(
    var id: UUID,
    var name: String,
    var message: String,
    var agentImage: String
)
