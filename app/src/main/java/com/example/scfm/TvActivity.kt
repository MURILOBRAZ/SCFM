package com.example.scfm

import SCFM.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.google.firebase.firestore.FirebaseFirestore

class TvActivity : Activity() {

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var db: FirebaseFirestore
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0

    private val handler = Handler()
    private val checkConnectivityRunnable = object : Runnable {
        override fun run() {
            if (!isConnected()) {
                setContentView(R.layout.activity_disconnect)
                player?.release()
                player = null
            } else {
                handler.postDelayed(this, 5000)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.player_view)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Configura o listener de visibilidade do controlador
        playerView.setControllerVisibilityListener(object : PlayerControlView.VisibilityListener {
            override fun onVisibilityChange(visibility: Int) {
                // Aqui você pode adicionar lógica se necessário, por exemplo:
                if (visibility == View.VISIBLE) {
                    // O controlador está visível
                } else {
                    // O controlador não está visível
                }
            }
        })

        db = FirebaseFirestore.getInstance()
    }

    private fun isConnected(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nInfo: NetworkInfo? = cm.activeNetworkInfo
            nInfo != null && nInfo.isAvailable && nInfo.isConnected
        } catch (e: Exception) {
            Log.e("Connectivity Exception", e.message ?: "Unknown error")
            false
        }
    }

    private fun iniciarPlayer() {
        if (!isConnected()) {
            setContentView(R.layout.activity_disconnect)
            return
        }

        loadingProgressBar.visibility = View.VISIBLE

        db.collection("configs").document("streaming_radio")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val streamUrl = documentSnapshot.getString("streaming_url")
                    if (!streamUrl.isNullOrEmpty()) {
                        configurarPlayer(streamUrl)
                    } else {
                        setContentView(R.layout.activity_disconnect)
                    }
                } else {
                    setContentView(R.layout.activity_disconnect)
                }
            }
            .addOnFailureListener {
                setContentView(R.layout.activity_disconnect)
            }
    }

    private fun configurarPlayer(streamUrl: String) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also {
                playerView.player = it

                val mediaItem = MediaItem.fromUri(streamUrl)
                it.setMediaItem(mediaItem)

                it.playWhenReady = playWhenReady
                it.seekTo(currentWindow, playbackPosition)
                it.prepare()

                it.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            loadingProgressBar.visibility = View.GONE
                            playerView.visibility = View.VISIBLE
                            handler.post(checkConnectivityRunnable)
                        }
                    }
                })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        iniciarPlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()

        player?.release()
        iniciarPlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
        handler.removeCallbacks(checkConnectivityRunnable)
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        handler.removeCallbacks(checkConnectivityRunnable)
    }

    fun sair(view: View) {
        // Ação ao clicar no botão, como finalizar a atividade ou voltar para a tela anterior
        finish() // Exemplo de finalizar a Activity
    }


    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    @OptIn(UnstableApi::class)
    private fun releasePlayer() {
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            currentWindow = it.currentWindowIndex
            it.release()
            player = null
        }
    }
}