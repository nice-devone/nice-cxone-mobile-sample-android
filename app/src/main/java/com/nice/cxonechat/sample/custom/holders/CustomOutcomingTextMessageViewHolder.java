package com.nice.cxonechat.sample.custom.holders;

import android.view.View;
import android.widget.TextView;

import com.nice.cxonechat.sample.R;
import com.nice.cxonechat.sample.model.Message;
import com.stfalcon.chatkit.messages.MessageHolders;

public class CustomOutcomingTextMessageViewHolder
        extends MessageHolders.OutcomingTextMessageViewHolder<Message> {

    private TextView tvStatus;

    public CustomOutcomingTextMessageViewHolder(View itemView, Object payload) {
        super(itemView, payload);

        tvStatus = itemView.findViewById(R.id.messageStatus);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);

        tvStatus.setText(message.getStatus());
    }
}
