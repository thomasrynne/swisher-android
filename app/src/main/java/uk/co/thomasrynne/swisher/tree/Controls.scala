package uk.co.thomasrynne.swisher.tree

import android.content.{Context, Intent}
import de.greenrobot.event.EventBus
import org.json.JSONObject
import uk.co.thomasrynne.swisher._
import uk.co.thomasrynne.swisher.sources._
import uk.co.thomasrynne.swisher.util.MediaStore

/**
 */
class Controls(context:Context, mediaStore:MediaStore) {

  def menuForPath(paths:Array[String]) = {
    var menu = rootMenu
    paths.foreach(path => menu = menu.subMenu(path))
    menu
  }
  def rootMenu:Menu = {
    new Menu {
      def items = rootItems
      def path = ""
      def name: String = "Swisher"
      def search = None
    }
  }

  val actions = {
    val x = List("stop", "pause", "next", "previous").map { name =>
      val json = new JSONObject
      json.put("action", name)
      ItemMenuItem(name, None, json, None)
    }
    Menu("Actions", x)
  }
  private val printCards = ActionMenuItem("Print Cards", () => {
    //val intent = new Intent(activity, classOf[PrintCardsPreview])
    //activity.startActivity(intent)
  })
  private val recordPlaylist = ActionMenuItem("Record Playlist", () => {
    EventBus.getDefault().post(new Events.RecordPlayListEvent())
  })
  private val clearPlaylist = ActionMenuItem("Clear Playlist", () => {
    EventBus.getDefault.post(new Events.ClearPlayListEvent)
  })
  private val refreshAlbumCovers = ActionMenuItem("Refresh Album Covers", () => {
    EventBus.getDefault.post(new Events.RefreshAlbumCoversEvent)
  })

  val mediaStoreSource = new MediaStoreSource(mediaStore)
  val youTubeSource = new YouTubeSource(context)
  val podcasts = new Podcasts(context)
  val rootItems = List(
    mediaStoreSource.songsMenu,
    mediaStoreSource.albumsMenu,
    actions,
    RadioStations.menu,
    youTubeSource.RootMenu,
    podcasts.PodcastMenu,
    printCards,
    recordPlaylist,
    clearPlaylist,
    refreshAlbumCovers
  )
}