package com.andreapivetta.blu.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andreapivetta.blu.R;
import com.andreapivetta.blu.activities.ImageActivity;
import com.andreapivetta.blu.activities.NewTweetActivity;
import com.andreapivetta.blu.activities.TweetActivity;
import com.andreapivetta.blu.activities.UserActivity;
import com.andreapivetta.blu.twitter.FavoriteTweet;
import com.andreapivetta.blu.twitter.RetweetTweet;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;

public class TweetsListAdapter extends RecyclerView.Adapter<TweetsListAdapter.ViewHolder> {

    private static final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_ITEM_PHOTO = 2;

    private ArrayList<Status> mDataSet;
    private Context context;
    private Twitter twitter;
    private int headerPosition;

    public TweetsListAdapter(ArrayList<Status> mDataSet, Context context, Twitter twitter) {
        this.mDataSet = mDataSet;
        this.context = context;
        this.twitter = twitter;
        this.headerPosition = 0;
    }

    public TweetsListAdapter(ArrayList<Status> mDataSet, Context context, Twitter twitter, int headerPosition) {
        this.mDataSet = mDataSet;
        this.context = context;
        this.twitter = twitter;
        this.headerPosition = headerPosition;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM)
            return new VHItem(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_basic, parent, false));
        else if(viewType == TYPE_ITEM_PHOTO)
            return new VHItemPhoto(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_photo, parent, false));
        else
            return new VHHeader(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_expanded, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Status currentStatus;
        final int TYPE = getItemViewType(position);

        if (mDataSet.get(position).isRetweet()) {
            currentStatus = mDataSet.get(position).getRetweetedStatus();
            holder.retweetTextView.setVisibility(View.VISIBLE);
            holder.retweetTextView.setText(
                    context.getString(R.string.retweeted_by, mDataSet.get(position).getUser().getScreenName()));
        } else {
            currentStatus = mDataSet.get(position);
            holder.retweetTextView.setVisibility(View.GONE);
        }

        holder.userNameTextView.setText(currentStatus.getUser().getName());
        holder.timeTextView.setText(new SimpleDateFormat("hh:mm").format(currentStatus.getCreatedAt()));

        Picasso.with(context)
                .load(currentStatus.getUser().getBiggerProfileImageURL())
                .placeholder(context.getResources().getDrawable(R.drawable.placeholder))
                .into(holder.userProfilePicImageView);

        holder.userProfilePicImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile(currentStatus);
            }
        });

        holder.favouriteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favourite(currentStatus);
            }
        });

        holder.retweetImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retweet(currentStatus);
            }
        });

        holder.respondImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reply(currentStatus);
            }
        });

        holder.shareImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share(currentStatus);
            }
        });

        if (TYPE == TYPE_HEADER) {
            StringBuilder iHateHtml = new StringBuilder();
            Pattern p = Pattern.compile(URL_REGEX);
            Matcher m;
            String endString = "";
            for (String word : currentStatus.getText().split(" |\n")) {
                m = p.matcher(word);
                if (m.find()) {
                    iHateHtml.append("<a href=\"")
                            .append(word)
                            .append("\">")
                            .append(word)
                            .append("</a>");
                } else if (word.length() > 1) {
                    if (word.substring(0, 1).equals("@")) {
                        for (int i = 1; i < word.length(); i++)
                            if ("|/()=?'^[],;.:-\"\\".indexOf(word.charAt(i)) >= 0) {
                                endString = word.substring(i);
                                word = word.substring(0, i);
                                break;
                            }

                        iHateHtml.append("<a href=\"com.andreapivetta.blu.user://")
                                .append(word.substring(1))
                                .append("\">")
                                .append(word)
                                .append("</a>")
                                .append(endString);
                    } else if (word.substring(0, 1).equals("#")) {
                        for (int i = 1; i < word.length(); i++)
                            if ("|/()=?'^[],;.:-\"\\".indexOf(word.charAt(i)) >= 0) {
                                endString = word.substring(i);
                                word = word.substring(0, i);
                                break;
                            }

                        iHateHtml.append("<a href=\"com.andreapivetta.blu.hashtag://")
                                .append(word.substring(1))
                                .append("\">")
                                .append(word)
                                .append("</a>")
                                .append(endString);
                    } else {
                        iHateHtml.append(word);
                    }
                } else {
                    iHateHtml.append(word);
                }
                iHateHtml.append(" ");
            }

            holder.statusTextView.setText(Html.fromHtml(iHateHtml.toString()));
            holder.statusTextView.setMovementMethod(LinkMovementMethod.getInstance());

            String amount = currentStatus.getFavoriteCount() + "";
            StyleSpan b = new StyleSpan(android.graphics.Typeface.BOLD);

            SpannableStringBuilder sb = new SpannableStringBuilder(context.getString(R.string.favourites, amount));
            sb.setSpan(b, 0, amount.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ((VHHeader) holder).favouritesStatsTextView.setText(sb);

            amount = currentStatus.getRetweetCount() + "";
            b = new StyleSpan(android.graphics.Typeface.BOLD);

            sb = new SpannableStringBuilder(context.getString(R.string.retweets, amount));
            sb.setSpan(b, 0, amount.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            ((VHHeader) holder).retweetsStatsTextView.setText(sb);

            MediaEntity mediaEntityArray[] = currentStatus.getMediaEntities();
            if (mediaEntityArray.length > 0) {
                for (final MediaEntity mediaEntity : mediaEntityArray) {
                    if (mediaEntity.getType().equals("photo")) {
                        ((VHHeader) holder).tweetPhotoImageView.setVisibility(View.VISIBLE);
                        Picasso.with(context)
                                .load(mediaEntity.getMediaURL())
                                .placeholder(context.getResources().getDrawable(R.drawable.placeholder))
                                .into(((VHHeader) holder).tweetPhotoImageView);

                        ((VHHeader) holder).tweetPhotoImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(context, ImageActivity.class);
                                i.putExtra("IMAGE", mediaEntity.getMediaURL());
                                context.startActivity(i);
                            }
                        });

                        break;
                    }
                }
            }
        } else {
            holder.statusTextView.setText(currentStatus.getText());
            holder.interactionLinearLayout.setVisibility(View.GONE);
            Linkify.addLinks(holder.statusTextView, Linkify.ALL);

            holder.statusTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.interactionLinearLayout.getVisibility() == View.VISIBLE)
                        holder.interactionLinearLayout.setVisibility(View.GONE);
                    else
                        holder.interactionLinearLayout.setVisibility(View.VISIBLE);
                }
            });

            if (TYPE == TYPE_ITEM_PHOTO) {
                MediaEntity mediaEntityArray[] = currentStatus.getMediaEntities();
                for (final MediaEntity mediaEntity : mediaEntityArray) {
                    if (mediaEntity.getType().equals("photo")) {
                        Picasso.with(context)
                                .load(mediaEntity.getMediaURL())
                                .placeholder(context.getResources().getDrawable(R.drawable.placeholder))
                                .fit()
                                .centerCrop()
                                .into(((VHItemPhoto) holder).tweetPhotoImageView);

                        ((VHItemPhoto) holder).tweetPhotoImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(context, ImageActivity.class);
                                i.putExtra("IMAGE", mediaEntity.getMediaURL());
                                context.startActivity(i);
                            }
                        });

                        break;
                    }
                }
            }

            ((VHItem) holder).cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.interactionLinearLayout.getVisibility() == View.VISIBLE)
                        holder.interactionLinearLayout.setVisibility(View.GONE);
                    else
                        holder.interactionLinearLayout.setVisibility(View.VISIBLE);
                }
            });

            ((VHItem) holder).openTweetImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(context, TweetActivity.class);
                    i.putExtra("STATUS", currentStatus.getId());
                    context.startActivity(i);
                }
            });
        }
    }

    void openProfile(Status status) {
        Intent i = new Intent(context, UserActivity.class);
        i.putExtra("ID", status.getUser().getId());
        context.startActivity(i);
    }

    void reply(Status status) {
        Intent i = new Intent(context, NewTweetActivity.class);
        i.putExtra("USER_PREFIX", "@" + status.getUser().getScreenName())
                .putExtra("REPLY_ID", status.getId());
        context.startActivity(i);
    }

    void retweet(final Status status) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.retweet_title))
                .setPositiveButton(R.string.retweet, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new RetweetTweet(context, twitter).execute(status.getId());
                    }
                })
                .setNegativeButton(R.string.cancel, null).create().show();
    }

    void favourite(Status status) {
        long type;
        if (status.isFavorited()) {
            type = -1;
            new FavoriteTweet(context, twitter).execute(status.getId(), type);
        } else {
            type = 1;
            new FavoriteTweet(context, twitter).execute(status.getId(), type);
        }
    }

    void share(Status status) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "https://twitter.com/" +
                        status.getUser().getScreenName() + "/status/" + status.getId())
                .setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_tweet)));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @Override
    public int getItemViewType(int position) {

        if (isPositionHeader(position))
            return TYPE_HEADER;

        if (mDataSet.get(position).getMediaEntities().length > 0)
            return TYPE_ITEM_PHOTO;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == headerPosition;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout interactionLinearLayout;
        public TextView userNameTextView, statusTextView, timeTextView, retweetTextView;
        public ImageView userProfilePicImageView;
        public ImageButton favouriteImageButton, retweetImageButton, respondImageButton,
                shareImageButton;

        public ViewHolder(View container) {
            super(container);

            this.interactionLinearLayout = (LinearLayout) container.findViewById(R.id.interactionLinearLayout);
            this.userNameTextView = (TextView) container.findViewById(R.id.userNameTextView);
            this.statusTextView = (TextView) container.findViewById(R.id.statusTextView);
            this.userProfilePicImageView = (ImageView) container.findViewById(R.id.userProfilePicImageView);
            this.timeTextView = (TextView) container.findViewById(R.id.timeTextView);
            this.retweetTextView = (TextView) container.findViewById(R.id.retweetTextView);
            this.favouriteImageButton = (ImageButton) container.findViewById(R.id.favouriteImageButton);
            this.retweetImageButton = (ImageButton) container.findViewById(R.id.retweetImageButton);
            this.respondImageButton = (ImageButton) container.findViewById(R.id.respondImageButton);
            this.shareImageButton = (ImageButton) container.findViewById(R.id.shareImageButton);
        }
    }

    class VHItem extends ViewHolder {
        public FrameLayout cardView;
        public ImageButton openTweetImageButton;

        public VHItem(View container) {
            super(container);

            this.cardView = (FrameLayout) container.findViewById(R.id.card_view);
            this.openTweetImageButton = (ImageButton) container.findViewById(R.id.openTweetImageButton);
        }
    }

    class VHItemPhoto extends VHItem {
        public ImageView tweetPhotoImageView; // TODO e se ho più foto?

        public VHItemPhoto(View container) {
            super(container);

            this.tweetPhotoImageView = (ImageView) container.findViewById(R.id.tweetPhotoImageView);
        }
    }

    class VHHeader extends ViewHolder {
        public TextView screenNameTextView, retweetsStatsTextView, favouritesStatsTextView;
        public ImageView tweetPhotoImageView; // TODO anche qui diversi header

        public VHHeader(View container) {
            super(container);

            this.userProfilePicImageView = (ImageView) container.findViewById(R.id.userProfilePicImageView);
            this.tweetPhotoImageView = (ImageView) container.findViewById(R.id.tweetPhotoImageView);
            this.userNameTextView = (TextView) container.findViewById(R.id.userNameTextView);
            this.screenNameTextView = (TextView) container.findViewById(R.id.screenNameTextView);
            this.timeTextView = (TextView) container.findViewById(R.id.timeTextView);
            this.statusTextView = (TextView) container.findViewById(R.id.statusTextView);
            this.retweetTextView = (TextView) container.findViewById(R.id.retweetTextView);
            this.retweetsStatsTextView = (TextView) container.findViewById(R.id.retweetsStatsTextView);
            this.favouritesStatsTextView = (TextView) container.findViewById(R.id.favouritesStatsTextView);
            this.favouriteImageButton = (ImageButton) container.findViewById(R.id.favouriteImageButton);
            this.retweetImageButton = (ImageButton) container.findViewById(R.id.retweetImageButton);
            this.respondImageButton = (ImageButton) container.findViewById(R.id.respondImageButton);
            this.shareImageButton = (ImageButton) container.findViewById(R.id.shareImageButton);
        }
    }
}