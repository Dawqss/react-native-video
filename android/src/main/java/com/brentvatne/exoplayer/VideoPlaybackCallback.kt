package com.brentvatne.exoplayer

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.brentvatne.exoplayer.VideoPlaybackService.Companion.COMMAND
import com.brentvatne.exoplayer.VideoPlaybackService.Companion.commandFromString
import com.brentvatne.exoplayer.VideoPlaybackService.Companion.handleCommand
import com.google.common.util.concurrent.ListenableFuture
import com.brentvatne.exoplayer.VideoPlaybackService.Companion.SEEK_INTERVAL_MS

class VideoPlaybackCallback(
    private val commandCallback: ((eventType: String, targetTime: Long?) -> Unit)?
) : MediaSession.Callback {
    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
        try {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_SEEK_FORWARD)
                        .add(Player.COMMAND_SEEK_BACK)
                        .build()
                ).setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(COMMAND.SEEK_FORWARD.stringValue, Bundle.EMPTY))
                        .add(SessionCommand(COMMAND.SEEK_BACKWARD.stringValue, Bundle.EMPTY))
                        .build()
                )
                .build()
        } catch (e: Exception) {
            return MediaSession.ConnectionResult.reject()
        }
    }

    override fun onPlayerCommandRequest(session: MediaSession, controller: ControllerInfo, playerCommand: Int): Int {
        when (playerCommand) {
            Player.COMMAND_PLAY_PAUSE -> {
                val eventType = if (session.player.isPlaying) "pause" else "play"
                commandCallback?.invoke(eventType, null)
            }
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                /**
                 * Currently targetTime for seekTo is unavailable
                 * if needed we will fix it
                 */
                commandCallback?.invoke("playbackPositionChanged", 0L);
            }
        }
        return super.onPlayerCommandRequest(session, controller, playerCommand)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (commandFromString(customCommand.customAction)) {
            COMMAND.SEEK_BACKWARD -> {
                val targetTime = session.player.contentPosition - SEEK_INTERVAL_MS;
                commandCallback?.invoke("skipBackward", targetTime);
            }
            COMMAND.SEEK_FORWARD -> {
                val targetTime = session.player.contentPosition + SEEK_INTERVAL_MS;
                commandCallback?.invoke("skipForward", targetTime);
            }
            else -> {}
        }
        handleCommand(commandFromString(customCommand.customAction), session)
        return super.onCustomCommand(session, controller, customCommand, args)
    }
}
