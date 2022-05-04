package com.customerdynamics.sdk.custom.holders;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.customerdynamics.androidlibrary.enums.ElementType;
import com.customerdynamics.androidlibrary.models.MessageElement;
import com.customerdynamics.sdk.R;
import com.customerdynamics.sdk.model.Message;
import com.ouattararomuald.slider.ImageLoader;
import com.ouattararomuald.slider.ImageSlider;
import com.ouattararomuald.slider.SliderAdapter;
import com.ouattararomuald.slider.loaders.picasso.PicassoImageLoaderFactory;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.utils.DateFormatter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by troy379 on 05.04.17.
 */
public class IncomingTextAndButtonsMessageViewHolder
        extends MessageHolders.IncomingTextMessageViewHolder<Message> implements ImageLoader.EventListener {

    private LinearLayout bubble;
    private TextView tvTime;
    private ImageSlider imageSlider;

    public IncomingTextAndButtonsMessageViewHolder(View itemView, Object payload) {
        super(itemView, payload);
        bubble = itemView.findViewById(R.id.bubble);
        tvTime = itemView.findViewById(R.id.time);
        imageSlider = itemView.findViewById(R.id.image_slider);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);
        if (!message.getPayload().getElements().isEmpty()) {
            List<MessageElement> payloadElements = message.getPayload().getElements();
            if (payloadElements.size() > 1) {
                bubble.removeAllViews();
                List<String> imageUrls = new ArrayList<>();
                List<String> descriptions = new ArrayList<>();
                for (MessageElement payloadElement : payloadElements) {
                    List<MessageElement> messageElements = payloadElement.getElements();

                    for (MessageElement element : messageElements) {
                        if (element.getType().equals(ElementType.File.toString())) {
                            imageUrls.add(element.getUrl());
                        }
                    }
                }

                imageSlider.setAdapter(
                        new SliderAdapter(
                                bubble.getContext(),
                                new PicassoImageLoaderFactory(R.drawable.account_avatar, R.drawable.bubble_circle, this),
                                imageUrls,
                                descriptions,
                                "slider"
                        ));
                bubble.addView(imageSlider);
            } else {
                if (!payloadElements.get(0).getElements().isEmpty()) {
                    List<MessageElement> messageElements = payloadElements.get(0).getElements();
                    bubble.removeAllViews();
                    for (MessageElement element : messageElements) {
                        if (element.getType().equals(ElementType.Text.toString())) {
                            TextView textView = new TextView(bubble.getContext());
                            textView.setText(element.getText());
                            bubble.addView(textView);
                        } else if (element.getType().equals(ElementType.Button.toString())) {
                            Button button = new Button(bubble.getContext());
                            button.setText(element.getText());
                            bubble.addView(button);
                        } else if (element.getType().equals(ElementType.File.toString())) {
                            ImageView imageView = new ImageView(bubble.getContext());
                            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                            Picasso.get().load(element.getUrl()).into(imageView);
                            bubble.addView(imageView);
                        } else if (element.getType().equals(ElementType.Title.toString())) {
                            TextView textView = new TextView(bubble.getContext());
                            textView.setText(element.getText());
                            textView.setTextSize(16);
                            textView.setTypeface(Typeface.DEFAULT_BOLD);
                            bubble.addView(textView);
                        }
                    }
                } else {
                    MessageElement messageElement = payloadElements.get(0);
                    bubble.removeAllViews();
                    if (messageElement.getType().equals(ElementType.Text.toString())) {
                        TextView textView = new TextView(bubble.getContext());
                        textView.setText(messageElement.getText());
                        bubble.addView(textView);
                    } else if (messageElement.getType().equals(ElementType.Button.toString())) {
                        Button button = new Button(bubble.getContext());
                        button.setText(messageElement.getText());

                        try {
                            JSONObject jsonObject = (JSONObject) new JSONTokener(messageElement.getPostback()).nextValue();

                            if (jsonObject != null && !jsonObject.isNull("type")) {
                                String type = jsonObject.getString("type");
                                if (type.equals("deepLink")) {
                                    String deepLink = jsonObject.getString("deepLink");

                                    button.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            try {
                                                Uri uri = Uri.parse(deepLink);
                                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                                bubble.getContext().startActivity(intent);
                                            } catch (Exception e) {
                                                new AlertDialog.Builder(bubble.getContext())
                                                        .setTitle("Information")
                                                        .setMessage(e.getMessage())
                                                        .setPositiveButton(android.R.string.ok, null)
                                                        .show();
                                            }
                                        }
                                    });
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        bubble.addView(button);
                    } else if (messageElement.getType().equals(ElementType.File.toString())) {
                        ImageView imageView = new ImageView(bubble.getContext());
                        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                        Picasso.get().load(messageElement.getUrl()).into(imageView);
                        bubble.addView(imageView);
                    } else if (messageElement.getType().equals(ElementType.Title.toString())) {
                        TextView textView = new TextView(bubble.getContext());
                        textView.setText(messageElement.getText());
                        textView.setTextSize(16);
                        textView.setTypeface(Typeface.DEFAULT_BOLD);
                        bubble.addView(textView);
                    }  else if (messageElement.getType().equals(ElementType.Custom.toString())) {
                        Object variables = messageElement.getVariables();
                        try {
                            JSONObject jsonObject = (JSONObject) new JSONTokener(variables.toString()).nextValue();
                            String color = jsonObject.getString("color");

                            View view = new View(bubble.getContext());

                            switch (color) {
                                case "green":
                                    view.setBackgroundColor(Color.GREEN);
                                    break;
                                case "blue":
                                    view.setBackgroundColor(Color.BLUE);
                                    break;
                                default:
                                    view.setBackgroundColor(Color.BLACK);
                                    break;
                            }

                            view.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
                            bubble.addView(view);

                            Log.i("", "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        tvTime.setText(DateFormatter.format(message.getCreatedAt(), DateFormatter.Template.TIME));
    }

    @Override
    public void onImageViewConfiguration(@NonNull ImageView imageView) {
    }
}
