package com.example.mp3player

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.example.mp3player.databinding.ActivityPlayBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlayActivity : AppCompatActivity() {
    val binding by lazy { ActivityPlayBinding.inflate(layoutInflater) }
    // 음악플레이어 변수
    private var mediaPlayer: MediaPlayer? = null
    // 음악 정보 객체 변수
    private var currentTrack: Music? = null
    // 음악 위치 객체 변수
    private var pos = 0
    // 앨범 아트 사이즈
    private val ALBUMART_WIDTH = 300
    private val ALBUMART_HEIGHT = 300
    // 코루틴 스코프
    private var playerJob: Job? = null
    // 데이터베이스 객체화
    val dbHelper by lazy { DBHelper(this, DBHelper.VERSION) }
    var adapter: RecyclerAdapter? = null

    var playList: MutableList<Music> = mutableListOf<Music>()
    var from = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 툴바 사용
        setSupportActionBar(binding.toolbarPlay)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 인텐트를 통해 MusicRecyclerAdapter에서 음악 받아오기
        currentTrack = intent.getSerializableExtra("track") as Music
        pos = intent.getIntExtra("pos", 0)
        from = intent.getStringExtra("from").toString()
        if(from == "main") playList = dbHelper.selectAllData()
        else if(from == "favorite") playList = dbHelper.selectFavorite()

        // 음악 준비 및 재생
        prepare(currentTrack)
        mediaPlayer!!.start()
        binding.ivPlay.setImageResource(R.drawable.pause_24)
        playing()
    }

    // 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.play_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    // 옵션 아이템 선택 이벤트
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.goDetail -> {
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("currentTrack", currentTrack)
                this.startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    // 음악을 시작하기 위해 세팅
    fun prepare(track: Music?) {
        if(track != null) {
            // 뷰 설정 - 문자열
            binding.tvTitle.text = track.title
            binding.tvArtist.text = track.artist
            binding.tvDurationStart.text = "00:00"
            binding.tvDurationEnd.text = SimpleDateFormat("mm:ss").format(track.duration)

            // 뷰 설정 - 앨범아트
            val bitmap: Bitmap? = track.getAlbumArt(this, ALBUMART_WIDTH, ALBUMART_HEIGHT)
            if(bitmap != null) {
                binding.ivAlbumArt.setImageBitmap(bitmap)
                setBackground(bitmap)
            }
            else binding.ivAlbumArt.setImageResource(R.drawable.albumart)



            // 즐겨찾기 상태
            if(track.favorites == 0) binding.ivFavorites.setImageResource(R.drawable.star_border_24)
            else binding.ivFavorites.setImageResource(R.drawable.star_24)

            // 음원 생성
            mediaPlayer = MediaPlayer.create(this, track.getMusicUri())

            // seekBar 최대값을 음악 길이와 같게 설정
            binding.seekBar.max = track.duration!!.toInt()

            // seekBar 이벤트로 음악과 동기화 처리
            binding.seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                // seekBar를 터치해서 이동할 때
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // progress로 이동
                    if(fromUser) mediaPlayer!!.seekTo(progress)
                    binding.tvDurationStart.text = SimpleDateFormat("mm:ss").format(progress)
                }

                // seekBar를 터치하는 순간
                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                // seekBar에서 손을 떼는 순간
                override fun onStopTrackingTouch(p0: SeekBar?) {

                }
            })// end of seekBar Listener
        }
    }

    fun onClickView(view: View?) {
        when(view?.id) {
            // 메인(트랙리스트 화면)으로 돌아가기
            R.id.ivList -> {
                // 음악 정지
                mediaPlayer?.stop()
                // 코루틴 해제
                playerJob?.cancel()
                finish()
            }

            // 즐겨찾기
            R.id.ivFavorites -> {
                if(currentTrack!!.favorites == 0) {
                    // 즐겨찾기로 등록되어 있지 않을 때 - 클릭 시 등록
                    currentTrack!!.favorites = 1
                    binding.ivFavorites.setImageResource(R.drawable.star_24)
                }else {
                    // 즐겨찾기로 등록되어 있을 때 - 클릭 시 해제
                    currentTrack!!.favorites = 0
                    binding.ivFavorites.setImageResource(R.drawable.star_border_24)
                }
                if (dbHelper.updateFavorites(currentTrack!!)) Log.d("log", "즐겨찾기: ${currentTrack!!.favorites}")
            }

            // 음악 재생 또는 일시정지
            R.id.ivPlay -> {
                if (mediaPlayer!!.isPlaying) {
                    // 음악이 진행중이라면 일시정지
                    mediaPlayer!!.pause()
                    binding.ivPlay.setImageResource(R.drawable.play_24)
                }else {
                    // 음악이 일시정지 상태라면 시작
                    mediaPlayer!!.start()
                    binding.ivPlay.setImageResource(R.drawable.pause_24)
                    Log.d("log", "현재 음악 pos: $pos")
                    playing()
                }
            }

            // 처음으로 / 이전 곡으로 넘기기
            R.id.ivPrevious -> {
                var start: Long = 0L
                if(mediaPlayer!!.currentPosition > 3000) {
                    // 음악 시작으로 돌아가며 seekBar 초기화
                    binding.seekBar.progress = 0
                    mediaPlayer!!.seekTo(0)
                    // 경과 시간 초기화
                    binding.tvDurationStart.text = "00:00"
                    Log.d("log", "progress = ${binding.seekBar.progress.toInt()}")
                    Log.d("log", "mediaPlayer!!.currentPosition = ${mediaPlayer!!.currentPosition}")
                }else {
                    // 이전 곡으로 넘기기
                    mediaPlayer!!.stop()
                    playPrevious()
                }
            }

            // 다음 곡으로 넘기기
            R.id.ivNext -> {
                mediaPlayer!!.seekTo(binding.seekBar.max)
                Log.d("log", "seekBar max = ${binding.seekBar.max}")
                Log.d("log", "mediaPlayer!!.currentPosition = ${mediaPlayer!!.currentPosition}")
                playNext()
            }

            // 반복
            R.id.ivRepeat -> {
                if(mediaPlayer!!.isLooping){
                    // 반복 중일 때 클릭
                    mediaPlayer!!.isLooping = false
                    binding.ivRepeat.setImageResource(R.drawable.repeatoff_24)
                }else {
                    // 반복 중이 아닐 때 클릭
                    mediaPlayer!!.isLooping = true
                    binding.ivRepeat.setImageResource(R.drawable.repeat_one_24)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFav() {
        // adapter?.notifyDataSetChanged()
    }

    // 음악이 시작됐을 때 1) seekBar 이동 2) 경과 시간 변동 3) 음악 진행 => 코루틴 사용
    private fun playing() {
        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
        playerJob = backgroundScope.launch {
            while (mediaPlayer!!.isPlaying) {
                // UI 데이터 설정 권한은 UI스레드에만 있기 때문에 runOnUiThread 호출하여 AppCompatActivity()에 요청
                runOnUiThread {
                    val currentPosition = mediaPlayer!!.currentPosition
                    binding.seekBar.progress = currentPosition
                    binding.tvDurationStart.text = SimpleDateFormat("mm:ss").format(currentPosition)
                }
                // 컴퓨터의 요청 횟수를 줄이기 위해 0.5초씩 딜레이
                try {
                    delay(500)
                }catch (e: Exception) {
                    Log.d("log", "delay error: $e")
                }
            }// end of while
            // 음악이 끝날 때
            runOnUiThread {
                if (mediaPlayer!!.currentPosition >= binding.seekBar.max - 1000) {
                    // seekBar의 최대값과 currentPosition 시간차 맞춤

                    // seekbar 초기화
                    binding.seekBar.progress = 0
                    // 음악이 끝나면 경과 시간 초기화
                    binding.tvDurationStart.text = "00:00"
                    playNext()
                }
            }
        }// end of backgroundScope.launch
    }

    // 이전 곡 재생
    private fun playPrevious() {
        if(pos != 0) {
            // 현재 곡의 위치가 처음이 아닐 경우
            pos--
            currentTrack = playList[pos]
        }else {
            // 현재 곡의 위치가 처음일 경우 마지막 곡으로 가서 재생
            pos = playList.size - 1
            currentTrack = playList[pos]
        }

        // 음악 준비 및 재생
        prepare(currentTrack)
        mediaPlayer!!.start()
        binding.ivPlay.setImageResource(R.drawable.pause_24)
        playing()
    }

    // 다음 곡 재생
    private fun playNext() {
        if(pos != playList.size - 1) {
            // 현재 곡의 위치가 마지막이 아닐 경우
            pos++
            currentTrack = playList[pos]
        }else {
            // 현재 곡의 위치가 마지막일 경우 첫 곡 재생
            pos = 0
            currentTrack = playList[pos]
        }

        // 음악 준비 및 재생
        prepare(currentTrack)
        mediaPlayer!!.start()
        binding.ivPlay.setImageResource(R.drawable.pause_24)
        playing()
        binding.ivRepeat.setImageResource(R.drawable.repeatoff_24)
    }

    // 뒤로가기 버튼을 누르면 음악 종료
    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer?.stop()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
        binding.ivPlay.setImageResource(R.drawable.play_24)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    // Palette API
    fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    @SuppressLint("ResourceAsColor")
    fun setBackground(bitmap: Bitmap) {
        val dominantSwatch = createPaletteSync(bitmap).dominantSwatch
        val vibrantSwatch = createPaletteSync(bitmap).vibrantSwatch

        with(binding) {
            playbackground.setBackgroundColor(dominantSwatch?.rgb ?:
            ContextCompat.getColor(this@PlayActivity, R.color.white))

            // 화면 밝기
            if(dominantSwatch != null) {
                val light = dominantSwatch.hsl[2]
                if(light <= 0.5) {
                    // 화면이 어두울 때
                    tvTitle.setTextColor(Color.WHITE)
                    tvArtist.setTextColor(Color.LTGRAY)
                    tvDurationStart.setTextColor(Color.LTGRAY)
                    tvDurationEnd.setTextColor(Color.LTGRAY)
                    ivPlay.setColorFilter(resources.getColor(R.color.white))
                    ivPrevious.setColorFilter(resources.getColor(R.color.white))
                    ivNext.setColorFilter(resources.getColor(R.color.white))
                    ivFavorites.setColorFilter(resources.getColor(R.color.white))
                    ivRepeat.setColorFilter(resources.getColor(R.color.white))
                    ivList.setColorFilter(resources.getColor(R.color.white))
                }else {
                    // 화면이 밝을 때
                    tvTitle.setTextColor(Color.BLACK)
                    tvArtist.setTextColor(Color.DKGRAY)
                    tvDurationStart.setTextColor(Color.DKGRAY)
                    tvDurationEnd.setTextColor(Color.DKGRAY)
                    ivPlay.setColorFilter(resources.getColor(R.color.black))
                    ivPrevious.setColorFilter(resources.getColor(R.color.black))
                    ivNext.setColorFilter(resources.getColor(R.color.black))
                    ivFavorites.setColorFilter(resources.getColor(R.color.black))
                    ivRepeat.setColorFilter(resources.getColor(R.color.black))
                    ivList.setColorFilter(resources.getColor(R.color.black))
                }
            }
        }
    }

}