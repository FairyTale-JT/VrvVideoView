package com.jt.vrplayer;


import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.jt.face.RegularExpress;

import java.io.IOException;

/**
 * VR- 播放管理
 */

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
    private View mReplay;//重播
    private View mVideoBuffer;//加载进度圈
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE:
                    mVideoPorgressContainer.setVisibility(View.GONE);//隐藏播放控制面板
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置横屏
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        //取消标题栏
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //保存当前播放进度,视频总时长,暂停播放状态
        outState.putLong(STATE_PROGRESS_TIME, mVideoView.getCurrentPosition());
        outState.putLong(STATE_VIDEO_DURATION, mVideoView.getDuration());
        outState.putBoolean(STATE_IS_PLAYING, isPlaying);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        mVideoView.seekTo(progressTime); //得到播放进度进行设置
        long duration = savedInstanceState.getLong(STATE_VIDEO_DURATION);
        mTotallDuration = RegularExpress.parseDuration(duration);
        mSeekBar.setMax((int) duration);
        mSeekBar.setProgress((int) progressTime);

        isPlaying = savedInstanceState.getBoolean(STATE_IS_PLAYING);
        performChangePlayState(isPlaying);//根据保存的播放状态进行播放/暂停处理
    }

    private void initView() {
//        Intent intent = getIntent();
//        mUrl = intent.getStringExtra("url"); //上一个activity传递过来的url播放地址

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
        //第一次视频加载成功的时候,isplaying应该为true,onLoadSuccess()方法会执行多次(初次加载视频,seekto()被调用,home/锁屏退出再进入等都会执行)
        isPlaying = true;
        mSeekBar.setMax(Integer.MAX_VALUE);//防止刚加载视频时进度条跳一下又返回正常比例,主要因为第一次设置progress时,可能还未设置最大值
    }

    private void initListener() {
        mPlayView.setOnClickListener(this);
        mVideoVr.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new SeekBarListener());
        mVideoView.setEventListener(new VrVideoEventListener() {
            @Override
            public void onClick() {
                //处理控制面板的显示和隐藏
                int visiblity = mVideoPorgressContainer.getVisibility();
                if (visiblity == View.VISIBLE) {
                    mVideoPorgressContainer.setVisibility(View.GONE);
                } else {
                    mVideoPorgressContainer.setVisibility(View.VISIBLE);
                    hidePlayerControllerDelayed();//延时隐藏控制面板
                }
            }

            @Override
            public void onCompletion() {
                performChangePlayState(false);
                mVideoPorgressContainer.setVisibility(View.VISIBLE);
                mReplay.setVisibility(View.VISIBLE);
                hidePlayerControllerDelayed();//延迟隐藏控制面板
            }

            @Override
            public void onNewFrame() {
                updateVideoProgress();
            }

            @Override
            public void onLoadSuccess() {
                mVideoBuffer.setVisibility(View.GONE);//视频加载成功隐藏加载进度圈
                long duration = mVideoView.getDuration();//视频总时长/毫秒
                mTotallDuration = RegularExpress.parseDuration(duration);
                mSeekBar.setMax((int) duration);
                performChangePlayState(isPlaying);//视频加载成功,开始播放更新状态
                mVideoPorgressContainer.setVisibility(View.VISIBLE);//默认不可见,当加载视频成功后显示视频时长等信息
                hidePlayerControllerDelayed();
                /**这里解释一下为什么没把下面的判断逻辑操作放在performClickPlay()方法的else语句中,因为seekTo是耗时操作,不能马上完成,在else语句中虽然seekTo(0)
                 * 但是紧接着执行mVideoView.playVideo();方法,视频这时的播放位置还是在最后,会触发onCompletion()方法,该方法中的mReplay.setVisibility(View.VISIBLE);
                 * 就被执行了,结果就是视频虽然重播了,但是重播按钮还是显示的,为避免这种情况,故做了下面的判断操作[因为seekTo(0)之后会执行onLoadSuccess()方法]
                 */
                if (mReplay.getVisibility() == View.VISIBLE) {
                    mReplay.setVisibility(View.GONE);
                }
            }

            @Override
            public void onLoadError(String errorMessage) {
                super.onLoadError(errorMessage);
                Toast.makeText(MainActivity.this, "出现问题,无法播放", Toast.LENGTH_SHORT).show();
                mVideoBuffer.setVisibility(View.GONE);//隐藏加载进度圈
            }

            @Override
            public void onDisplayModeChanged(int newDisplayMode) {
                super.onDisplayModeChanged(newDisplayMode);
            }
        });

    }

    /**
     * 更新播放进度
     */
    private void updateVideoProgress() {
        long currentPosition = mVideoView.getCurrentPosition();
        String currentPos = RegularExpress.parseDuration(currentPosition);
        mSeekBar.setProgress((int) currentPosition);//更新播放进度
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(currentPos);
        stringBuffer.append("/");
        if (mTotallDuration == null) {
            mTotallDuration = RegularExpress.parseDuration(mVideoView.getDuration());
        }
        stringBuffer.append(mTotallDuration);
        mVideoDuration.setText(stringBuffer);

    }

    //播放视频
    private void handleIntent() {
        options.inputType = VrVideoView.Options.TYPE_STEREO_OVER_UNDER;
//        if ("".equals(mUrl)) {
        String uri = "congo.mp4";
        try {
            mVideoView.loadVideoFromAsset(uri, options);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        } else {
//            Uri uri = Uri.parse(mUrl);
//            try {
//                mVideoView.loadVideo(uri, options);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * 播放进度条
     */
    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (b) {
                mVideoView.seekTo(i);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            hidePlayerControllerDelayed();
            mReplay.setVisibility(seekBar.getProgress() < seekBar.getMax() ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * 延时隐藏播放器控制面板
     */
    private void hidePlayerControllerDelayed() {
        mHandler.removeMessages(HIDE);
        mHandler.sendEmptyMessageDelayed(HIDE, 5000);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
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

    /**
     * 播放暂停切换
     */
    private void performClickPlay() {
        if (isPlaying) {
            mVideoView.pauseVideo();
            mPlayView.setImageResource(R.mipmap.play);
            isPlaying = false;
        } else {
            if (mReplay.getVisibility() == View.VISIBLE) {
                mVideoView.seekTo(0);
            }
            mVideoView.playVideo();
            mPlayView.setImageResource(R.mipmap.stop);
            isPlaying = true;
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
    }
}
