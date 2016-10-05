package uk.co.thomasrynne.swisher.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.{BaseColumns, MediaStore}
import android.provider.MediaStore.Audio.{AudioColumns, AlbumColumns, Media, Albums}
import uk.co.thomasrynne.swisher.model.TrackDescription
import uk.co.thomasrynne.swisher.{Songs}

/**

 */
class MediaStore(context:Context) {
  case class Track(name:String, path:String, artist:String, image:Option[Uri]) extends TrackDescription {
    def imageOrNull = image.getOrElse(null)
  }
  case class Album(name:String, artist:String, id:Int) {
    def image = Option(Songs.imageForAlbum(id))
    def imageOrNull = image.getOrElse(null)
  }

  def allSongs = findTracks("", Array())

  def searchTracks(text:String) = {
    findTracks(android.provider.MediaStore.MediaColumns.TITLE + " like ?", Array("%"+text+"%"))
  }

  def findTrackOrNull(trackName: String, artistName: String): Track = {
    val where: String = android.provider.MediaStore.MediaColumns.TITLE + "=? and " + android.provider.MediaStore.Audio.AlbumColumns.ARTIST + "=?"
    val tracks: Array[Track] = findTracks(where, Array(trackName, artistName))
    tracks.headOption.getOrElse(null)
  }

  def albumAndTracks(name:String, artist:String) = {
    albumForNameAndArtist(name, artist) match {
      case Some(album) => Some((album, albumTracks(album.id)))
      case None => None
    }
  }

  def albumTracks(albumId: Int): Array[Track] = {
    return findTracks(AlbumColumns.ALBUM_ID + " = ?", Array(albumId.toString))
  }

  def allAlbums: Array[Album] = {
    findAlbums(null, null)
  }

  def searchAlbums(text:String): Array[Album] = {
    val selection = AlbumColumns.ALBUM + " like ? or " + MediaStore.Audio.AudioColumns.ARTIST + " like ? "
    val selectionArgs = Array("%" + text + "%", "%" + text + "%")
    findAlbums(selection, selectionArgs)
  }

  private def albumForNameAndArtist(name:String, artist:String) = {
    findAlbums(AlbumColumns.ALBUM + " = ? or " + MediaStore.Audio.AudioColumns.ARTIST + " = ? ", Array(name, artist)).headOption
  }
  private def findTracks(where: String, whereVal: Array[String]): Array[Track] = {
    val column: Array[String] = Array(
      MediaStore.MediaColumns.TITLE,
      MediaStore.MediaColumns.DATA,
      MediaStore.Audio.AudioColumns.ARTIST,
      MediaStore.Audio.AlbumColumns.ALBUM_ID
    )
    val orderBy: String = android.provider.MediaStore.Audio.AudioColumns.TRACK
    val cursor: Cursor = context.getContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, column, where, whereVal, orderBy)
    val titleIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
    val pathIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
    val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST);
    val albumIdIndex: Int = cursor.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_ID)
    val tracks = new Array[Track](cursor.getCount)
    if (cursor.moveToFirst) {
      do {
        tracks(cursor.getPosition) = Track(
          cursor.getString(titleIndex),
          cursor.getString(pathIndex),
          cursor.getString(artistIndex),
          Option(Songs.imageForAlbum(cursor.getInt(albumIdIndex))))
      } while (cursor.moveToNext)
    }
    cursor.close
    return tracks
  }

  private def findAlbums(selection: String, selectionArgs: Array[String]): Array[Album] = {
    val sortOrder: String = AudioColumns.ALBUM + " ASC"
    val projection: Array[String] = Array[String](BaseColumns._ID, AlbumColumns.ALBUM, AudioColumns.ARTIST)
    val cursor: Cursor = context.getContentResolver.query(Albums.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder)
    val albumIndex: Int = cursor.getColumnIndexOrThrow(AudioColumns.ALBUM)
    val artistIndex: Int = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
    val albumIdIndex: Int = cursor.getColumnIndexOrThrow(BaseColumns._ID)
    val albums = new Array[Album](cursor.getCount)
    if (cursor.moveToFirst) {
      do {
        val album = cursor.getString(albumIndex)
        val artist = cursor.getString(artistIndex)
        val albumId = cursor.getInt(albumIdIndex)
        albums(cursor.getPosition) = Album(album, artist, albumId)
      } while (cursor.moveToNext)
    }
    cursor.close()
    albums
  }
}
