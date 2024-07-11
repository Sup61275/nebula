package com.example.nebula.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.nebula.R


class VoiceSearchActivity : AppCompatActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var listeningText: TextView
    private lateinit var topAnimation: LottieAnimationView
    private lateinit var voiceSearchAnimation: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_search)

        listeningText = findViewById(R.id.listeningText)
        topAnimation = findViewById(R.id.topAnimation)
        voiceSearchAnimation = findViewById(R.id.voiceSearchAnimation)

        topAnimation.setFailureListener { exception ->
            Log.e("LottieError", "Error loading top animation", exception)
        }

        voiceSearchAnimation.setFailureListener { exception ->
            Log.e("LottieError", "Error loading voice search animation", exception)
        }

        initializeSpeechRecognizer()
        startListening()
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listeningText.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                listeningText.text = ""
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listeningText.text = "Processing..."
            }

            override fun onError(error: Int) {
                listeningText.text = "Error occurred. Please try again."
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1500)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    val intent = Intent().apply {
                        putExtra("RECOGNIZED_TEXT", recognizedText)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    listeningText.text = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}