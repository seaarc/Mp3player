package com.example.mp3player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mp3player.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    // 데이터베이스 객체화
    val dbHelper by lazy { DBHelper(this, DBHelper.VERSION) }
    // musicList
    var playList: MutableList<Music> = mutableListOf<Music>()
    var searchedList: MutableList<Music> = mutableListOf<Music>()
    var favoriteList: MutableList<Music> = mutableListOf<Music>()
    // 토글
    lateinit var toggle: ActionBarDrawerToggle

    // 퍼미션 요청
    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val REQUEST_READ = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        // 툴바
        setSupportActionBar(binding.toolbarMain)
        // 토글 구현
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.drawer_open, R.string.drawer_close)
        // 툴 바 업 버튼, 타이틀 설정
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        // 업 버튼을 햄버거 버튼 모양으로 설정
        toggle.syncState()

        //드로어 레이아웃에 네비게이션 메뉴 이벤트 설정
        binding.navigationView.setNavigationItemSelectedListener(this)

        // 외부 파일 접근 요청 승인 여부
        if (ContextCompat.checkSelfPermission(this, permission[0]) == PackageManager.PERMISSION_GRANTED) {
            // 접근 승인
            startProcess()
        }else {
            // 접근 거부 시 재요청
            ActivityCompat.requestPermissions(this, permission, REQUEST_READ)
            // 요청이 승인된다면 onRequestPermissionResult 콜백 함수로 승인 결과값을 알려줌
        }
    }

    // 접근 승인 시 음악 파일을 가져와 컬렉션 프레임워크에 저장하고, 어댑터를 호출해 리사이클러뷰에 넣음
    private fun startProcess() {
        var playList: MutableList<Music>? = mutableListOf<Music>()

        // musicTBL에서 데이터를 가져오는데 1) 테이블에 데이터가 있는 경우 바로 출력 / 2) 테이블에 데이터가 없는 경우 getPlayList()로 음악 가져와 musicTBL에 저장
        playList = dbHelper.selectAllData()
        // 2) 경우
        if(playList.size <= 0) {
            playList = getMusicList()
            for(i in 0..playList!!.size-1) {
                val music = playList[i]
                if(!dbHelper.insertData(music)) {
                    Log.d("log", "음악 데이터 삽입 실패 $music")
                }
            }
            Log.d("log", "테이블에 데이터가 없어 음악 정보 가져옴")
        }else {
            Log.d("log", "테이블에 데이터가 있으니 출력")
        }

        // 리사이클러 뷰로 출력
        // 어댑터를 만들어 MutableList에 제공
        binding.recyclerview.adapter = RecyclerAdapter(this, playList)
        // 레이아웃 설정
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        // 구분선
        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager(this).orientation)
        binding.recyclerview.addItemDecoration(dividerItemDecoration)
    }

    // 콜백 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startProcess()
            } else {
                Toast.makeText(this, "권한을 허용해야 앱을 사용할 수 있습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 데이터 갱신
         // PlayActivity().updateFav()
        playList = dbHelper.selectAllData()
        binding.recyclerview.adapter = RecyclerAdapter(this, playList)
        binding.toolbarMain.title = "음악"

        favoriteList = dbHelper.selectFavorite()
        // binding.recyclerview.adapter = FavoriteAdapter(this@MainActivity, favoriteList)
    }

    // 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // MenuItem 객체 얻기
        val searchMenu = menu?.findItem(R.id.search)
        // MenuItem 객체에 등록된 SearchView 객체 얻기
        val searchView = searchMenu?.actionView as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            // 검색 버튼 클릭 시 발생
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("log", "검색 완료: ${query}")
                return true
            }

            // 검색어를 입력할 때마다 발생
            override fun onQueryTextChange(query: String?): Boolean {
                if(query.isNullOrBlank()) {
                    // 검색어가 없을 경우
                    searchedList.clear()
                    playList = dbHelper.selectAllData()
                    binding.recyclerview.adapter = RecyclerAdapter(this@MainActivity, playList)
                }else {
                    Log.d("log", "검색 진행: ${query}")
                    searchedList = dbHelper.selectData(query)
                    binding.recyclerview.adapter = RecyclerAdapter(this@MainActivity, searchedList)
                }
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    // 옵션 아이템 선택 시
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 토글 버튼 선택 시 드로어 레이아웃 열기
        if(toggle.onOptionsItemSelected(item)) binding.drawerLayout.openDrawer(GravityCompat.START)
        return super.onOptionsItemSelected(item)
    }

    // 네비게이션 뷰 메뉴
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.allList -> {
                playList = dbHelper.selectAllData()
                binding.recyclerview.adapter = RecyclerAdapter(this, playList)
                binding.toolbarMain.title = "음악"
            }

            R.id.favoritesList -> {
                favoriteList = dbHelper.selectFavorite()
                binding.recyclerview.adapter = FavoriteAdapter(this@MainActivity, favoriteList)
                binding.toolbarMain.title = "즐겨찾기"
            }
        }
        binding.drawerLayout.closeDrawers()
        return true
    }

    // 음악 정보를 가져와 playList에 추가
    private fun getMusicList(): MutableList<Music>? {
        // 외부 파일 음악 정보 주소
        val listUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // 요청할 음원 정보 컬럼들
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,     // 음악 정보 ID
            MediaStore.Audio.Media.TITLE,   // 타이틀
            MediaStore.Audio.Media.ARTIST,  // 아티스트
            MediaStore.Audio.Media.ALBUM_ID,    // 앨범 ID
            MediaStore.Audio.Media.DURATION,    // 길이
            MediaStore.Audio.Media.ALBUM,   // 앨범명
            MediaStore.Audio.Media.TRACK    // 트랙 번호
        )

        // content resolver 쿼리에 Uri, 음원 정보 컬럼
        val cursor = contentResolver.query(listUri, proj, null, null, null)
        val musicList: MutableList<Music> = mutableListOf<Music>()
        while(cursor?.moveToNext() == true) {
            val id = cursor.getString(0)
            var title = cursor.getString(1).replace("'", "")
            var artist = cursor.getString(2).replace("'", "")
            var albumId = cursor.getString(3)
            var duration = cursor.getLong(4)
            var favorites = 0
            var albumName = cursor.getString(5).replace("'", "")
            var track = cursor.getString(6)

            val music = Music(id, title, artist, albumId, duration, favorites, albumName, track)
            musicList?.add(music)
        }
        cursor!!.close()
        return musicList
    }

    // 데이터 삭제를 리사이클러뷰에 반영
    @SuppressLint("NotifyDataSetChanged")
    fun deleteMusic(position: Int): MutableList<Music>? {
        var playList: MutableList<Music>? = mutableListOf<Music>()
        playList = dbHelper.selectAllData()
        var dialog = Dialog(applicationContext)

        // 다이얼로그 이벤트 핸들러 등록
        val eventHandler = object: DialogInterface.OnClickListener{
            override fun onClick(p0: DialogInterface?, p1: Int) {
                when(p1){
                    DialogInterface.BUTTON_POSITIVE -> {
                        if(dbHelper.deleteData(playList[position].id)) {
                            Toast.makeText(this@MainActivity, "${playList[position].title} 삭제", Toast.LENGTH_LONG).show()
                            playList.removeAt(position)
                            binding.recyclerview.adapter!!.notifyDataSetChanged()
                        }else {
                            Toast.makeText(this@MainActivity, "${playList[position].title} 삭제 실패", Toast.LENGTH_LONG).show()
                        }
                        binding.recyclerview.adapter = RecyclerAdapter(this@MainActivity, playList)
                        dialog.dismiss()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                }
            }
        }

        dialog = AlertDialog.Builder(this).run {
            setMessage("${playList[position].title} 삭제하시겠습니까?")
            setPositiveButton("삭제", eventHandler)
            setNegativeButton("취소", eventHandler)
            show()
        }

        return playList
    }

}