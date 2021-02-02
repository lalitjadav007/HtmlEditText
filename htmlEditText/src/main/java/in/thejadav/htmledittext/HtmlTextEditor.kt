package `in`.thejadav.htmledittext

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.webkit.*
import androidx.core.text.HtmlCompat
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.io.InputStream


class HtmlTextEditor(context: Context, attrs: AttributeSet?) : WebView(context, attrs) {
    private var editableHtmlTemplate: String = ""
    var isInputEnabled = true
    private var inputString = ""
    private var hint = ""

    internal inner class JsObject {
        // This field always keeps the latest edited text
        var text = MutableLiveData<String?>()

        @JavascriptInterface
        fun textDidChange(newText: String) {
            text.postValue(newText.replace("\n", ""))
        }
    }

    private val mJsObject: JsObject

    fun enableInput(enabled: Boolean) {
        isInputEnabled = enabled
        text = inputString
    }

    fun setHint(hint: String) {
        this.hint = hint
        text = inputString
    }

    // Init the text field in case it's read without editing the text before
    var text: String?
        get() = if (mJsObject.text.value == null) {
            ""
        } else mJsObject.text.value
        set(text) {
            var text = text
            if (text == null) {
                text = ""
            }
            inputString = text
            var htmlContent: String = editableHtmlTemplate.replace("___REPLACE___", text)
            htmlContent = htmlContent.replace("___INPUT_ENABLED___", isInputEnabled.toString())
            htmlContent = htmlContent.replace("___HINT___", hint)
            loadDataWithBaseURL(null,htmlContent,  null,"UTF-8", null)
            // Init the text field in case it's read without editing the text before
            mJsObject.text.postValue(text)
        }

    fun observeStringValue(): MutableLiveData<String?> {
        return mJsObject.text
    }

    fun getPlainText() : String {
        return HtmlCompat.fromHtml(HtmlCompat.fromHtml(text ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim(), HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    init {
        settings.javaScriptEnabled = true
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.loadWithOverviewMode = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        mJsObject = JsObject()
        addJavascriptInterface(mJsObject, "injectedObject")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, request.url)
                view.context.startActivity(intent)
                return true
            }
        }
        var `is`: InputStream? = null
        try {
            `is` = context.assets.open("htmlEditor.html")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            editableHtmlTemplate = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        text = ""
    }
}