package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.utils.AlbumCoverManager
import com.dhananjaysaini.musicplayerapp.utils.ArtistAvatarHelper

// 1. Top Picks
data class ForYouTopPick(
    val tag: String,
    val title: String,
    val artUri: String?,
    val iconRes: Int? = null,
    val bgRes: Int? = null,
    val songs: List<Music>,
    val onCardClick: () -> Unit
)

class ForYouTopPicksAdapter(
    private var items: List<ForYouTopPick>,
    private val onPlayClick: (List<Music>) -> Unit
) : RecyclerView.Adapter<ForYouTopPicksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val btnPlay: FrameLayout = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_top_pick, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTag.text = item.tag
        holder.tvTitle.text = item.title

        if (item.iconRes != null) {
            holder.ivArt.setImageDrawable(null)
            if (item.bgRes != null) {
                holder.ivArt.setBackgroundResource(item.bgRes)
            } else {
                holder.ivArt.setBackgroundResource(R.drawable.bg_preset_top)
            }
            holder.ivIcon.setImageResource(item.iconRes)
            holder.ivIcon.visibility = View.VISIBLE
        } else if (!item.artUri.isNullOrEmpty()) {
            holder.ivIcon.visibility = View.GONE
            holder.ivArt.background = null
            Glide.with(holder.itemView.context)
                .load(item.artUri)
                .apply(RequestOptions().placeholder(item.bgRes ?: R.drawable.bg_preset_top).centerCrop())
                .into(holder.ivArt)
        } else {
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(item.bgRes ?: R.drawable.bg_preset_top)
            holder.ivIcon.setImageResource(R.drawable.ic_neon_fire)
            holder.ivIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }

        holder.btnPlay.setOnClickListener {
            onPlayClick(item.songs)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouTopPick>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 2. Recently Played
data class ForYouRecentItem(
    val title: String,
    val subtitle: String,
    val artUri: String?,
    val iconRes: Int? = null,
    val bgRes: Int? = null,
    val songs: List<Music>,
    val onCardClick: () -> Unit
)

class ForYouRecentAdapter(
    private var items: List<ForYouRecentItem>,
    private val onPlayClick: (List<Music>) -> Unit
) : RecyclerView.Adapter<ForYouRecentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnPlay: FrameLayout = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_recent, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle

        if (item.iconRes != null && item.artUri.isNullOrEmpty()) {
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(item.bgRes ?: R.drawable.bg_playlist_card)
            holder.ivIcon.setImageResource(item.iconRes)
            holder.ivIcon.visibility = View.VISIBLE
        } else if (!item.artUri.isNullOrEmpty()) {
            holder.ivIcon.setImageResource(item.iconRes ?: R.drawable.ic_neon_music_note)
            holder.ivIcon.visibility = View.VISIBLE
            holder.ivArt.setBackgroundResource(item.bgRes ?: R.drawable.bg_playlist_card)
            Glide.with(holder.itemView.context)
                .load(item.artUri)
                .apply(RequestOptions().centerCrop())
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.ivArt.setImageDrawable(null)
                        holder.ivArt.setBackgroundResource(item.bgRes ?: R.drawable.bg_playlist_card)
                        holder.ivIcon.setImageResource(item.iconRes ?: R.drawable.ic_neon_music_note)
                        holder.ivIcon.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.ivIcon.visibility = View.GONE
                        return false
                    }
                })
                .into(holder.ivArt)
        } else {
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(item.bgRes ?: R.drawable.bg_playlist_card)
            holder.ivIcon.setImageResource(item.iconRes ?: R.drawable.ic_neon_music_note)
            holder.ivIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }

        holder.btnPlay.setOnClickListener {
            onPlayClick(item.songs)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouRecentItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 3. Explore Mixes
data class ForYouExploreMix(
    val title: String,
    val subtitle: String,
    val bgDrawableRes: Int,
    val songs: List<Music>,
    val onCardClick: () -> Unit
)

class ForYouExploreAdapter(
    private var items: List<ForYouExploreMix>,
    private val onPlayClick: (List<Music>) -> Unit
) : RecyclerView.Adapter<ForYouExploreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bgContainer: FrameLayout = view.findViewById(R.id.bgContainer)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnPlay: FrameLayout = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_explore, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.bgContainer.setBackgroundResource(item.bgDrawableRes)

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }

        holder.btnPlay.setOnClickListener {
            onPlayClick(item.songs)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouExploreMix>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 4. Playlists You Love
data class ForYouPlaylistItem(
    val title: String,
    val subtitle: String,
    val artUri: String?,
    val isFav: Boolean = false,
    val songs: List<Music>,
    val onCardClick: () -> Unit,
    val onMenuClick: ((View) -> Unit)? = null
)

class ForYouPlaylistsAdapter(
    private var items: List<ForYouPlaylistItem>,
    private val onPlayClick: (ForYouPlaylistItem) -> Unit
) : RecyclerView.Adapter<ForYouPlaylistsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnPlay: FrameLayout = view.findViewById(R.id.btnPlay)
        val btnMenu: ImageView = view.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle

        if (item.isFav) {
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(R.drawable.bg_preset_fav)
            holder.ivIcon.setImageResource(R.drawable.ic_neon_favourite)
            holder.ivIcon.visibility = View.VISIBLE
        } else if (!item.artUri.isNullOrEmpty()) {
            holder.ivIcon.visibility = View.GONE
            holder.ivArt.background = null
            Glide.with(holder.itemView.context)
                .load(item.artUri)
                .apply(RequestOptions().placeholder(R.drawable.bg_playlist_card).centerCrop())
                .into(holder.ivArt)
        } else {
            // Custom playlist: show custom playlist folder icon ic_neon_playlist_card
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(R.drawable.bg_playlist_card)
            holder.ivIcon.setImageResource(R.drawable.ic_neon_playlist_card)
            holder.ivIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }

        holder.btnPlay.setOnClickListener {
            onPlayClick(item)
        }

        holder.btnMenu.setOnClickListener { v ->
            item.onMenuClick?.invoke(v)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouPlaylistItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 5. Albums You Love
data class ForYouAlbumItem(
    val name: String,
    val artist: String,
    val artUri: String?,
    val songs: List<Music>,
    val onCardClick: () -> Unit
)

class ForYouAlbumsAdapter(
    private var items: List<ForYouAlbumItem>,
    private val onPlayClick: (List<Music>) -> Unit
) : RecyclerView.Adapter<ForYouAlbumsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnPlay: FrameLayout = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_album, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.name
        holder.tvSubtitle.text = item.artist

        val customCover = AlbumCoverManager.getCustomCover(holder.itemView.context, item.name)
        val coverToLoad = customCover ?: item.artUri

        holder.ivArt.setBackgroundResource(R.drawable.bg_playlist_card)
        if (!coverToLoad.isNullOrEmpty()) {
            holder.ivIcon.setImageResource(R.drawable.ic_neon_album)
            holder.ivIcon.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(coverToLoad)
                .apply(RequestOptions().centerCrop())
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.ivArt.setImageDrawable(null)
                        holder.ivArt.setBackgroundResource(R.drawable.bg_playlist_card)
                        holder.ivIcon.setImageResource(R.drawable.ic_neon_album)
                        holder.ivIcon.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.ivIcon.visibility = View.GONE
                        return false
                    }
                })
                .into(holder.ivArt)
        } else {
            holder.ivArt.setImageDrawable(null)
            holder.ivArt.setBackgroundResource(R.drawable.bg_playlist_card)
            holder.ivIcon.setImageResource(R.drawable.ic_neon_album)
            holder.ivIcon.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }

        holder.btnPlay.setOnClickListener {
            onPlayClick(item.songs)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouAlbumItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 6. Artists You Love
data class ForYouArtistItem(
    val name: String,
    val artUri: String?,
    val songs: List<Music>,
    val onCardClick: () -> Unit
)

class ForYouArtistsAdapter(
    private var items: List<ForYouArtistItem>
) : RecyclerView.Adapter<ForYouArtistsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_for_you_artist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name

        val initial = ArtistAvatarHelper.getInitial(item.name)
        holder.tvInitial.text = initial ?: "?"
        val color = ArtistAvatarHelper.getColorForName(item.name)
        holder.tvInitial.setTextColor(color)

        val bgColor = androidx.core.graphics.ColorUtils.blendARGB(color, android.graphics.Color.BLACK, 0.72f)
        val ivAvatarBg = holder.itemView.findViewById<ImageView>(R.id.ivAvatarBg)
        ivAvatarBg?.imageTintList = android.content.res.ColorStateList.valueOf(bgColor)

        if (!item.artUri.isNullOrEmpty()) {
            holder.ivArt.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.artUri)
                .apply(RequestOptions().circleCrop())
                .into(holder.ivArt)
        } else {
            holder.ivArt.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            item.onCardClick()
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ForYouArtistItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// 7. Month Recap Songs Card Adapter
class RecapSongsCardAdapter(
    private var items: List<Music>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RecapSongsCardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recap_song_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = items[position]
        holder.tvRank.text = (position + 1).toString()
        holder.tvTitle.text = song.title
        holder.tvSubtitle.text = song.artist ?: song.album ?: "<unknown>"

        Glide.with(holder.itemView.context)
            .load(song.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_neon_music_note)
                    .error(R.drawable.ic_neon_music_note)
                    .centerCrop()
            )
            .into(holder.ivArt)

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<Music>) {
        items = newItems
        notifyDataSetChanged()
    }
}
