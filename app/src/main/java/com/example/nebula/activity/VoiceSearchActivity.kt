package com.example.nebula.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.nebula.R


class VoiceSearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_search)

        val topAnimation = findViewById<LottieAnimationView>(R.id.topAnimation)
        val voiceSearchAnimation = findViewById<LottieAnimationView>(R.id.voiceSearchAnimation)

        topAnimation.setFailureListener { exception ->
            Log.e("LottieError", "Error loading top animation", exception)
        }

        voiceSearchAnimation.setFailureListener { exception ->
            Log.e("LottieError", "Error loading voice search animation", exception)
        }
    }
}