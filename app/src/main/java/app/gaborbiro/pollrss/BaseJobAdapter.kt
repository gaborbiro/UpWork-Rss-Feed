package app.gaborbiro.pollrss

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import app.gaborbiro.pollrss.jobs.JobUIModel
import app.gaborbiro.pollrss.utils.shrinkBetween
import kotlinx.android.synthetic.main.card_job_base.view.*

abstract class BaseJobAdapter<VH : BaseJobViewHolder>(
    private val jobs: MutableList<JobUIModel>,
    private val callback: BaseJobAdapterCallback,
    @LayoutRes val itemLayout: Int
) :
    RecyclerView.Adapter<VH>() {

    abstract fun createViewHolder(view: View): VH

    fun addItem(position: Int, job: JobUIModel) {
        jobs.add(position, job)
        notifyItemInserted(position)
    }

    fun indexOf(job: JobUIModel) = jobs.indexOf(job)

    fun removeItem(job: JobUIModel): Int {
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
            title.movementMethod = LinkMovementMethod.getInstance()
            description.text = Html.fromHtml(job.description, Html.FROM_HTML_MODE_LEGACY)
            description.movementMethod = LinkMovementMethod.getInstance()
            if (job.description.length > 400) {
                description.shrinkBetween(
                    textSize = R.dimen.fine_print_text_size,
                    startOffset = 128,
                    endOffset = 128
                )
            }
            posted.text = Html.fromHtml("${job.time} / <b>${job.country}</b>", 0)
            budget.text = job.budget ?: "-"
            title.setOnClickListener {
                callback.onBodyClicked(job)
            }
            description.setOnClickListener {
                callback.onBodyClicked(job)
            }
            posted.setOnClickListener {
                callback.onBodyClicked(job)
            }
            postedLabel.setOnClickListener {
                callback.onBodyClicked(job)
            }
            itemView.setOnClickListener {
                callback.onBodyClicked(job)
            }
            budget.setOnClickListener {
                callback.onBodyClicked(job)
            }
            budgetLabel.setOnClickListener {
                callback.onBodyClicked(job)
            }
        }
    }
}

open class BaseJobViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.title
    val description: TextView = view.description
    val postedLabel: TextView = view.posted_label
    val posted: TextView = view.posted
    val budgetLabel: TextView = view.budget_label
    val budget: TextView = view.budget
}

interface BaseJobAdapterCallback {
    fun onBodyClicked(job: JobUIModel)
}