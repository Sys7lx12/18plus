package com.Sextb // Khai báo package của class

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin // Import annotation để đánh dấu class là plugin
import com.lagradost.cloudstream3.plugins.Plugin // Import class Plugin, class cha của SextbPlugin
import android.content.Context // Import Context để truy cập tài nguyên Android
import com.Sextb.Stbturbo // Import extractor Stbturbo tự viết
import com.lagradost.cloudstream3.extractors.StreamTape // Import extractor StreamTape
//import com.lagradost.cloudstream3.extractors.Wishonly // Import extractor Wishonly (đã được comment)

@CloudstreamPlugin // Annotation đánh dấu class là plugin của Cloudstream
class SextbPlugin: Plugin() { // Khai báo class SextbPlugin kế thừa từ class Plugin
    override fun load(context: Context) { // Hàm load được gọi khi plugin được tải
        registerMainAPI(SextbProvider()) // Đăng ký API chính của plugin, sử dụng SextbProvider
        registerExtractorAPI(StreamTape()) // Đăng ký extractor StreamTape
        //registerExtractorAPI(Wishonly()) // Đăng ký extractor Wishonly (đã được comment)
        registerExtractorAPI(Stbturbo()) // Đăng ký extractor Stbturbo
    }
}
