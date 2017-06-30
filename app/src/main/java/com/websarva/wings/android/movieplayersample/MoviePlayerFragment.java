package com.websarva.wings.android.movieplayersample;


import android.annotation.TargetApi;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import junit.framework.Assert;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;


/**
 * A simple {@link Fragment} subclass.
 */
public class MoviePlayerFragment extends Fragment implements Runnable {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private View mControlBackground;
    private View mControlLayer;
    private Button mPlayPauseButton;
    private Button mLoopButton;
    private SeekBar mSeekBar;
    private TextView mLeftTimeText;
    private TextView mRightTimeText;
    private boolean mRunning;
    private Thread mPlayerThread;
    private final DecimalFormat mFormatterMin = new DecimalFormat("#");
    private final DecimalFormat mFormatterSec = new DecimalFormat("00");
    private String mPlayingUri;
    private static final String PLAYING_URI_KEY = "PLAYING_URI";

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Listener
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            // SurfaceViewの準備ができたらボタンを操作可能にする
            mPlayPauseButton.setAlpha(1);
            mLoopButton.setAlpha(1);
            mSeekBar.setAlpha(1);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fragment
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MoviePlayerFragment newInstance(String uri) {

        Bundle args = new Bundle();

        MoviePlayerFragment fragment = new MoviePlayerFragment();
        args.putString(PLAYING_URI_KEY,uri);
        fragment.setArguments(args);
        return fragment;
    }

    public MoviePlayerFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_player, container, false);

        // 動画URI
        mPlayingUri = getArguments().getString(PLAYING_URI_KEY);
        Assert.assertNotNull(mPlayingUri);

        // 動画表示部分のSurface
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceCallback);

        // 再生コントロール部分背景
        mControlBackground = view.findViewById(R.id.control_bar_background);
        mControlLayer = view.findViewById(R.id.control_bar_layout);

        // 再生・停止ボタン
        mPlayPauseButton = (Button) view.findViewById(R.id.button_play_pause);
        mPlayPauseButton.setAlpha(0);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
                if (mMediaPlayer == null) {
                    preparePlayer();
                    try {
                        mMediaPlayer.setDataSource(mPlayingUri);
                        mMediaPlayer.setDisplay(mSurfaceHolder);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                        mSeekBar.setMax(mMediaPlayer.getDuration());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                } else {
                    mMediaPlayer.start();
                }
            }
        });

        // 連続再生ボタン
        mLoopButton = (Button) view.findViewById(R.id.button_loop);
        mLoopButton.setAlpha(0);
        mLoopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMediaPlayer.setLooping(!mMediaPlayer.isLooping());
            }
        });

        // シークバー
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        mSeekBar.setAlpha(0);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (null != mMediaPlayer) {
                    if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.seekTo(i);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (null != mMediaPlayer) {
                    mMediaPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (null != mMediaPlayer) {
                    mMediaPlayer.start();
                }
            }
        });

        // 時間表示ラベル
        mLeftTimeText = (TextView) view.findViewById(R.id.left_time_text);
        mRightTimeText = (TextView) view.findViewById(R.id.right_time_text);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRunning();
        releasePlayer();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Private
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * プレイヤー準備 初期化、リスナ設定、プログレス用のスレッド生成
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void preparePlayer() {
        mMediaPlayer = new MediaPlayer();

//        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//            @Override
//            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
//                Log.d("DEBUG","onBufferingUpdate");
//            }
//        });
//
//        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mediaPlayer) {
//                Log.d("DEBUG","onCompletion");
//            }
//        });
//
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d("DEBUG","onError");
                MoviePlayerFragment.this.releasePlayer();
                return false;
            }
        });
//
//        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
//            @Override
//            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
//                Log.d("DEBUG","onInfo");
//                return false;
//            }
//        });
//
//        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mediaPlayer) {
//                Log.d("DEBUG","onPrepared");
//            }
//        });

        // 動画のサイズに応じてSurfaceViewのサイズを変更させる
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mediaPlayer, int w, int h) {
                Log.d("DEBUG","onVideoSizeChanged:"+String.valueOf(w)+":"+String.valueOf(h));
                int winWidth = getView().getWidth();
                int winHeight = getView().getHeight();
                if (w > h || (w == h && winWidth < winHeight)) {
                    int width = winWidth;
                    float p = (float) h / (float) w;
                    int height = (int) ((float) width * p);
                    mSurfaceHolder.setFixedSize(width, height);
                } else  {
                    int height = winHeight;
                    float p = (float) w / (float) h;
                    int width = (int) ((float) height * p);
                    mSurfaceHolder.setFixedSize(width, height);
                }
            }
        });

        // 再生コントロール表示を更新するための設定
        mRunning = true;
        mPlayerThread = new Thread(this);
        mPlayerThread.start();
    }

    /**
     * プレイヤー解放
     */
    private void releasePlayer() {
        if (null == mMediaPlayer) {
            return;
        }
        mMediaPlayer.pause();
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Runnable
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 別スレッドから呼び出される プログレスバーや時間表示を設定する
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mMediaPlayer == null) {
                return;
            }
            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());

            String playButtonText = mMediaPlayer.isPlaying() ? "停止" : "再生";
            mPlayPauseButton.setText(playButtonText);

            String loopButtonText = mMediaPlayer.isLooping() ? "連続" : "一回";
            mLoopButton.setText(loopButtonText);

            // 再生時間表示
            int leftTime = mMediaPlayer.getCurrentPosition()/1000;
            String leftMinText = mFormatterMin.format((int)(leftTime / 60));
            String leftSecText = mFormatterSec.format((int)(leftTime % 60));
            int rightTime = mMediaPlayer.getDuration()/1000 - leftTime;
            String rightMinText = mFormatterMin.format((int)(rightTime / 60));
            String rightSecText = mFormatterSec.format((int)(rightTime % 60));
            mLeftTimeText.setText(leftMinText +":"+ leftSecText);
            mRightTimeText.setText("-"+rightMinText +":"+ rightSecText);
        }
    };

    public void run() {
        while(mRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandler.sendMessage(Message.obtain());
        }
    }

    public void stopRunning() {
        mRunning = false;
    }

}
