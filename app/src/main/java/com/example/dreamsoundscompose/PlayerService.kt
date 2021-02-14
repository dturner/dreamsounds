package com.example.dreamsoundscompose


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService.BrowserRoot.EXTRA_RECENT
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver


/**
 * TODO
 *
 * - Cast not supported yet
 * - Notification sound is currently played and it shouldn't be (should it?)
 * - Tapping the pause button does nothing, although onStartCommand should be called
 * - All media controls except the pause button do nothing because they don't have intents
 * - The media player is prepared synchronously inside onCreate (which results in an HTTP
 * connection), I feel like this should be async
 *
 */

class PlayerService : MediaBrowserServiceCompat() {

    //private lateinit var mSelector: MediaRouteSelector
    //private lateinit var mRouter: MediaRouter
    var rootCallCount = 0
    private lateinit var mediaPlayer: MediaPlayer

    private var isPrepared = false
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {

        super.onCreate()

        Log.d(TAG, "onCreate")



        // Create and prepare a media player
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource("https://storage.googleapis.com/automotive-media/The_Messenger.mp3")
            setOnPreparedListener {
                isPrepared = true
            }
            prepare()
        }

        // Create a media session
        mediaSession = MediaSessionCompat(applicationContext, MEDIA_SESSION_ID).apply {
            isActive = true

            // Set our own media browser service session token to the media session token
            // otherwise MediaBrowser clients will not be able to connect
            setSessionToken(sessionToken)
            setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, TEST_METADATA_TITLE)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, TEST_METADATA_ARTIST)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, TEST_METADATA_DURATION)
                    .putString(
                        MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                        getUriFromResource(TEST_METADATA_ALBUM_ART_RESOURCE_ID).toString()
                    )
                    .build()
            )
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0F)
                    .build()
            )
        }

        mediaSession.setCallback(MediaSessionCallback()) // TODO setting this inside the `apply` block causes the media session to not be created?

        createNotificationChannel()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        mediaSession.run {
            isActive = false
            release()
        }

        mediaPlayer.stop()
        mediaPlayer.release()
        isPrepared = false
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren for parent id $parentId")

        if (parentId.startsWith("com.android.systemui/recent")) {

            val description = MediaDescriptionCompat.Builder()
                .setTitle("Recent title $rootCallCount")
                .setSubtitle("Recent artist")
                .setIconUri(getUriFromResource(TEST_METADATA_ALBUM_ART_RESOURCE_ID))
                .setMediaId(parentId)
                .build()
            val item = MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
            val itemList = mutableListOf(item)
            result.sendResult(itemList)
        } else {
            result.sendResult(null)
        }

        rootCallCount++
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {

        Log.d(TAG, "onGetRoot called by $clientPackageName")

        var rootId = clientPackageName

        if (rootHints != null) {
            Log.d(TAG, "root hints: $rootHints")
            if (rootHints.containsKey(EXTRA_RECENT)) rootId = "$rootId/recent"
        }

        return BrowserRoot(rootId, null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val name = getString(R.string.app_name)
        val descriptionText = getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
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

        val playbackSpeed = 1.0f

        val pauseAction = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(
                this@PlayerService, R.drawable.ic_baseline_pause_24
            )
            ,
            "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                applicationContext,
                PlaybackStateCompat.ACTION_PAUSE
            )
        ).build()

        val notification: Notification =
            NotificationCompat.Builder(this@PlayerService, CHANNEL_ID)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(pauseAction)
                .setOnlyAlertOnce(true)
                .build()

        override fun onPlay() {
            Log.d(TAG, "mediaSession onPlay")

            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, this@PlayerService.javaClass)
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
            startForeground(NOTIFICATION_ID, notification)

            mediaPlayer.start()
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        mediaPlayer.currentPosition.toLong(),
                        playbackSpeed
                    )
                    .setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO
                                or PlaybackStateCompat.ACTION_PLAY
                                or PlaybackStateCompat.ACTION_PAUSE
                    )
                    .build()
            )
        }

        override fun onPause() {

            Log.d(TAG, "mediaSession onPause")
            mediaPlayer.pause()
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        mediaPlayer.currentPosition.toLong(),
                        playbackSpeed
                    )
                    .setActions(
                        PlaybackStateCompat.ACTION_SEEK_TO
                                or PlaybackStateCompat.ACTION_PLAY
                                or PlaybackStateCompat.ACTION_PAUSE
                    )
                    .build()
            )
        }

        override fun onStop() {
            Log.d(TAG, "mediaSession onStop")
            mediaPlayer.stop()
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder().setState(
                    PlaybackStateCompat.STATE_STOPPED,
                    0L,
                    playbackSpeed
                ).build()
            )
            stopForeground(/* removeNotification= */true)
            stopSelf()
        }

        override fun onSeekTo(pos: Long) {
            Log.d(TAG, "mediaSession onSeekTo")
            mediaPlayer.seekTo(pos.toInt())
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder().setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    pos, 1.0f
                ).build()
            )
        }
    }

    private fun getUriFromResource(@DrawableRes resourceId: Int): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(applicationContext.resources.getResourcePackageName(resourceId))
            .appendPath(applicationContext.resources.getResourceTypeName(resourceId))
            .appendPath(applicationContext.resources.getResourceEntryName(resourceId))
            .build()
    }


}


const val APP_ROOT_ID = "app_root_id"
const val SYSTEM_UI_ROOT_ID = "system_ui_root_id"
const val TEST_METADATA_TITLE = "Awakening"
const val TEST_METADATA_ARTIST = "Silent Partner"
const val TEST_METADATA_DURATION = 132000L
const val TEST_METADATA_ALBUM_ART_RESOURCE_ID = R.drawable.babbling_stream