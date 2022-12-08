package com.nice.cxonechat.sample.model

import com.stfalcon.chatkit.commons.models.IUser

data class User(
    private val id: String,
    private val name: String,
    private val avatar: String? = null,
) : IUser {
    override fun getId(): String = id
    override fun getName(): String = name
    override fun getAvatar(): String? = avatar
}
