package com.Sextb // Khai báo package của class, giúp tổ chức code

import org.jsoup.nodes.Element // Import class Element từ thư viện Jsoup để làm việc với các phần tử HTML
import com.lagradost.cloudstream3.* // Import tất cả các class từ thư viện Cloudstream3
import com.lagradost.cloudstream3.utils.* // Import các hàm tiện ích từ thư viện Cloudstream3
import okhttp3.FormBody // Import class FormBody từ thư viện OkHttp để tạo request body cho các POST request

class SextbProvider : MainAPI() { // Khai báo class SextbProvider, kế thừa từ class MainAPI của Cloudstream3, đây là class chính của một provider

    override var mainUrl = "https://sextb.net" // Khai báo biến mainUrl, lưu trữ địa chỉ trang web, đây là địa chỉ gốc của trang web
    override var name = "Sextb" // Khai báo biến name, lưu trữ tên của provider, tên này sẽ hiển thị trong ứng dụng
    override val hasMainPage = true // Khai báo biến hasMainPage, cho biết provider này có trang chủ hay không
    override var lang = "en" // Khai báo biến lang, lưu trữ ngôn ngữ của nội dung, ở đây là tiếng Anh
    override val hasDownloadSupport = true // Khai báo biến hasDownloadSupport, cho biết provider này có hỗ trợ tải về hay không
    override val hasChromecastSupport = true // Khai báo biến hasChromecastSupport, cho biết provider này có hỗ trợ Chromecast hay không
    override val supportedTypes = setOf(TvType.NSFW) // Khai báo biến supportedTypes, cho biết provider này hỗ trợ loại nội dung nào, ở đây là NSFW (Not Safe For Work)
    override val vpnStatus = VPNStatus.MightBeNeeded // Khai báo biến vpnStatus, cho biết có cần VPN để truy cập nội dung hay không

    private val ajaxUrl = "$mainUrl/ajax/player" // Khai báo biến ajaxUrl, lưu trữ địa chỉ API để lấy link phim

    override val mainPage = mainPageOf( // Khai báo biến mainPage, định nghĩa các mục lục của trang chủ
        "/amateur" to "Amateur", // Mục "Amateur"
        "/censored" to "Censored", // Mục "Censored"
        "/uncensored" to "Uncensord", // Mục "Uncensored"
        "/subtitle" to "English Subtitled" // Mục "English Subtitled"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse { // Hàm lấy nội dung của trang chủ
        val document = app.get("$mainUrl${request.data}/pg-$page").document // Lấy HTML của trang web
        val responseList = document.select(".tray-item").mapNotNull { it.toSearchResult() } // Chọn tất cả các phần tử có class "tray-item" (mỗi phần tử này là một video) và chuyển đổi chúng thành các SearchResponse object
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = false), hasNext = true) // Trả về kết quả
    }

    private fun getRequestBody(episode: String, filmId: String): FormBody { // Hàm tạo request body cho các POST request
        return FormBody.Builder()
            .addEncoded("episode", episode) // Thêm tham số "episode" vào request body
            .addEncoded("filmId", filmId) // Thêm tham số "filmId" vào request body
            .build() // Xây dựng request body
    }

    private fun Element.toSearchResult(): SearchResponse { // Hàm chuyển đổi một phần tử HTML (video) thành SearchResponse object
        val title = this.select(".tray-item-title").text() // Lấy tiêu đề của video
        val href = mainUrl + this.select("a:nth-of-type(1)").attr("href") // Lấy link của video
        val posterUrl = this.selectFirst(".tray-item-thumbnail")?.attr("data-src") // Lấy link poster của video
        return newMovieSearchResponse(title, href, TvType.Movie) { // Tạo SearchResponse object
            this.posterUrl = posterUrl // Gán posterUrl cho SearchResponse object
        }
    }

    override suspend fun search(query: String): List<SearchResponse> { // Hàm tìm kiếm video
        val searchResponse = mutableListOf<SearchResponse>() // Khởi tạo một list để lưu trữ kết quả tìm kiếm

        for (i in 1..5) { // Tìm kiếm ở 5 trang đầu tiên
            val document = app.get("$mainUrl/search/$query/pg-$i").document // Lấy HTML của trang tìm kiếm
            val results = document.select(".tray-item").mapNotNull { it.toSearchResult() } // Chuyển đổi các phần tử HTML (video) thành SearchResponse object

            if (!searchResponse.containsAll(results)) { // Nếu kết quả tìm kiếm không bị trùng lặp
                searchResponse.addAll(results) // Thêm kết quả tìm kiếm vào list
            } else {
                break // Nếu kết quả tìm kiếm bị trùng lặp, dừng lại
            }

            if (results.isEmpty()) break // Nếu không có kết quả tìm kiếm, dừng lại
        }

        return searchResponse // Trả về kết quả tìm kiếm
    }

    override suspend fun load(url: String): LoadResponse { // Hàm lấy thông tin chi tiết của video
        val document = app.get(url).document // Lấy HTML của trang chi tiết video

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString().replace("| PornHoarder.tv", "") // Lấy tiêu đề của video
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content")) // Lấy link poster của video
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim() // Lấy mô tả của video

        return newMovieLoadResponse(title, url, TvType.NSFW, url) { // Tạo LoadResponse object
            this.posterUrl = poster // Gán posterUrl cho LoadResponse object
            this.plot = description // Gán description cho LoadResponse object
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean { // Hàm lấy link xem phim
        val doc = app.get(data).document // Lấy HTML của trang web
        val episodeList = doc.select(".episode-list .btn-player") // Chọn tất cả các phần tử có class "btn-player" (mỗi phần tử này là một tập phim)
        val sourceId = doc.selectFirst(".episode-list .btn-player")?.attr("data-source") // Lấy data-source của tập phim

        episodeList.forEach { item -> // Duyệt qua từng tập phim
            val requestBody = getRequestBody(item.attr("data-id"), sourceId ?: "") // Tạo request body cho POST request
            val doc = app.post(ajaxUrl, requestBody = requestBody).document // Gửi POST request để lấy link phim
            val iframeSrc = doc.select("iframe").attr("src") // Lấy link iframe
            val finalUrl = iframeSrc.replace("\\\"", "").replace("\\/", "\\").substringBefore("?") // Xử lý link iframe
            loadExtractor(finalUrl, subtitleCallback, callback) // Gọi hàm loadExtractor để lấy link phim
        }

        return true // Trả về true
    }
}
