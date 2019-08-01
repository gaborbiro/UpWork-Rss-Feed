package app.gaborbiro.pollrss.rss

import android.annotation.SuppressLint
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.lang.reflect.InvocationTargetException

class RssHandler : DefaultHandler() {

    lateinit var result: RssFeed
        private set
    private var rssItem: RssItem? = null
    private lateinit var stringBuilder: StringBuilder

    override fun startDocument() {
        result = RssFeed()
    }

    override fun startElement(
        uri: String, localName: String, qName: String, attributes: Attributes
    ) {
        stringBuilder = StringBuilder()

        if (qName == "item") {
            rssItem = RssItem(feed = result).also(result::addRssItem)
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        stringBuilder.append(ch, start, length)
    }

    @SuppressLint("DefaultLocale")
    @Suppress("NAME_SHADOWING")
    override fun endElement(uri: String, localName: String, qName: String?) {
        try {
            (rssItem ?: result).setValue(qName?.replace("content:encoded", "content"))
        } catch (e: SecurityException) {
        } catch (e: NoSuchMethodException) {
        } catch (e: IllegalArgumentException) {
        } catch (e: IllegalAccessException) {
        } catch (e: InvocationTargetException) {
        }
    }

    private fun getSetterMethodName(qName: String) =
        "set" + qName.substring(0, 1).toUpperCase() + qName.substring(1)

    private fun Any.setValue(qName: String?) {
        if (qName != null && qName.isNotEmpty()) {
            val method = javaClass.getMethod(getSetterMethodName(qName), String::class.java)
            method.invoke(this, stringBuilder.toString())
        }
    }
}
