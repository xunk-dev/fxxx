package com.frameout.app.search

import com.frameout.core.search.ImageSearchEngines
import com.frameout.core.search.SearchEngine

internal data class UploadSpec(
    val origin: String,
    val action: String,
    val field: String
) {
    fun html(): String {
        val value = "var d=new DataTransfer();d.items.add(new File([b],'frame.jpg',{type:b.type||'image/jpeg'}));t.files=d.files;f.submit();"
        val input = "<input type=\"file\" name=\"$field\" id=\"t\">"
        return "<!doctype html><meta name=\"viewport\" content=\"width=device-width\"><body style=\"background:#000;color:#ccc;font-family:sans-serif\">" +
            "<form id=\"f\" method=\"post\" enctype=\"multipart/form-data\" action=\"$action\">$input</form><p>…</p>" +
            "<script>var f=document.getElementById('t').form,t=document.getElementById('t');" +
            "fetch('${ImageSearchActivity.IMAGE_PATH}').then(function(r){return r.blob()}).then(function(b){$value}).catch(function(){document.body.textContent='upload failed'});</script>"
    }
}

internal object UploadSpecs {
    fun of(engine: SearchEngine): UploadSpec? = when (engine.id) {
        ImageSearchEngines.LENS_ID -> UploadSpec("https://lens.google.com", "https://lens.google.com/v3/upload?ep=gsbubb", "encoded_image")
        else -> null
    }
}
