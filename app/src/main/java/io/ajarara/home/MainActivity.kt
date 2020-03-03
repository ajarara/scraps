package io.ajarara.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.ajarara.R
import io.ajarara.omdb.MovieFragment

class MainActivity : AppCompatActivity(), Navigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            navigate(Screen.Home)
        }
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

    override fun onBackPressed() {
        navigate(Screen.Home)
    }
}

interface Navigator {
    fun navigate(screen: Screen)
}

sealed class Screen {
    object Home : Screen()
    object Movies : Screen()
}
