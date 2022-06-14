package com.example.mp3player

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mp3player.databinding.ActivityDetailBinding
import java.text.SimpleDateFormat

class DetailActivity : AppCompatActivity() {
    val binding by lazy { ActivityDetailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 앨범 아트 사이즈
        val ALBUMART_WIDTH = 300
        val ALBUMART_HEIGHT = 300

        // 인텐트를 통해 MusicRecyclerAdapter에서 음악 받아오기
        var currentTrack = intent.getSerializableExtra("currentTrack") as Music
        binding.detailTitle.text = currentTrack.title
        binding.detailAlbum.text = currentTrack.albumName
        binding.detailTrack.text = currentTrack.track
        binding.detailArtist.text = currentTrack.artist
        binding.detailDuration.text = SimpleDateFormat("mm:ss").format(currentTrack.duration)

        // 뷰 설정 - 앨범아트
        val bitmap: Bitmap? = currentTrack.getAlbumArt(this, ALBUMART_WIDTH, ALBUMART_HEIGHT)
        if(bitmap != null) binding.imageView.setImageBitmap(bitmap)
        else binding.imageView.setImageResource(R.drawable.albumart)

    }
}