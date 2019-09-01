package app.gaborbiro.pollrss.favorite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.utils.openLink
import app.gaborbiro.pollrss.utils.share
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_favorites.*

class FavoritesActivity : AppCompatActivity() {

    private lateinit var adapter: FavoriteJobAdapter
    private var pendingRemoveId: Long? = null

    companion object {
        fun start(parent: Activity) {
            Intent(parent, FavoritesActivity::class.java).also {
                parent.startActivity(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = "Favorites"
        }
        toolbar.setNavigationOnClickListener { finish() }
        loadFavorites()
    }

    override fun onPause() {
        super.onPause()
        pendingRemoveId?.let {
            AppPreferences.favorites.remove(it)
            pendingRemoveId = null
        }
    }

    private fun loadFavorites() {
        val jobs = AppPreferences.favorites.mapNotNull { AppPreferences.jobs[it] }
        if (jobs.isNotEmpty()) {
            adapter = FavoriteJobAdapter(
                jobs.toMutableList(),
                jobAdapterCallback
            )
            recycle_view.adapter = adapter
            recycle_view.visibility = View.VISIBLE
            empty.visibility = View.GONE
        } else {
            recycle_view.visibility = View.GONE
            empty.visibility = View.VISIBLE
        }
    }

    private val jobAdapterCallback = object : FavoriteJobAdapter.FavoritesJobAdapterCallback {

        override fun onTitleClicked(job: Job) {
            openLink(job.link)
        }

        override fun onShare(job: Job) {
            share(job.link)
        }

        override fun onDelete(job: Job) {
            val position = adapter.removeItem(job)
            pendingRemoveId = job.id
            Snackbar.make(recycle_view, "Removing from favorites...", Snackbar.LENGTH_LONG)
                .setAction("Undo") {
                    adapter.addItem(position, job)
                }
                .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_CONSECUTIVE) {
                            AppPreferences.favorites.remove(job.id)
                            pendingRemoveId = null
                        }
                    }
                })
                .show()
        }
    }
}