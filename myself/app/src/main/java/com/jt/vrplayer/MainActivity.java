package com.jt.vrplayer;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.jt.face.RegularExpress;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int HIDE = 0; //隐藏播放控制面板
    private static final String STATE_PROGRESS_TIME = "progressTime";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    private static final String STATE_IS_PLAYING = "isPlaying";
    private VrVideoView mVideoView;
    private VrVideoView.Options options = new VrVideoView.Options();
    private String mUrl; //上一个Activity传递过来的url播放地址
    private TextView mVideoDuration;
    private SeekBar mSeekBar;
    private View mVideoPorgressContainer;
    private String mTotallDuration;//视频总时长
    private ImageView mPlayView;
    private boolean isPlaying;//播放/暂停状态标记
    private View mVideoVr;
    private View mReplay;
    private View mVideoBuffer;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE:
                    mVideoPorgressContainer.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(STATE_PROGRESS_TIME,mVideoView.getCurrentPosition());
        outState.putLong(STATE_VIDEO_DURATION,mVideoView.getDuration());
        outState.putBoolean(STATE_IS_PLAYING,isPlaying);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long progressTime =savedInstanceState.getLong(STATE_PROGRESS_TIME);
        mVideoView.seekTo(progressTime);
        long duration =savedInstanceState.getLong(STATE_VIDEO_DURATION);
        mTotallDuration=RegularExpress.parseDuration(duration);
        mSeekBar.setMax((int) duration);
        mSeekBar.setProgress((int) progressTime);

        isPlaying=savedInstanceState.getBoolean(STATE_IS_PLAYING);
        performChangePlayState(isPlaying);
    }

    private void initView() {
//        Intent intent = getIntent();
//        mUrl = intent.getStringExtra("url");

        mVideoView = (VrVideoView) findViewById(R.id.video_view);
        mPlayView = (ImageView) findViewById(R.id.play);
        mReplay = findViewById(R.id.replay);
        mVideoDuration = (TextView) findViewById(R.id.video_duration);
        mSeekBar = (SeekBar) findViewById(R.id.video_progress);
        mVideoPorgressContainer = findViewById(R.id.video_progress_container);
        mVideoVr = findViewById(R.id.video_vr);
        mVideoBuffer = findViewById(R.id.video_buffer);

        mVideoView.setInfoButtonEnabled(false);//设置左侧信息原圈不可见
        mVideoView.setFullscreenButtonEnabled(false);//设置全屏按钮不可见
        mVideoView.setStereoModeButtonEnabled(false);//设置立体眼镜模式按钮不可见
        mVideoView.setTransitionViewEnabled(false);//设置将手机放入盒子中的提示取消
        mVideoView.setTouchTrackingEnabled(true);//开启手触模式
    }

    private void initData() {
        handleIntent();
        isPlaying = true;
        mSeekBar.setMax(Integer.MAX_VALUE);
    }

    private void initListener() {
        mPlayView.setOnClickListener(this);
        mVideoVr.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new SeekBarListener());
        mVideoView.setEventListener(new VrVideoEventListener(){
            @Override
            public void onClick() {
                int visiblity =mVideoPorgressContainer.getVisibility();
                if (visiblity==View.VISIBLE){
                    mVideoPorgressContainer.setVisibility(View.GONE);
                }else{
                    mVideoPorgressContainer.setVisibility(View.VISIBLE);
                    hidePlayerControllerDelayed();
                }
            }

            @Override
            public void onCompletion() {
                super.onCompletion();
                performChangePlayState(false);
                mVideoPorgressContainer.setVisibility(View.VISIBLE);
                mReplay.setVisibility(View.VISIBLE);
                hidePlayerControllerDelayed();
            }

            @Override
            public void onNewFrame() {
                updateVideoProgress();
            }

            @Override
            public void onLoadSuccess() {
                mVideoBuffer.setVisibility(View.GONE);
                long duration =mVideoView.getDuration();
                mTotallDuration=RegularExpress.parseDuration(duration);
                mSeekBar.setMax((int) duration);
                performChangePlayState(isPlaying);
                mVideoPorgressContainer.setVisibility(View.VISIBLE);
                hidePlayerControllerDelayed();
                if (mReplay.getVisibility()==View.VISIBLE){
                    mReplay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadError(String errorMessage) {
                super.onLoadError(errorMessage);
                Toast.makeText(MainActivity.this,"出现问题,无法播放",Toast.LENGTH_SHORT).show();
                mVideoBuffer.setVisibility(View.GONE);
            }

            @Override
            public void onDisplayModeChanged(int newDisplayMode) {
                super.onDisplayModeChanged(newDisplayMode);
            }
        });

    }
    private void updateVideoProgress(){
        long currentPosition =mVideoView.getCurrentPosition();
        String currentPos = RegularExpress.parseDuration(currentPosition);
        mSeekBar.setProgress((int) currentPosition);
        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append(currentPos);
        stringBuffer.append("/");
        if (mTotallDuration==null)
            mTotallDuration=RegularExpress.parseDuration(mVideoView.getDuration());

        stringBuffer.append(mTotallDuration);
        mVideoDuration.setText(stringBuffer);

    }
    private void handleIntent() {
        if ("".equals(mUrl)) {
            String uri = "congo.mp4";
            try {
                mVideoView.loadVideoFromAsset(uri, options);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Uri uri = Uri.parse(mUrl);
            try {
                mVideoView.loadVideo(uri, options);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (b){
                mVideoView.seekTo(i);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            hidePlayerControllerDelayed();
            mReplay.setVisibility(seekBar.getProgress()<seekBar.getMax()?View.GONE:View.VISIBLE);
        }
    }
    private void hidePlayerControllerDelayed(){

        mHandler.removeMessages(HIDE);
        mHandler.sendEmptyMessageDelayed(HIDE,5000);

    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.play:
                performClickPlay();
                break;
            case R.id.video_vr:
                performClickVideoVr();
                break;
            case R.id.replay:
                performClickReplay();
                break;

            default:
                break;
        }
    }
    private void performClickPlay(){
        if (isPlaying){
            mVideoView.pauseVideo();
            mPlayView.setImageResource(R.mipmap.play);
            isPlaying=false;
        }else {

            if (mReplay.getVisibility()==View.VISIBLE){
                mVideoView.seekTo(0);
            }
            mVideoView.playVideo();
            mPlayView.setImageResource(R.mipmap.stop);
            isPlaying=true;
        }
    }
    private void performClickVideoVr() {
        mVideoView.setDisplayMode(3);//enterStereoMode,眼镜模式
    }
    private void performClickReplay() {
        mVideoView.seekTo(0);//重播时进度置为初始进度0
        performChangePlayState(true);
        mReplay.setVisibility(View.GONE);
    }
    /**
     * 控制播放的状态
     *
     * @param b 是否播放
     */
    private void performChangePlayState(boolean b) {
        if (b) {
            mVideoView.playVideo();
            mPlayView.setImageResource(R.mipmap.stop);
            isPlaying = true;
        } else {
            mVideoView.pauseVideo();
            mPlayView.setImageResource(R.mipmap.play);
            isPlaying = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pauseRendering();
        mVideoView.pauseVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resumeRendering();
        performChangePlayState(isPlaying);
    }

    @Override
    protected void onDestroy() {
        mVideoView.shutdown();
        mHandler.removeMessages(HIDE);
        mHandler = null;
        super.onDestroy();
        super.onDestroy();
    }
}
