package com.example.mp3player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mp3player.databinding.ActivityDetailBinding
import java.text.SimpleDateFormat

class DetailActivity : AppCompatActivity() {
    val binding by lazy { ActivityDetailBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 인텐트를 통해 MusicRecyclerAdapter에서 음악 받아오기
        var currentTrack = intent.getSerializableExtra("currentTrack") as Music
        binding.detailTitle.text = currentTrack.title
        binding.detailAtrist.text = currentTrack.artist
        binding.detailDuration.text = SimpleDateFormat("mm:ss").format(currentTrack.duration)


    }
}