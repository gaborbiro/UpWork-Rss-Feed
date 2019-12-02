package app.gaborbiro.pollrss.favorites

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import app.gaborbiro.pollrss.AppPreferences
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.jobs.JobUIModel
import app.gaborbiro.pollrss.jobs.JobsUIMapper
import app.gaborbiro.pollrss.utils.openLink
import app.gaborbiro.pollrss.utils.share
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_favorites.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.yesButton

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.favorites, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_all -> {
                alert {
                    message = "Are you sure you want to delete all of your favorite jobs?"
                    yesButton {
                        AppPreferences.favorites.clear()
                        loadFavorites()
                    }
                    cancelButton { }
                }.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
                jobs.map(JobsUIMapper::map).toMutableList(),
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

        override fun onBodyClicked(job: JobUIModel) {
            openLink(job.link)
        }

        override fun onShare(job: JobUIModel) {
            share(job.link)
        }

        override fun onDelete(job: JobUIModel) {
            val position = adapter.removeItem(job)
            pendingRemoveId = job.id
            makeTopSnackBar("Removing from favorites...", Snackbar.LENGTH_SHORT)
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
            if (adapter.itemCount == 0) {
                recycle_view.visibility = View.GONE
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun makeTopSnackBar(message: String, duration: Int): Snackbar {
        return Snackbar.make(snackbar_host, message, duration)
            .apply {
                (view.layoutParams as CoordinatorLayout.LayoutParams).apply {
                    gravity = Gravity.TOP
                }.also {
                    view.layoutParams = it
                    view.rotation = 180f
                }
            }
    }
}