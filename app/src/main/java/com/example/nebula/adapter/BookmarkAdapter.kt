package com.example.nebula.adapter

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.example.nebula.R
import com.example.nebula.activity.MainActivity
import com.example.nebula.activity.changeTab
import com.example.nebula.activity.checkForInternet
import com.example.nebula.databinding.BookmarkViewBinding
import com.example.nebula.databinding.LongBookmarkViewBinding
import com.example.nebula.fragment.BrowseFragment
import com.example.nebula.model.Bookmark
import com.google.android.material.snackbar.Snackbar


class BookmarkAdapter(private val context: Context, private val isActivity: Boolean = false) : RecyclerView.Adapter<BookmarkAdapter.MyHolder>() {
    private val colors = context.resources.getIntArray(R.array.myColors)
    private val list: List<Bookmark> = if (isActivity) {
        MainActivity.bookmarkList + MainActivity.defaultBookmarks.filter { default ->
            MainActivity.bookmarkList.none { it.url == default.url }
        }
    } else {
        (MainActivity.bookmarkList + MainActivity.defaultBookmarks).distinctBy { it.url }.take(15)
    }

    class MyHolder(binding: BookmarkViewBinding? = null, bindingL: LongBookmarkViewBinding? = null)
        : RecyclerView.ViewHolder((binding?.root ?: bindingL?.root)!!) {
        val image = (binding?.bookmarkIcon ?: bindingL?.bookmarkIcon)!!
        val name = (binding?.bookmarkName ?: bindingL?.bookmarkName)!!
        val root = (binding?.root ?: bindingL?.root)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return if (isActivity) {
            MyHolder(bindingL = LongBookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
        } else {
            MyHolder(binding = BookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val bookmark = list[position]

        when {
            bookmark.image != null -> {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(bookmark.image, 0, bookmark.image.size)
                    if (bitmap != null) {
                        setImageOrBackground(holder.image, bitmap)
                        holder.image.text = null
                    } else {
                        setDefaultBackground(holder.image, bookmark.name)
                    }
                } catch (e: Exception) {
                    setDefaultBackground(holder.image, bookmark.name)
                }
            }
            bookmark.imageResource != null -> {
                try {
                    val drawable = ContextCompat.getDrawable(context, bookmark.imageResource)
                    if (drawable != null) {
                        setImageOrBackground(holder.image, drawable)
                        holder.image.text = null
                    } else {
                        setDefaultBackground(holder.image, bookmark.name)
                    }
                } catch (e: Resources.NotFoundException) {
                    setDefaultBackground(holder.image, bookmark.name)
                }
            }
            else -> {
                setDefaultBackground(holder.image, bookmark.name)
            }
        }

        holder.name.text = bookmark.name

        holder.name.text = bookmark.name

        holder.root.setOnClickListener {
            when {
                checkForInternet(context) -> {
                    changeTab(bookmark.name, BrowseFragment(urlNew = bookmark.url))
                    if (isActivity) (context as Activity).finish()
                }
                else -> Snackbar.make(holder.root, "Internet Not Connected\uD83D\uDE03", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setDefaultBackground(view: View, name: String) {
        val backgroundColor = colors[(colors.indices).random()]
        view.setBackgroundColor(backgroundColor)
        if (view is TextView) {
            view.text = name[0].toString()
        }
    }

    private fun setImageOrBackground(view: View, bitmap: Bitmap) {
        when (view) {
            is ImageView -> view.setImageBitmap(bitmap)
            else -> view.background = BitmapDrawable(context.resources, bitmap)
        }
    }

    private fun setImageOrBackground(view: View, drawable: Drawable) {
        when (view) {
            is ImageView -> view.setImageDrawable(drawable)
            else -> view.background = drawable
        }
    }

    override fun getItemCount(): Int = list.size
}