package com.nice.cxonechat.sample.domain

import com.nice.cxonechat.ChatThreadHandler
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
internal class SelectedThreadRepository @Inject constructor() {

    var chatThreadHandler: ChatThreadHandler? = null

}
