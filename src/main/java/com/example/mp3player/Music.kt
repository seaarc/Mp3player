package com.example.mp3player

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.IOException
import java.io.Serializable

class Music(var id: String, var title: String?, var artist: String?, var albumId: String?, var duration: Long?, var favorites: Int): Serializable {

    // contents resolver를 이용해 앨범 정보의 url을 가져옴
    fun getAlbumUri(): Uri {
        return Uri.parse("content://media/external/audio/albumart/$albumId")
    }

    // 음악 정보를 가져오기 위한 경로 Uri
    fun getMusicUri(): Uri {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    // 음악의 앨범 이미지를 가져와 원하는 사이즈로 비트맵 만들기
    fun getAlbumArt(context: Context, albumArtWidth: Int, albumArtHeight: Int): Bitmap? {
        val contentResolver: ContentResolver = context.getContentResolver()
        val uri = getAlbumUri()
        val options = BitmapFactory.Options()

        if(uri != null) {
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            try {
                // contentResolver를 통해 외부 파일에 있는 이미지 정보(uri)를 가져오는 스트림
                parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                var bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor!!.fileDescriptor, null, options)

                // 비트맵을 가져와서 사이즈 설정
                if(bitmap != null) {
                    if(options.outWidth != albumArtWidth || options.outHeight != albumArtHeight) {
                        val tempBitmap = Bitmap.createScaledBitmap(bitmap, albumArtWidth, albumArtHeight, true)
                        bitmap.recycle()
                        bitmap = tempBitmap
                    }
                }
                return bitmap
            }catch (e: Exception) {
                Log.d("log", "getAlbumArt() ${e.toString()}")
            }finally {
                try {
                    parcelFileDescriptor?.close()
                }catch (e: IOException) {
                    Log.d("log", "parcelFileDescriptor?.close() ${e.toString()}")
                }
            }
        }// end of if(uri != null)

        return null
    }
}