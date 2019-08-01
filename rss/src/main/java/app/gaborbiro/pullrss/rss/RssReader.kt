package app.gaborbiro.pullrss.rss

import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

object RssReader {

    @Throws(SAXException::class, IOException::class)
    fun read(url: URL): RssFeed? {
        return read(url.openStream())
    }

    @Throws(SAXException::class, IOException::class)
    fun read(stream: InputStream): RssFeed? {
        try {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val reader = parser.xmlReader
            val handler = RssHandler()
            val input = InputSource(stream)
            reader.contentHandler = handler
            reader.parse(input)
            return handler.result
        } catch (e: ParserConfigurationException) {
            throw SAXException()
        }
    }

    @Throws(SAXException::class, IOException::class)
    fun read(source: String): RssFeed? {
        return read(ByteArrayInputStream(source.toByteArray()))
    }
}
