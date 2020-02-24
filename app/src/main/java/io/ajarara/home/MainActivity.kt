package io.ajarara.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.ajarara.R
import io.ajarara.omdb.MovieFragment

class MainActivity : AppCompatActivity(), Navigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        navigate(Screen.Home)
    }

    override fun navigate(screen: Screen) {
        val toShow = when(screen) {
            is Screen.Home -> HomeFragment.newInstance()
            is Screen.Movies -> MovieFragment.newInstance()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.canvas, toShow)
            .commit()
    }
}

interface Navigator {
    fun navigate(screen: Screen)
}

sealed class Screen {
    object Home : Screen()
    object Movies : Screen()
}
