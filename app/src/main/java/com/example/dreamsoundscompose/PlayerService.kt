package com.example.dreamsoundscompose


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.AssetDataSource
import com.google.android.exoplayer2.upstream.DataSource

private const val TAG = "PlayerService"

class PlayerService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    private val dreamSoundsAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(dreamSoundsAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
        }
    }

    private var mediaSources: MutableMap<String, MediaSource> = mutableMapOf()

    override fun onCreate() {

        super.onCreate()

        Log.d(TAG, "onCreate")

        // Create the media player and all the sounds as media sources.
        val dataSourceFactory = DataSource.Factory { AssetDataSource(this@PlayerService) }
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        for ((soundId, sound) in SOUNDS) {
            val uri = Uri.parse("assets:///${sound.filename}")
            val mediaItem = MediaItem.fromUri(uri)
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
            val loopingMediaSource = LoopingMediaSource(mediaSource)
            mediaSources[soundId] = loopingMediaSource
        }

        // Create a media session.
        mediaSession = MediaSessionCompat(applicationContext, MEDIA_SESSION_ID).apply {
            isActive = true

            // Set our own media browser service session token to the media session token
            // otherwise MediaBrowser clients will not be able to connect
            setSessionToken(sessionToken)

            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                    .setState(PlaybackStateCompat.STATE_PAUSED, PLAYBACK_POSITION_UNKNOWN, 1.0F)
                    .build()
            )
        }
        mediaSession.setCallback(MediaSessionCallback())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession.run {
            isActive = false
            release()
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren for parent id $parentId")
        result.sendResult(null)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {

        Log.d(TAG, "onGetRoot called by $clientPackageName")
        return BrowserRoot(clientPackageName, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        Log.d(TAG, "Creating notification channel")
        val name = getString(R.string.app_name)
        val descriptionText = getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setSound(null, null)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand ${intent?.extras}")

        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        val pausedState: PlaybackStateCompat = PlaybackStateCompat.Builder()
            .setState(
                PlaybackStateCompat.STATE_PAUSED,
                PLAYBACK_POSITION_UNKNOWN,
                1.0F
            )
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .build()

        val playingState: PlaybackStateCompat = PlaybackStateCompat.Builder()
            .setState(
                PlaybackStateCompat.STATE_PLAYING,
                PLAYBACK_POSITION_UNKNOWN,
                1.0F
            )
            .setActions(PlaybackStateCompat.ACTION_PAUSE)
            .build()

        val pauseAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(
                this@PlayerService, R.drawable.ic_baseline_pause_24
            ),
            "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_PAUSE
            )
        ).build()

        val playAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(
                this@PlayerService, R.drawable.ic_play_arrow
            ),
            "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_PLAY
            )
        ).build()

        fun createNotification(isPlaying: Boolean): Notification {
            val notificationBuilder = NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0)
                )
                .setContentTitle(
                    mediaSession.controller.metadata.getText(MediaMetadata.METADATA_KEY_TITLE)
                        .toString()
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOnlyAlertOnce(true)

            if (isPlaying) {
                notificationBuilder.addAction(pauseAction)
            } else {
                notificationBuilder.addAction(playAction)
            }

            return notificationBuilder.build()
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "mediaSession onPlayFromMediaId $mediaId")
            super.onPlayFromMediaId(mediaId, extras)

            exoPlayer.setMediaSource(mediaSources[mediaId]!!)
            exoPlayer.prepare()
            exoPlayer.play()

            // Update the media session
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, mediaId)
                    .build()
            )

            val notification = createNotification(true)

            mediaSession.setPlaybackState(playingState)
            notificationManager.notify(NOTIFICATION_ID, notification)
            startForeground(NOTIFICATION_ID, notification)
        }

        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "mediaSession onPlay")

            exoPlayer.play()
            mediaSession.setPlaybackState(playingState)

            val notification = createNotification(true)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "mediaSession onPause")
            exoPlayer.pause()
            mediaSession.setPlaybackState(pausedState)
            notificationManager.notify(NOTIFICATION_ID, createNotification(false))
        }
    }
}