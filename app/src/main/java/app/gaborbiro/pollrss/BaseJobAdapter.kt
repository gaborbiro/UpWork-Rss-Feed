package app.gaborbiro.pollrss

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import app.gaborbiro.pollrss.model.Job
import app.gaborbiro.pollrss.model.exactFormattedTime
import kotlinx.android.synthetic.main.card_job_base.view.*

abstract class BaseJobAdapter<VH : BaseJobViewHolder>(private val jobs: MutableList<Job>, @LayoutRes val itemLayout: Int) :
    RecyclerView.Adapter<VH>() {

    abstract fun createViewHolder(view: View): VH

    fun addItem(position: Int, job: Job) {
        jobs.add(position, job)
        notifyItemInserted(position)
    }

    fun removeItem(job: Job): Int {
        val position = jobs.indexOf(job)
        jobs.remove(job)
        notifyItemRemoved(position)
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return LayoutInflater.from(parent.context)
            .inflate(itemLayout, parent, false).let {
                createViewHolder(it)
            }
    }

    override fun getItemCount() = jobs.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val job = jobs[position]
        with(holder) {
            title.text = Html.fromHtml(job.title, 0)
            description.text = Html.fromHtml(job.description, 0)
            description.movementMethod = LinkMovementMethod.getInstance()
            posted.text = Html.fromHtml("${job.exactFormattedTime()} / <b>${job.country}</b>", 0)
        }
    }
}

open class BaseJobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.title
    val description: TextView = view.description
    val posted: TextView = view.posted
}