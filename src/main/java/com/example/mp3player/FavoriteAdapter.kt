package com.example.mp3player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mp3player.databinding.ItemBinding
import java.text.SimpleDateFormat

// 뷰 홀더 클래스
class FuViewHolder(val binding: ItemBinding): RecyclerView.ViewHolder(binding.root)

class FavoriteAdapter(val context: Context, val favoriteList: MutableList<Music>): RecyclerView.Adapter<FuViewHolder>() {
    // 앨범아트 사이즈
    val ALBUMART_WIDTH = 50
    val ALBUMART_HEIGHT = 50
    val dbHelper by lazy { DBHelper(context, DBHelper.VERSION) }

    // 뷰 홀더 객체 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuViewHolder {
        val bindItem = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val viewHolder = FuViewHolder(bindItem)

        // 항목 터치 이벤트 - 색상 강조
        viewHolder.itemView.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                if(event?.action == MotionEvent.ACTION_DOWN) viewHolder.itemView.setBackgroundColor(
                    Color.LTGRAY)
                else viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
                return false
            }
        })

        return viewHolder
    }

    // 화면에 표시된 뷰에 데이터를 넣기
    override fun onBindViewHolder(holder: FuViewHolder, position: Int) {
        val binding = holder.binding
        val music = favoriteList[position]
        binding.liTitle.text = music.title
        binding.liArtist.text = music.artist
        // 음악 길이
        binding.liDuration.text = SimpleDateFormat("mm:ss").format(music.duration)
        // 앨범 아트
        val bitmap: Bitmap? = music.getAlbumArt(context, ALBUMART_WIDTH, ALBUMART_HEIGHT)
        if (bitmap != null) {
            binding.liAlbumArt.setImageBitmap(bitmap)
        }else {
            // 앨범 아트가 없을 경우 기본 이미지
            binding.liAlbumArt.setImageResource(R.drawable.albumicon)
        }

        // 항목 선택 시 이벤트
        binding.root.setOnClickListener {
            // 인텐트를 통해 음악 정보를 음악 재생 액티비티로 전달
            val intent = Intent(this.context, PlayActivity::class.java)
            intent.putExtra("track", music)    // 음악 하나
            intent.putExtra("pos", position)
            intent.putExtra("from", "favorite")
            this.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = favoriteList.size

}