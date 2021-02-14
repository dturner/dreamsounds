package com.example.dreamsoundscompose

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.AssetDataSource
import com.google.android.exoplayer2.upstream.DataSource

const val TAG = "MainActivity"
const val MEDIA_SESSION_ID = "DreamSoundsMediaSessionID"
const val CHANNEL_ID = "DreamSoundsNotificationChannel"
const val NOTIFICATION_ID = 1

data class SoundFile (val imageId: Int, val filename: String, var soundPoolId: Int? = null)

val SOUND_LIST = listOf(
    SoundFile(R.drawable.babbling_stream, "babbling_stream.mp3"),
    SoundFile(R.drawable.city_noise, "city_noise.mp3"),
    SoundFile(R.drawable.crickets, "crickets.mp3"),
    SoundFile(R.drawable.rain, "rain.mp3"),
    SoundFile(R.drawable.thunder_storm, "thunder_storm.mp3"),
    SoundFile(R.drawable.tropical_rainforest, "tropical_rainforest.mp3"),
    SoundFile(R.drawable.waterfall, "waterfall.mp3"),
    SoundFile(R.drawable.waves, "waves.mp3"),
    SoundFile(R.drawable.white_noise, "white_noise.mp3"),
    SoundFile(R.drawable.wind_chimes, "wind_chimes.mp3")
)

class MainActivity : AppCompatActivity() {

    lateinit var exoPlayer : SimpleExoPlayer

    private lateinit var mediaBrowser: MediaBrowser
    private var controller: MediaController? = null
    private lateinit var mediaBrowserConnectionCallback: MediaBrowserConnectionCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(this)

        setContent {
            SoundList {

                if (exoPlayer.isPlaying){
                    exoPlayer.stop()
                }

                val uri = Uri.parse("assets:///${it.filename}")
                val dataSourceFactory = DataSource.Factory { AssetDataSource(this@MainActivity) }
                val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
                val mediaItem = MediaItem.fromUri(uri)
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                val loopingMediaSource = LoopingMediaSource(mediaSource)

                exoPlayer.setMediaSource(loopingMediaSource)
                exoPlayer.prepare()
                exoPlayer.play()


                if (mediaBrowser.isConnected) {
                    if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controller?.transportControls?.pause()
                        //binding.playButton.text = "Play"
                    } else {
                        controller?.transportControls?.play()
                        //binding.playButton.text = "Pause"
                    }
                }

            }
        }

        exoPlayer = SimpleExoPlayer.Builder(this).build()

        mediaBrowser = MediaBrowser(
            this,
            ComponentName(this, PlayerService::class.java),
            mediaBrowserConnectionCallback, null
        ).apply {
            Log.d(TAG, "Connecting to PlayerService")
            connect()
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowser.ConnectionCallback() {
        override fun onConnectionFailed() {
            Log.d(TAG, "Connection failed")
        }

        override fun onConnected() {
            Log.d(TAG, "Connected to media browser, got session token $mediaBrowser.sessionToken")

            // Get a MediaController for the MediaSession.
            controller = MediaController(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
        }

        override fun onConnectionSuspended() {
            Log.d(TAG, "Connection suspended")
            super.onConnectionSuspended()
        }
    }

    private inner class MediaControllerCallback : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed to $state?.state")
            super.onPlaybackStateChanged(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "metadata changed to $metadata?.description")
            super.onMetadataChanged(metadata)
        }
    }
}

@Composable
fun SoundList(onPlaySound: (SoundFile) -> Unit) {

    val imageModifier = Modifier
        .preferredHeight(120.dp)
        .clip(shape = RoundedCornerShape(4.dp))

    ScrollableColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 4.dp)) {
        for (sound in SOUND_LIST) {
            Row {
                Button(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp),
                    contentPadding = PaddingValues(all = 0.dp),
                    onClick = { onPlaySound(sound) },) {
                    val imageResource = imageResource(id = sound.imageId)
                    Image(
                        imageResource,
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

}

@Composable
@Preview
fun DefaultPreview(){
    SoundList(onPlaySound = { /*TODO*/ })
}
