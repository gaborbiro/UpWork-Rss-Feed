package app.gaborbiro.pollrss

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.model.PATH_UNFAVORITE
import app.gaborbiro.pollrss.model.QUERY_PARAM_ID
import app.gaborbiro.pollrss.model.formatDescriptionForFavorite
import kotlinx.android.synthetic.main.activity_favorites.*
import kotlinx.android.synthetic.main.activity_favorites.content
import kotlinx.android.synthetic.main.content_main.*

class FavoritesActivity : AppCompatActivity() {

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
        content.movementMethod = LinkMovementMethod.getInstance()
        loadFavorites()
    }

    private fun loadFavorites() {
        val contentStr = AppPreferences.favorites.mapNotNull { AppPreferences.jobs[it] }
            .joinToString(separator = "<br><br>", transform = Job::formatDescriptionForFavorite)
        if (contentStr.isNotEmpty()) {
            content.text = Html.fromHtml(contentStr, 0)
        } else {
            content.text = "This is where your favorites will show up"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.extras.get("com.android.browser.application_id") != BuildConfig.APPLICATION_ID) {
            return
        }
        when (intent.data?.path) {
            "/$PATH_UNFAVORITE" -> {
                val id = intent.data.getQueryParameter(QUERY_PARAM_ID)!!.toLong()
                AppPreferences.favorites.remove(id)
                loadFavorites()
            }
        }
    }
}