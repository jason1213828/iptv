package com.example.iptvplayer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private List<Channel> channelList = new ArrayList<>();
    private int currentChannelIndex = 0;

    private static final String IPTV_M3U_URL = "https://itv.aptv.app/china-iptv/sdyd.m3u";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerView = new PlayerView(this);
        setContentView(playerView);

        player = ExoPlayerFactory.newSimpleInstance(this);
        playerView.setPlayer(player);

        new LoadM3UAsyncTask().execute(IPTV_M3U_URL);
    }

    private void playChannel(String url) {
        if (player == null) return;
        MediaSource mediaSource = new HlsMediaSource.Factory(new DefaultHttpDataSourceFactory("iptv-player"))
                .createMediaSource(android.net.Uri.parse(url));
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            currentChannelIndex = (currentChannelIndex - 1 + channelList.size()) % channelList.size();
            playChannel(channelList.get(currentChannelIndex).url);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            currentChannelIndex = (currentChannelIndex + 1) % channelList.size();
            playChannel(channelList.get(currentChannelIndex).url);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private class LoadM3UAsyncTask extends AsyncTask<String, Void, List<Channel>> {
        @Override
        protected List<Channel> doInBackground(String... strings) {
            List<Channel> channels = new ArrayList<>();
            try {
                URL url = new URL(strings[0]);
                InputStream is = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                String name = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#EXTINF:")) {
                        int commaIndex = line.indexOf(",");
                        if (commaIndex != -1 && commaIndex < line.length() - 1) {
                            name = line.substring(commaIndex + 1);
                        }
                    } else if (!line.startsWith("#") && line.length() > 0) {
                        if (name != null) {
                            channels.add(new Channel(name, line));
                            name = null;
                        }
                    }
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return channels;
        }

        @Override
        protected void onPostExecute(List<Channel> channels) {
            if (channels.isEmpty()) {
                Toast.makeText(MainActivity.this, "频道列表为空", Toast.LENGTH_LONG).show();
                return;
            }
            channelList.clear();
            channelList.addAll(channels);
            playChannel(channelList.get(0).url);
        }
    }

    private static class Channel {
        String name;
        String url;

        Channel(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }
}