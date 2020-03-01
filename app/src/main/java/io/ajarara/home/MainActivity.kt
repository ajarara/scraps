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
            is Screen.Home -> FragmentWithTag(
                HomeFragment.newInstance(), HomeFragment.tag
            )
            is Screen.Movies -> FragmentWithTag(
                MovieFragment.newInstance(),
                MovieFragment.tag
            )
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.canvas, toShow.fragment, toShow.tag)
            .commit()
    }
}

class FragmentWithTag(
    val fragment: Fragment,
    val tag: String
)
interface Navigator {
    fun navigate(screen: Screen)
}

sealed class Screen {
    object Home : Screen()
    object Movies : Screen()
}
