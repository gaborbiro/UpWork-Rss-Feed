package app.gaborbiro.pollrss.jobs

import android.view.View
import android.widget.ImageView
import app.gaborbiro.pollrss.BaseJobAdapter
import app.gaborbiro.pollrss.BaseJobAdapterCallback
import app.gaborbiro.pollrss.BaseJobViewHolder
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.utils.PUSHBULLET_PACKAGE
import app.gaborbiro.pollrss.utils.isPackageInstalled
import kotlinx.android.synthetic.main.card_job.view.*

class JobsAdapter(
    private val callback: JobAdapterCallback
) : BaseJobAdapter<JobViewHolder>(callback, R.layout.card_job) {

    interface JobAdapterCallback : BaseJobAdapterCallback {
        fun onMarkedAsRead(job: JobUIModel)
        fun onMarkedAsUnread(job: JobUIModel)
        fun onShare(job: JobUIModel)
        fun onFavorite(job: JobUIModel)
    }

    override fun createViewHolder(view: View) = JobViewHolder(view)

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val job = getItem(position)
        with(holder) {
            shareBtn.setImageResource(if (shareBtn.context.isPackageInstalled(PUSHBULLET_PACKAGE)) R.drawable.ic_computer else R.drawable.ic_share)
            if (job.markedAsRead) {
                markReadBtn.setImageResource(R.drawable.ic_unmark_as_read)
                markReadBtn.setOnClickListener {
                    callback.onMarkedAsUnread(job)
                }
                markReadBtn.imageTintList = shareBtn.context.getColorStateList(R.color.black_40)
            } else {
                markReadBtn.setImageResource(R.drawable.ic_mark_as_read)
                markReadBtn.setOnClickListener {
                    callback.onMarkedAsRead(job)
                }
                markReadBtn.imageTintList = shareBtn.context.getColorStateList(R.color.black_100)
            }
            shareBtn.setOnClickListener {
                callback.onShare(job)
            }
            favoriteBtn.setOnClickListener {
                callback.onFavorite(job)
            }
        }
    }

    fun markItemAsRead(job: JobUIModel) {
        job.markedAsRead = true
        notifyItemChanged(indexOf(job))
    }

    fun markItemAsUnread(job: JobUIModel) {
        job.markedAsRead = false
        notifyItemChanged(indexOf(job))
    }
}

class JobViewHolder(view: View) : BaseJobViewHolder(view) {
    val markReadBtn: ImageView = view.btn_mark_read
    val shareBtn: ImageView = view.btn_share
    val favoriteBtn: ImageView = view.btn_favorite
}