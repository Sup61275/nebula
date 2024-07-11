package com.example.nebula.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nebula.R
import com.example.nebula.activity.BookmarkActivity
import com.example.nebula.activity.MainActivity
import com.example.nebula.activity.VoiceSearchActivity
import com.example.nebula.activity.changeTab
import com.example.nebula.activity.checkForInternet
import com.example.nebula.adapter.BookmarkAdapter
import com.example.nebula.databinding.FragmentHomeBinding
import com.example.nebula.utils.SpaceItemDecoration
import com.google.android.material.snackbar.Snackbar
import java.io.File


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val WALLPAPER_PREF = "wallpaper_preference"
    private val WALLPAPER_KEY = "wallpaper_path"

    private lateinit var searchView: SearchView
    private lateinit var micButton: ImageView



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val customSearchView = view.findViewById<View>(R.id.custom_search_view)
        searchView = customSearchView.findViewById(R.id.search_view)
        micButton = customSearchView.findViewById(R.id.search_mic)
        micButton.setOnClickListener {
            val intent = Intent(requireContext(), VoiceSearchActivity::class.java)
            startActivity(intent)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission
                query?.let {
                    if (checkForInternet(requireContext())) {
                        changeTab(it, BrowseFragment(it))
                    } else {
                        Snackbar.make(binding.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text changes if needed
                return false
            }
        })
        sharedPreferences = requireActivity().getSharedPreferences(WALLPAPER_PREF, Context.MODE_PRIVATE)
        loadWallpaper()
        binding.viewAllBtn.setOnClickListener {
            val intent = Intent(requireContext(), BookmarkActivity::class.java)
            startActivity(intent)
        }

        updateBookmarksList()

    }
    private fun updateBookmarksList() {
        val combinedList = (MainActivity.bookmarkList + MainActivity.defaultBookmarks).distinctBy { it.url }
        val displayList = combinedList.take(10) // Show only 2 rows (5 items per row)

        val layoutManager = object : GridLayoutManager(requireContext(), 5) {
            override fun canScrollVertically(): Boolean {
                return false // Disable vertical scrolling
            }
        }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = BookmarkAdapter(requireContext(), false)

        binding.viewAllBtn.visibility = if (combinedList.size > 10) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()

        val mainActivityRef = requireActivity() as MainActivity

        MainActivity.tabsBtn.text = MainActivity.tabsList.size.toString()
        MainActivity.tabsList[MainActivity.myPager.currentItem].name = "Home"
        mainActivityRef.binding.topSearchBar.setText("")
        searchView.setQuery("", false)  // Use the class-level searchView variable
        mainActivityRef.binding.webIcon.setImageResource(R.drawable.ic_search)
        val layoutManager = object : GridLayoutManager(requireContext(), 5) {
            override fun canScrollVertically(): Boolean {
                return false // Disable vertical scrolling
            }
        }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = BookmarkAdapter(requireContext())

        // Show "View All" button if there are more than 15 bookmarks
        binding.viewAllBtn.visibility = if ((MainActivity.bookmarkList + MainActivity.defaultBookmarks).distinctBy { it.url }.size > 15)
            View.VISIBLE else View.GONE

        mainActivityRef.binding.refreshBtn.visibility = View.GONE
        binding.recyclerView.adapter = BookmarkAdapter(requireContext())
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(result: String?): Boolean {
                if (checkForInternet(requireContext()))
                    changeTab(result!!, BrowseFragment(result))
                else
                    Snackbar.make(binding.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean = false
        })
        mainActivityRef.binding.goBtn.setOnClickListener {
            if (checkForInternet(requireContext()))
                changeTab(mainActivityRef.binding.topSearchBar.text.toString(),
                    BrowseFragment(mainActivityRef.binding.topSearchBar.text.toString())
                )
            else
                Snackbar.make(binding.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
        }
        val spaceHeight = 16 // Adjust this value as needed
        binding.recyclerView.addItemDecoration(SpaceItemDecoration(spaceHeight))

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(5)
        binding.recyclerView.isNestedScrollingEnabled = false // Ensure nested scrolling is disabled

        if (MainActivity.bookmarkList.size < 1 && MainActivity.defaultBookmarks.isNotEmpty()) {
            binding.viewAllBtn.visibility = View.VISIBLE
        } else {
            binding.viewAllBtn.visibility = if (MainActivity.bookmarkList.size > 5) View.VISIBLE else View.GONE
        }
    }



    fun loadWallpaper() {
        val savedPath = sharedPreferences.getString(WALLPAPER_KEY, null)
        if (savedPath != null) {
            val file = File(requireContext().filesDir, savedPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.root.background = BitmapDrawable(resources, bitmap)
            } else {
                // If file doesn't exist, set default wallpaper
                binding.root.setBackgroundResource(R.drawable.defaultwallpaper)
            }
        } else {
            // If no saved path, set default wallpaper
            binding.root.setBackgroundResource(R.drawable.defaultwallpaper)
        }
    }

    fun updateWallpaper() {
        loadWallpaper()
    }
}