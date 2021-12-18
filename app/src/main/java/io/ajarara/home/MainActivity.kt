package io.ajarara.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.ajarara.R
import io.ajarara.omdb.MovieFragment

class MainActivity : FragmentActivity(), Navigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            navigate(Screen.Home)
        }
    }

    override fun navigate(screen: Screen) {
        when(val oldFragment = supportFragmentManager.findFragmentByTag(screen.tag)) {
            null -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.canvas, screen.newInstance(), screen.tag)
                    .addToBackStack(screen.tag)
                    .commit()
            }
            else -> {
                println("Reusing fragment: ${screen.tag}")
                supportFragmentManager.beginTransaction()
                    .replace(R.id.canvas, oldFragment, oldFragment.tag)
                    .commit()
            }
        }
    }
}

interface Navigator {
    fun navigate(screen: Screen)
}

enum class Screen {
    Home {
        override fun newInstance(): Fragment = HomeFragment.newInstance()
        override val tag: String =  HomeFragment.tag
    }, Movies {
        override fun newInstance(): Fragment = MovieFragment.newInstance()
        override val tag: String = MovieFragment.tag
    };

    abstract fun newInstance(): Fragment
    abstract val tag: String
}
