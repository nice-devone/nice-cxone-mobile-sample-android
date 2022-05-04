package com.customerdynamics.sdk.custom.holders;

import android.view.View;
import android.widget.TextView;

import com.customerdynamics.sdk.R;
import com.customerdynamics.sdk.model.Message;
import com.stfalcon.chatkit.messages.MessageHolders;

/*
 * Created by troy379 on 05.04.17.
 */
public class OutcomingTextAndButtonsMessageViewHolder
        extends MessageHolders.OutcomingTextMessageViewHolder<Message> {

    private TextView tvDuration;
    private TextView tvTime;

    public OutcomingTextAndButtonsMessageViewHolder(View itemView, Object payload) {
        super(itemView, payload);
        tvDuration = itemView.findViewById(R.id.duration);
        tvTime = itemView.findViewById(R.id.time);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);
        tvDuration.setText("12min.");
        tvTime.setText("custom plugin");
    }
}
