package com.example.nebula.activity


import com.example.nebula.R

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.nebula.activity.MainActivity.Companion.myPager
import com.example.nebula.activity.MainActivity.Companion.tabsBtn
import com.example.nebula.adapter.TabAdapter
import com.example.nebula.databinding.ActivityMainBinding
import com.example.nebula.databinding.BookmarkDialogBinding
import com.example.nebula.databinding.MoreFeaturesBinding
import com.example.nebula.databinding.TabsViewBinding
import com.example.nebula.fragment.BrowseFragment
import com.example.nebula.fragment.HomeFragment
import com.example.nebula.model.Bookmark
import com.example.nebula.model.Tab
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var printJob: PrintJob? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val WALLPAPER_PREF = "wallpaper_preference"
    private val WALLPAPER_KEY = "wallpaper_path"

    companion object{
        private const val PERMISSION_REQUEST_CODE = 100
        var tabsList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = false
        var bookmarkList: MutableList<Bookmark> = mutableListOf()
        var bookmarkIndex: Int = -1
        lateinit var myPager: ViewPager2
        lateinit var tabsBtn: MaterialTextView

        val defaultBookmarks = listOf(
            Bookmark("Google", "https://www.google.com", imageResource = R.drawable.google),
            Bookmark("Amazon", "https://www.amazon.com", imageResource = R.drawable.amazon),
            Bookmark("Flipkart", "https://www.flipkart.com", imageResource = R.drawable.flipkart),
            Bookmark("Facebook", "https://www.facebook.com", imageResource = R.drawable.facebook),
            Bookmark("Instagram", "https://www.instagram.com", imageResource = R.drawable.instagram),
            Bookmark("Netflix", "https://www.netflix.com", imageResource = R.drawable.netflix),

        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        // Set status bar icons to dark
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences and load wallpaper after setting content view
        sharedPreferences = getSharedPreferences(WALLPAPER_PREF, Context.MODE_PRIVATE)


        getAllBookmarks()

        tabsList.add(Tab("Home", HomeFragment()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager = binding.myPager
        tabsBtn = binding.tabsBtn

        initializeView()
        // Load wallpaper after view is set up
        binding.root.post {
            loadSavedWallpaper()
        }
        changeFullscreen(enable = true)
    }
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                setWallpaper(uri)
                saveWallpaperPath(uri.toString())
            }
        }
    }

    private fun openPhotoPicker() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
                intent.type = "image/*"
                getContent.launch(intent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                getContent.launch(intent)
            }
            else -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                getContent.launch(intent)
            }
        }
    }
    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val fileName = "wallpaper.jpg"
            openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            fileName
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private fun setWallpaper(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }

                val fileName = saveBitmapToInternalStorage(bitmap)
                if (fileName != null) {
                    withContext(Dispatchers.Main) {
                        // Update MainActivity background
                        window.decorView.background = BitmapDrawable(resources, bitmap)

                        saveWallpaperPath(fileName)

                        // Update all fragments
                        updateAllFragments()
                    }
                } else {
                    throw IOException("Failed to save bitmap")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to set wallpaper", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to set wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateAllFragments() {
        for (fragment in supportFragmentManager.fragments) {
            when (fragment) {
                is HomeFragment -> fragment.updateWallpaper()
                // Add other fragments here if they also need to update their wallpaper
            }
        }
    }

    // Add this method to allow fragments to access the current wallpaper
    fun getCurrentWallpaper(): Drawable? {
        return window.decorView.background
    }



    private fun saveWallpaperPath(path: String) {
        sharedPreferences.edit().putString(WALLPAPER_KEY, path).apply()
    }

    private fun loadSavedWallpaper() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val savedPath = sharedPreferences.getString(WALLPAPER_KEY, null)
                if (savedPath != null) {
                    val file = File(filesDir, savedPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        withContext(Dispatchers.Main) {
                            binding.root.background = BitmapDrawable(resources, bitmap)
                        }
                    } else {
                        Log.w("MainActivity", "Saved wallpaper file not found")
                    }
                } else {
                    Log.w("MainActivity", "No saved wallpaper path found")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to load wallpaper", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to load wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        }catch (e:Exception){}

        when{
            frag?.binding?.webView?.canGoBack() == true -> frag.binding.webView.goBack()
            binding.myPager.currentItem != 0 ->{
                tabsList.removeAt(binding.myPager.currentItem)
                binding.myPager.adapter?.notifyDataSetChanged()
                binding.myPager.currentItem = tabsList.size - 1

            }
            else -> super.onBackPressed()
        }
    }


    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) : FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }



    private fun initializeView(){

        binding.tabsBtn.setOnClickListener {
            val viewTabs = layoutInflater.inflate(R.layout.tabs_view, binding.root, false)
            val bindingTabs = TabsViewBinding.bind(viewTabs)

            val dialogTabs = MaterialAlertDialogBuilder(this, R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self, _ ->
                    changeTab("Home", HomeFragment())
                    self.dismiss()
                }
                .setNeutralButton("Google"){self, _ ->
                    changeTab("Google", BrowseFragment(urlNew = "www.google.com"))
                    self.dismiss()
                }
                .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager = LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs)

            dialogTabs.show()

            val pBtn = dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn = dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps = false
            nBtn.isAllCaps = false

            pBtn.setTextColor(Color.BLACK)
            nBtn.setTextColor(Color.BLACK)

            pBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.ic_home, theme)
                , null, null, null)
            nBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme)
                , null, null, null)
        }

        binding.settingBtn.setOnClickListener {

            var frag: BrowseFragment? = null
            try {
                frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
            }catch (e:Exception){}

            val view = layoutInflater.inflate(R.layout.more_features, binding.root, false)
            val dialogBinding = MoreFeaturesBinding.bind(view)

            val dialog = MaterialAlertDialogBuilder(this).setView(view).create()

            dialog.window?.apply {
                attributes.gravity = Gravity.BOTTOM
                attributes.y = 50
                setBackgroundDrawable(ColorDrawable(0xFFFFFFFF.toInt()))
            }
            dialog.show()

            if(isFullscreen){
                dialogBinding.fullscreenBtn.apply {
                    setIconTintResource(R.color.cool_blue)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                }
            }

            frag?.let {
                bookmarkIndex = isBookmarked(it.binding.webView.url!!)
                if(bookmarkIndex != -1){

                    dialogBinding.bookmarkBtn.apply {
                        setIconTintResource(R.color.cool_blue)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                    }
                } }

            if(isDesktopSite){
                dialogBinding.desktopBtn.apply {
                    setIconTintResource(R.color.cool_blue)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                }
            }



            dialogBinding.backBtn.setOnClickListener {
                onBackPressed()
            }
            dialogBinding.wallpaperBtn.setOnClickListener {

                openPhotoPicker()
            }


            dialogBinding.saveBtn.setOnClickListener {
                dialog.dismiss()
                if(frag != null)
                    saveAsPdf(web = frag.binding.webView)
                else Snackbar.make(binding.root, "First Open A WebPage\uD83D\uDE03", 3000).show()
            }

            dialogBinding.fullscreenBtn.setOnClickListener {
                it as MaterialButton

                isFullscreen = if (isFullscreen) {
                    changeFullscreen(enable = false)
                    it.setIconTintResource(R.color.black)
                    it.setTextColor(ContextCompat.getColor(this, R.color.black))
                    false
                }
                else {
                    changeFullscreen(enable = true)
                    it.setIconTintResource(R.color.cool_blue)
                    it.setTextColor(ContextCompat.getColor(this, R.color.cool_blue))
                    true
                }
            }

            dialogBinding.desktopBtn.setOnClickListener {
                it as MaterialButton

                frag?.binding?.webView?.apply {
                    isDesktopSite = if (isDesktopSite) {
                        settings.userAgentString = null
                        it.setIconTintResource(R.color.black)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                        false
                    }
                    else {
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0"
                        settings.useWideViewPort = true
                        evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                                " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null)
                        it.setIconTintResource(R.color.cool_blue)
                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                        true
                    }
                    reload()
                    dialog.dismiss()
                }

            }

            dialogBinding.bookmarkBtn.setOnClickListener {
                frag?.let{
                    if(bookmarkIndex == -1){
                        val viewB = layoutInflater.inflate(R.layout.bookmark_dialog, binding.root, false)
                        val bBinding = BookmarkDialogBinding.bind(viewB)
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Add Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Add"){self, _ ->
                                try {
                                    val array = ByteArrayOutputStream()
                                    it.webIcon?.compress(Bitmap.CompressFormat.PNG, 200, array)
                                    bookmarkList.add(
                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(), url = it.binding.webView.url!!, image = array.toByteArray())
                                    )
                                } catch (e: Exception) {
                                    bookmarkList.add(
                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(), url = it.binding.webView.url!!)
                                    )
                                }
                                self.dismiss()}
                            .setNegativeButton("Cancel"){self, _ -> self.dismiss()}
                            .setView(viewB).create()
                        dialogB.show()
                        bBinding.bookmarkTitle.setText(it.binding.webView.title)
                    }else{
                        val dialogB = MaterialAlertDialogBuilder(this)
                            .setTitle("Remove Bookmark")
                            .setMessage("Url:${it.binding.webView.url}")
                            .setPositiveButton("Remove"){self, _ ->
                                bookmarkList.removeAt(bookmarkIndex)
                                self.dismiss()}
                            .setNegativeButton("Cancel"){self, _ -> self.dismiss()}
                            .create()
                        dialogB.show()
                    }
                }

                dialog.dismiss()
            }
        }

    }


    override fun onResume() {
        super.onResume()
        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(binding.root, "Successful -> ${it.info.label}", 4000).show()
                it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000).show()
            }
        }
    }

    private fun saveAsPdf(web: WebView){
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager

        val jobName = "${URL(web.url).host}_${SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH)
            .format(Calendar.getInstance().time)}"
        val printAdapter = web.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
        printJob = pm.print(jobName, printAdapter, printAttributes.build())
    }

    private fun changeFullscreen(enable: Boolean){
        if(enable){
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
               
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url: String): Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if(bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks(){
        //for storing bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply()
    }

    private fun getAllBookmarks(){
        //for getting bookmarks data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)

        if(data != null){
            val list: ArrayList<Bookmark> = GsonBuilder().create().fromJson(data, object: TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }
        // If no bookmarks are saved, add default bookmarks
        if (bookmarkList.isEmpty()) {
            bookmarkList.addAll(defaultBookmarks)
            saveBookmarks() // Save default bookmarks
        }
    }


}


@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment, isBackground: Boolean = false) {
    if (MainActivity.bookmarkList.none { it.url == url }) {
        MainActivity.tabsList.add(Tab(name = url, fragment = fragment))
        myPager.adapter?.notifyDataSetChanged()
        tabsBtn.text = MainActivity.tabsList.size.toString()

        if (!isBackground) myPager.currentItem = MainActivity.tabsList.size - 1
    } else {
        // Show a message that the bookmark already exists
        Toast.makeText(myPager.context, "Already bookmarked", Toast.LENGTH_SHORT).show()
    }
}

fun checkForInternet(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val networkInfo =
            connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }
}