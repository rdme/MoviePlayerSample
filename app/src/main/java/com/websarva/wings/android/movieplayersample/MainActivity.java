package com.websarva.wings.android.movieplayersample;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MoviePlayerFragment f = MoviePlayerFragment.newInstance("https://player.vimeo.com/external/159035843.hd.mp4?s=d322529c7bf7f6638f992970fa01119eea37151f&profile_id=119&oauth2_token_id=57447761");
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,f).commit();

//        MoviePlayerFragment f = MoviePlayerFragment.newInstance("https://dwknz3zfy9iu1.cloudfront.net/uscenes_h-264_hd_test.mp4");
//        MoviePlayerFragment f = MoviePlayerFragment.newInstance("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");

    }

}
