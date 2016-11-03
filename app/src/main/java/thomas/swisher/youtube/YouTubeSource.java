package thomas.swisher.youtube;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Value;
import lombok.val;
import thomas.swisher.MediaHandler;
import thomas.swisher.shared.Core;
import thomas.swisher.tree.Menus;
import thomas.swisher.ui.MainActivity;
import thomas.swisher.utils.Utils;

/**
 * Adds youtube support.
 *
 * Includes menu items for the user's Likes & Favorites as well as an embedded youtube player.
 */
public class YouTubeSource {

    private static final String MENU_NAME = "You Tube";
    public static final Core.MenuPath MENU_PATH = new Core.MenuPath(new String[] { MENU_NAME });

    private static final String PLAY_SERVICES_EXPLANATION =
        "Swisher uses the app 'Play-Services' to access You Tube Likes & Favorites. " +
        "You need to install the latest version to create You Tube swisher cards";
    private static final String YOUTUBE_PERMISSION_EXPLANATION =
            "To create swisher cards for YouTube you need to give swisher permission to " +
            "see the name(s) of your google accounts and read-only access to your youtube account." +
            "Swisher will read your history/watch-later & likes when you use the youtube menus here" +
            "This will not be shared or leave the device. When you record a device only the videoID is shared";

    private final YouTubeApi youTubeApi;

    public MediaHandler videoHandler() {
        return new MediaHandler() {
            @Override
            public Optional<PlaylistEntry> handle(Utils.FlatJson json) {
                if (json.has("youtube_video") && json.has("youtube_title") && json.has("youtube_thumbnail")) {
                    val videoID = json.get("youtube_video");
                    val title = json.get("youtube_title");
                    val thumbnail = Uri.parse(json.get("youtube_thumbnail"));
                    val track = new YouTubeApi.YouTubeVideo(videoID, title, thumbnail);
                    return Optional.of(new PlaylistEntry(title, Optional.of(thumbnail), Collections.singletonList(track),
                            (playNow, currentTrack, playNext, listener) ->
                                    new YouTubeTracksPlayer(playNow, videoID, listener)
                    ));
                } else {
                    return Optional.absent();
                }
            }
        };
    }

    @Value
    private class YouTubeChannelMenu implements Menus.Menu {
        private final String name;
        private final String channel;

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Menus.MenuEntry> items() {
            return safeMenus(() ->
                FluentIterable.from(youTubeApi.userChannel(channel)).transform((item) -> {
                    return (Menus.MenuEntry) new Menus.FixedItemMenuEntry(new Core.PlaylistItemUIMenuItem(
                            item.name(), item.toJson(), Optional.of(item.thumbnail())));
                }).toList()
            );
        }
    }

    public YouTubeSource(YouTubeApi youTubeApi) {
        this.youTubeApi = youTubeApi;
    }

    public Menus.Menu menu() {
        return new Menus.Menu() {
            @Override
            public String name() {
                return MENU_NAME;
            }

            @Override
            public List<Menus.MenuEntry> items() {
                return safeMenus(() -> {
                    youTubeApi.checkToken(); //this throws an exception if the device does not
                                             //have permission to access youtube yet
                    return userChannelMenus;
                });
            }
        };

    }

    interface CreateMenus {
        List<Menus.MenuEntry> menus() throws SecurityException, GoogleAuthException, IOException;
    }

    private List<Menus.MenuEntry> safeMenus(CreateMenus create) {
        try {
            return create.menus();
        } catch (SecurityException e) {
            return Arrays.asList(new Menus.FixedItemMenuEntry(new Core.ErrorMenuItem(e.getMessage())));
        } catch (GooglePlayServicesAvailabilityException playServicesException) {
            return setupRequiredMenuItems(
                PLAY_SERVICES_EXPLANATION, "Install Latest Play-Services", playServicesException.getIntent());
        } catch (UserRecoverableAuthException userRecoverableAuthException) {
            return setupRequiredMenuItems(
                YOUTUBE_PERMISSION_EXPLANATION, "Next", userRecoverableAuthException.getIntent());
        } catch (Exception e) {
            return Arrays.asList(new Menus.FixedItemMenuEntry(new Core.ErrorMenuItem(e.getMessage())));
        }
    }

    private List<Menus.MenuEntry> setupRequiredMenuItems(
            String explanation, String buttonText, Intent recoveryIntent) {
        return Arrays.asList(
            new Menus.FixedItemMenuEntry(new Core.ErrorMenuItem(explanation)),
            new Menus.FixedItemMenuEntry(new Core.DoItActivityUIMenuItem(
                buttonText,
                (core, activity) -> {
                    activity.startActivityForResult(recoveryIntent, MainActivity.YOUTUBE_AUTH_REQUEST_CODE);
                }
            )),
            new Menus.FixedItemMenuEntry(new Core.DoItActivityUIMenuItem(
                "Retry",
                (core, activity) -> core.refresh()
            ))
        );
    }

    private final List<Menus.MenuEntry> userChannelMenus = Arrays.asList(
        new Menus.SubMenuMenuEntry(new YouTubeChannelMenu("Favorites", "favorites")),
        new Menus.SubMenuMenuEntry(new YouTubeChannelMenu("Likes", "likes"))
    );
}