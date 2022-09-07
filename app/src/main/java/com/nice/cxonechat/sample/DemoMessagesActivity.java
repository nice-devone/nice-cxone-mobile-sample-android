package com.nice.cxonechat.sample;

import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.nice.cxonechat.sample.model.Message;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class DemoMessagesActivity extends AppCompatActivity {
    protected final String senderId = "1";
    protected ImageLoader imageLoader;
    protected MessagesListAdapter<Message> messagesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageLoader = (imageView, url, payload) ->  Glide.with(this)
                .load(url)
                .into(imageView);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private MessagesListAdapter.Formatter<Message> getMessageStringFormatter() {
        return message -> {
            String createdAt = new SimpleDateFormat("MMM d, EEE 'at' h:mm a", Locale.getDefault())
                    .format(message.getCreatedAt());

            String text = message.getText();
            if (text == null) text = "[attachment]";

            return String.format(Locale.getDefault(), "%s: %s (%s)",
                    message.getUser().getName(), text, createdAt);
        };
    }
}
