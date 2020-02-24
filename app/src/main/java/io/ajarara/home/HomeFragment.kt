package io.ajarara.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.ajarara.R

class HomeFragment : Fragment() {

    var navigator: Navigator? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        require(context is Navigator)
        navigator = context
    }

    override fun onDetach() {
        super.onDetach()
        navigator = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.movies).setOnClickListener {
            navigator?.navigate(Screen.Movies)
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}