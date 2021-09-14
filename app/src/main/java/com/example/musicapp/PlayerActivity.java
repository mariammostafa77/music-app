package com.example.musicapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static com.example.musicapp.ApplicationClass.ACTION_NEXT;
import static com.example.musicapp.ApplicationClass.ACTION_PLAY;
import static com.example.musicapp.ApplicationClass.ACTION_PREVIOUS;
import static com.example.musicapp.ApplicationClass.CHANNEL_ID_2;
import static com.example.musicapp.MainActivity.musicFiles;
import static com.example.musicapp.MainActivity.repeatBoolean;
import static com.example.musicapp.MainActivity.shuffleBoolean;

public class PlayerActivity extends AppCompatActivity implements  ActionPlaying , ServiceConnection {

    private TextView song_name, artist_name, duration_displayed, duration_total;
    private ImageView cover_art, nextBtn, prevBtn, shuffleBtn, repeatBtn, backBtn;
    private FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    private int position=-1;
    public static ArrayList<MusicFiles>listSongs=new ArrayList<>();
    public static Uri uri;
    //public static MediaPlayer mediaPlayer;
    private Handler handler=new Handler();
    private Thread playThread, prevThread, nextThread;
    MusicService musicService;
    //MediaSessionCompat mediaSessionCompat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        //mediaSessionCompat=new MediaSessionCompat(getBaseContext(),"My Audio");
        initViews();
        getIntentMethod();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(musicService != null && fromUser){
                    musicService.seekTo(progress*1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){
                    int mCurrentPosition=musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_displayed.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this,1000);
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shuffleBoolean){
                    shuffleBoolean=false;
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_24);
                }
                else{
                    shuffleBoolean=true;
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle_on);
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(repeatBoolean){
                    repeatBoolean=false;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_24);
                }
                else{
                    repeatBoolean=true;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_on);
                }
            }
        });

    }


    @Override
    protected void onResume() {
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private String formattedTime(int mCurrentPosition) {
        String totalOut="";
        String totalNew="";
        String seconds=String.valueOf(mCurrentPosition % 60);
        String minutes=String.valueOf(mCurrentPosition /60);
        totalOut=minutes+ ":" + seconds;
        totalNew=minutes+ ":" + "0"+seconds;
        if (seconds.length()==1){
            return totalNew;
        }
        else{
            return totalOut;
        }

    }

    private void getIntentMethod() {
        position=getIntent().getIntExtra("position",-1);
        listSongs=musicFiles;
        if(listSongs != null){
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
            uri=Uri.parse(listSongs.get(position).getPath());
        }
        Intent intent=new Intent(this,MusicService.class);
        intent.putExtra("servicePosition",position);
        startService(intent);

    }

    private void prevThreadBtn() {
        prevThread=new Thread(){
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if(shuffleBoolean && ! repeatBoolean){
                position=getRandom(listSongs.size() - 1);
            }
            else if(! shuffleBoolean && ! repeatBoolean){
                position=((position - 1) <0? (listSongs.size()-1) :(position-1));
            }
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.showNotification(R.drawable.ic_baseline_pause_24);
            musicService.onCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
            musicService.start();
        }
        else{
            musicService.stop();
            musicService.release();
            if(shuffleBoolean && ! repeatBoolean){
                position=getRandom(listSongs.size() - 1);
            }
            else if(! shuffleBoolean && ! repeatBoolean){
                position=((position - 1) <0? (listSongs.size()-1) :(position-1));
            }
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_baseline_play_arrow_24);
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
    }

    private void nextThreadBtn() {
        nextThread=new Thread(){
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if(musicService.isPlaying()){
            musicService.stop();
            musicService.release();
            if(shuffleBoolean && ! repeatBoolean){
                position=getRandom(listSongs.size() - 1);
            }
            else if(! shuffleBoolean && ! repeatBoolean){
                position=((position+1) % listSongs.size());
            }
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
            musicService.showNotification(R.drawable.ic_baseline_pause_24);
            musicService.start();
        }
        else{
            musicService.stop();
            musicService.release();
            if(shuffleBoolean && !repeatBoolean){
                position=getRandom(listSongs.size()-1);
            }
            else if(! shuffleBoolean && ! repeatBoolean){
                position=((position+1) % listSongs.size());
            }
            uri=Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
            musicService.onCompleted();
            musicService.showNotification(R.drawable.ic_baseline_play_arrow_24);
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
    }

    private int getRandom(int i) {
        Random random=new Random();
        return random.nextInt(i + 1);
    }

    private void playThreadBtn() {
        playThread=new Thread(){
            @Override
            public void run() {
                super.run();
                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {
        if(musicService.isPlaying()){
            playPauseBtn.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            musicService.showNotification(R.drawable.ic_baseline_play_arrow_24);
            musicService.pause();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }
        else {
            playPauseBtn.setImageResource(R.drawable.ic_baseline_pause_24);
           musicService.showNotification(R.drawable.ic_baseline_pause_24);
            musicService.start();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition=musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this,1000);
                }
            });
        }
    }

    private void initViews(){
        song_name=findViewById(R.id.song_name);
        artist_name=findViewById(R.id.song_artist);
        duration_displayed=findViewById(R.id.durationPlayed);
        duration_total=findViewById(R.id.durationTotal);
        cover_art=findViewById(R.id.cover_art);
        nextBtn=findViewById(R.id.id_next);
        prevBtn=findViewById(R.id.id_prev);
        repeatBtn=findViewById(R.id.id_repeat);
        shuffleBtn=findViewById(R.id.id_shuffle);
        backBtn=findViewById(R.id.back_btn);
        playPauseBtn=findViewById(R.id.play_pause);
        seekBar=findViewById(R.id.seekBar);

    }
    private  void metaData(Uri uri){
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal=Integer.parseInt(listSongs.get(position).getDuration())/1000;
        duration_total.setText(formattedTime(durationTotal));
        byte [] art=retriever.getEmbeddedPicture();
        try {
            if(art != null){
                Glide.with(this).asBitmap().load(art).into(cover_art);
            }
            else{
                Glide.with(this).asBitmap().load(R.drawable.music).into(cover_art);
            }
        }catch (Exception e){

        }


    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder=(MusicService.MyBinder) service;
        musicService=myBinder.getService();
        musicService.setCallBack(this);
        Toast.makeText(this,"connected"+musicService,Toast.LENGTH_SHORT).show();
        seekBar.setMax(musicService.getDuration()/1000);
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());
        musicService.onCompleted();
        musicService.showNotification(R.drawable.ic_baseline_pause_24);

    }
    @Override
    public void onServiceDisconnected(ComponentName name) {

        musicService=null;

    }

}