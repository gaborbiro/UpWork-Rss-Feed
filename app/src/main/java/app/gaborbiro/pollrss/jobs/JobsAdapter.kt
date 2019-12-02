package app.gaborbiro.pollrss.jobs

import android.view.View
import android.widget.ImageView
import app.gaborbiro.pollrss.BaseJobAdapter
import app.gaborbiro.pollrss.BaseJobAdapterCallback
import app.gaborbiro.pollrss.BaseJobViewHolder
import app.gaborbiro.pollrss.R
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.utils.PUSHBULLET_PACKAGE
import app.gaborbiro.pollrss.utils.isPackageInstalled
import kotlinx.android.synthetic.main.card_job.view.*

class JobsAdapter(private val jobs: MutableList<Job>,
                  private val callback: JobAdapterCallback) :
    BaseJobAdapter<JobViewHolder>(jobs, callback, R.layout.card_job) {

    interface JobAdapterCallback: BaseJobAdapterCallback {
        fun onMarkedAsRead(job: Job)
        fun onShare(job: Job)
        fun onFavorite(job: Job)
    }

    override fun getItemCount() = jobs.size

    override fun createViewHolder(view: View) = JobViewHolder(view)

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val job = jobs[position]
        with(holder) {
            shareBtn.setImageResource(if (shareBtn.context.isPackageInstalled(PUSHBULLET_PACKAGE)) R.drawable.ic_computer else R.drawable.ic_share)
            markReadBtn.setOnClickListener {
                callback.onMarkedAsRead(job)
            }
            shareBtn.setOnClickListener {
                callback.onShare(job)
            }
            favoriteBtn.setOnClickListener {
                callback.onFavorite(job)
            }
        }
    }
}

class JobViewHolder(view: View) : BaseJobViewHolder(view) {
    val markReadBtn: ImageView = view.btn_mark_read
    val shareBtn: ImageView = view.btn_share
    val favoriteBtn: ImageView = view.btn_favorite
}