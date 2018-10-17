package ekoolab.com.show.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidnetworking.model.Progress;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.reflect.TypeToken;
import com.juziwl.ijkplayerlib.media.IjkVideoView;
import com.orhanobut.logger.Logger;
import com.rey.material.widget.ProgressView;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ekoolab.com.show.R;
import ekoolab.com.show.api.ApiServer;
import ekoolab.com.show.api.NetworkSubscriber;
import ekoolab.com.show.api.ResponseData;
import ekoolab.com.show.beans.Live;
import ekoolab.com.show.beans.LoginData;
import ekoolab.com.show.utils.AuthUtils;
import ekoolab.com.show.utils.Constants;
import ekoolab.com.show.utils.DisplayUtils;
import ekoolab.com.show.utils.ImageLoader;
import ekoolab.com.show.utils.Utils;
import ekoolab.com.show.views.BubbleView;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class LivePlayerActivity extends BaseActivity implements View.OnClickListener, Player.EventListener {
    public static final String LIVE_DATA = "live_data";
    private Live live;
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private DataSource.Factory mediaDataSourceFactory;
    private Handler mainHandler;

    private ImageView maskImageView;
    private FloatingActionButton likeBtn;
    private ImageView avatarIv;
    private TextView nameTv;
    private TextView audienceTv;
    private Button followBtn;
    private ImageButton dismissBtn;
    private ImageButton commentBtn;
    private ImageButton giftBtn;
    private ImageButton shareBtn;
    private ProgressView followStatePv;

    long mLastTime = 0;
    long mCurTime = 0;
    private final int DELAY = 500;//连续点击的临界点
    private BubbleView bubbleView;
    private Timer delayTimer;
    private TimerTask timeTask;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            addLiveLick();
            delayTimer.cancel();
            super.handleMessage(msg);
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_live_player;
    }

    @Override
    protected void initData() {
        super.initData();

        live = getIntent().getParcelableExtra(LIVE_DATA);
    }

    @Override
    protected void initViews() {
        super.initViews();

        maskImageView = findViewById(R.id.mask_iv);
        ImageLoader.displayImage(live.coverImage.medium, maskImageView, 25);

        likeBtn = findViewById(R.id.like_float_btn);
        likeBtn.setOnClickListener(this);

        avatarIv = findViewById(R.id.avatar_iv);
        ImageLoader.displayImageAsCircle(live.avatar.small, avatarIv);

        nameTv = findViewById(R.id.name_tv);
        nameTv.setText(Utils.getDisplayName(live.author, live.nickname));

        audienceTv = findViewById(R.id.audience_amount_tv);
        audienceTv.setText(live.audienceCount.size()+"");

        followBtn = findViewById(R.id.follow_btn);
        followBtn.setOnClickListener(this);

        dismissBtn = findViewById(R.id.dismiss_btn);
        dismissBtn.setOnClickListener(this);

        commentBtn = findViewById(R.id.comment_btn);
        commentBtn.setOnClickListener(this);

        giftBtn = findViewById(R.id.gift_btn);
        giftBtn.setOnClickListener(this);

        shareBtn = findViewById(R.id.share_btn);
        shareBtn.setOnClickListener(this);

        followStatePv= findViewById(R.id.follow_state_pv);

        bubbleView = findViewById(R.id.bubble_view);
        bubbleView.setDefaultDrawableList();

        mainHandler = new Handler();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initPlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initPlayer();
        }

        isFollowed();
    }

    private void initPlayer(){
        playerView = findViewById(R.id.player_view);
        playerView.hideController();
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        player = ExoPlayerFactory.newSimpleInstance(this);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        player.addListener(this);
        player.setPlayWhenReady(true);
        player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        playerView.setPlayer(player);

        mediaDataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Show"));
        MediaSource mediaSource = new HlsMediaSource(Uri.parse(live.resourceUri),
                mediaDataSourceFactory, mainHandler, null);
        player.prepare(mediaSource, false, false);
    }

    public void removeMask(){
        maskImageView.animate()
                .alpha(0.0f)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        maskImageView.setVisibility(View.GONE);
                    }
                });
    }

    private void isFollowed(){
        beforeGetFollowState();
        HashMap<String, String> map = new HashMap<>();
        map.put("token", AuthUtils.getInstance(this).getApiToken());
        map.put("userCode", live.userCode);
        ApiServer.basePostRequest(this, Constants.IS_FOLLOWED, map, new TypeToken<ResponseData<Boolean>>(){})
                .subscribe(new NetworkSubscriber<Boolean>() {
                    @Override
                    protected void onSuccess(Boolean isFollow) {
                        afterGetFollowState();
                        updateFollowState(isFollow);
                    }

                    @Override
                    protected boolean dealHttpException(int code, String errorMsg, Throwable e) {
                        afterGetFollowState();
                        return super.dealHttpException(code, errorMsg, e);
                    }
                });
    }

    private void beforeGetFollowState() {
        followStatePv.start();
        followBtn.setEnabled(false);
        followBtn.setVisibility(View.INVISIBLE);
    }

    private void afterGetFollowState() {
        followStatePv.stop();
        followBtn.setEnabled(true);
        followBtn.setVisibility(View.VISIBLE);
    }

    private void updateFollowState(boolean isFollowed){
        if (isFollowed){
            followBtn.setText(getString(R.string.un_follow));
        } else {
            followBtn.setText(getString(R.string.follow));
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.like_float_btn:
                mCurTime = System.currentTimeMillis();
                delay();
                mLastTime = mCurTime;
                bubbleView.startAnimation(bubbleView.getWidth(), bubbleView.getHeight());
                break;
            case R.id.follow_btn:
                updateFollow();
                break;
            case R.id.share_btn:
                break;
            case R.id.comment_btn:
                break;
            case R.id.dismiss_btn:
                onBackPressed();
                break;
        }
    }

    private void updateFollow(){
        boolean isFollowed = Utils.equals(getString(R.string.follow), followBtn.getText().toString());

        beforeGetFollowState();
        HashMap<String, String> map = new HashMap<>(2);
        map.put("userCode", live.userCode);
        map.put("token", AuthUtils.getInstance(this).getApiToken());
        ApiServer.basePostRequest(this, isFollowed ? Constants.FOLLOWCANCEL : Constants.FOLLOW, map,
                new TypeToken<ResponseData<String>>() {
                })
                .subscribe(new NetworkSubscriber<String>() {
                    @Override
                    protected void onSuccess(String s) {
                        Logger.i("----------> " + s);
                        afterGetFollowState();
                    }

                    @Override
                    protected boolean dealHttpException(int code, String errorMsg, Throwable e) {
                        afterGetFollowState();
                        return super.dealHttpException(code, errorMsg, e);
                    }
                });
    }

    private void delay() {
        if (timeTask != null)
            timeTask.cancel();

        timeTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                mHandler.sendMessage(message);
            }
        };

        delayTimer = new Timer();
        delayTimer.schedule(timeTask, DELAY);
    }

    private void addLiveLick() {
        //send request

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:       // The player does not have any media to play yet.
                break;
            case Player.STATE_BUFFERING:  // The player is buffering (loading the content)
                break;
            case Player.STATE_READY:      // The player is able to immediately play
                removeMask();
                break;
            case Player.STATE_ENDED:      // The player has finished playing the media
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}