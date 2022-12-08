package com.nice.cxonechat.sample.ui.config

import com.nice.cxonechat.sample.storage.Environment

internal object CustomEnvironments {
    val EU_QA1 = Environment(
        name = "EU_QA1",
        location = "Europe",
        baseUrl = "https://channels-eu1-qa.brandembassy.com/",
        socketUrl = "wss://chat-gateway-eu1-qa.brandembassy.com",
        originHeader = "https://livechat-eu1-qa.brandembassy.com/",
        chatUrl = "https://channels-eu1-qa.brandembassy.com/chat/"
    )
}
