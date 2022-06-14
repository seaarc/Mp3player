package com.example.mp3player

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DBHelper(context: Context, version: Int): SQLiteOpenHelper(context, "playmusicDB", null, version) {

    companion object{
        val TBL_NAME = "playmusicTBL"
        val VERSION = 1
    }

    // 테이블 설계
    override fun onCreate(db: SQLiteDatabase?) {
        val createQuery = "create table ${TBL_NAME}(id text primary key, title text, artist text, albumId text, duration integer, favorites integer, albumName text, track text)"
        db?.execSQL(createQuery)
    }

    // 테이블 재설계 - 버전 업그레이드 (데이터베이스를 다시 세팅할 때)
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropQuery = "drop table $TBL_NAME"
        db?.execSQL(dropQuery)
        // onCreate를 재호출하여 테이블 업데이트
        this.onCreate(db)
    }

    //새로운 음악 추가
    fun insertData(music: Music):Boolean {
        val db = this.writableDatabase
        var insertFlag = false
        val insertQuery = "insert into ${TBL_NAME}(id, title, artist, albumId, duration, favorites, albumName, track)" +
                "values('${music.id}', '${music.title}', '${music.artist}', '${music.albumId}', ${music.duration}, ${music.favorites}, '${music.albumName}', '${music.track}')"

        try {
            db.execSQL(insertQuery)
            insertFlag = true
        }catch (e: SQLException){
            Log.d("log","Insert SQLException Error: ${e.printStackTrace()}")
        }finally {
            db.close()
        }

        return insertFlag
    }

    // 데이터 전체 조회 (출력)
    fun selectAllData(): MutableList<Music> {
        val db = this.readableDatabase
        var musicList : MutableList<Music> = mutableListOf<Music>()
        val selectQuery = "select * from $TBL_NAME"
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery,null)

            if(cursor.count > 0){
                //커서를 다음 행으로 이동
                while(cursor.moveToNext()){
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorites = cursor.getInt(5)
                    val albumName = cursor.getString(6)
                    val track = cursor.getString(7)
                    val music = Music(id, title, artist, albumId, duration, favorites, albumName, track)
                    musicList.add(music)
                }
            }
        }catch (e: Exception){
            Log.d("log", "SelectAllData error: ${e.printStackTrace()}")
        }finally {
            cursor?.close()
            db.close()
        }
        return musicList
    }

    // 데이터 조건 조회 (검색)
    fun selectData(query: String): MutableList<Music>{
        val db = this.readableDatabase
        var musicList: MutableList<Music> = mutableListOf()
        var cursor: Cursor? = null

        val selectQuery = "select * from $TBL_NAME where title like '%$query%' or artist like '%$query%'"

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorites = cursor.getInt(5)
                    val albumName = cursor.getString(6)
                    val track = cursor.getString(7)
                    val music = Music(id, title, artist, albumId, duration, favorites, albumName, track)
                    musicList.add(music)
                }
            }
        } catch (e: Exception){
            Log.d("log", "SelectData error: ${e.printStackTrace()}")
        } finally {
            cursor?.close()
            db.close()
        }

        return musicList
    }

    // 데이터 삭제
    fun deleteData(id: String): Boolean {
        val db = this.writableDatabase
        var deleteFlag = false
        val deleteQuery = "delete from $TBL_NAME where id = '$id'"
        try {
            db.execSQL(deleteQuery)
            deleteFlag = true
        }catch (e: Exception) {
            Log.d("log", "DeleteData error: ${e.printStackTrace()}")
        }finally {
            db.close()
        }
        return deleteFlag
    }

    // 즐겨찾기
    fun updateFavorites(music: Music): Boolean {
        val db = this.writableDatabase
        var updateFlag = false
        val updateQuery = "update $TBL_NAME set favorites = ${music.favorites} where id = '${music.id}'"

        try {
            db.execSQL(updateQuery)
            Log.d("log", "등록한 페이버릿: ${music.favorites}")
            updateFlag = true
        }catch (e: SQLException){
            Log.d("log","UpdateFavorites SQLException Error: ${e.printStackTrace()}")
        }

        return updateFlag
    }

    // 즐겨찾기 목록 조회
    fun selectFavorite(): MutableList<Music> {
        val db = this.readableDatabase
        var favoritesList: MutableList<Music> = mutableListOf()

        val selectQuery = "select * from $TBL_NAME where favorites = 1"
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(selectQuery, null)
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val favorites = cursor.getInt(5)
                    val albumName = cursor.getString(6)
                    val track = cursor.getString(7)
                    favoritesList.add(Music(id, title, artist, albumId, duration, favorites, albumName, track))
                }
            }
        } catch (e: Exception) {
            Log.d("log", "SelectFavorite error: ${e.printStackTrace()}")
        } finally {
            cursor?.close()
            db.close()
        }

        return favoritesList
    }
}