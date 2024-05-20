package net

import com.alibaba.fastjson2.JSON
import data.Course
import data.Teacher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import util.StudentCourseParser
import util.TeacherCourseParser
import java.io.File
import java.util.*
import java.util.logging.Logger

private data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean
) {
    fun toCookie(): Cookie {
        return Cookie.Builder().name(name).value(value).expiresAt(expiresAt).domain(domain).path(path).apply {
                if (secure) secure()
                if (httpOnly) httpOnly()
            }.build()
    }

    companion object {
        fun fromCookie(cookie: Cookie): SerializableCookie {
            return SerializableCookie(
                cookie.name, cookie.value, cookie.expiresAt, cookie.domain, cookie.path, cookie.secure, cookie.httpOnly
            )
        }
    }
}

private class SimpleCookieJar(private val file: File) : CookieJar {
    private val cookieStore: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    private fun saveCookie() {
        val allCookies = cookieStore.flatMap { entry ->
            entry.value.map { SerializableCookie.fromCookie(it) }
        }

        val json = JSON.toJSONString(allCookies)
        file.writeText(json)
    }

    private fun loadCookie() {
        if (!file.exists()) return

        val json = file.readText()
        val allCookies = JSON.parseArray(json, SerializableCookie::class.java)

        allCookies.forEach { serializableCookie ->
            val cookie = serializableCookie.toCookie()
            val host = cookie.domain
            val storedCookies = cookieStore[host] ?: mutableListOf()
            storedCookies.add(cookie)
            cookieStore[host] = storedCookies
        }
    }

    init {
        loadCookie()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val storedCookies = cookieStore[host] ?: mutableListOf()

        cookies.forEach { newCookie ->
            val iterator = storedCookies.iterator()
            while (iterator.hasNext()) {
                val existingCookie = iterator.next()
                if (existingCookie.name == newCookie.name && existingCookie.path == newCookie.path) {
                    iterator.remove()
                }
            }
            storedCookies.add(newCookie)
        }

        cookieStore[host] = storedCookies

        saveCookie()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val storedCookies = cookieStore[host] ?: return emptyList()
        val validCookies = storedCookies.filter { it.expiresAt > System.currentTimeMillis() && it.matches(url) }
        if (validCookies.size < storedCookies.size) {
            cookieStore[host] = validCookies.toMutableList()
        }
        return validCookies
    }
}


class RequestQueueManager(
    val config: Config = Config(
        cookieFile = File("cookie.json"), period = 1000
    )
) {
    private val logger: Logger = Logger.getLogger("RequestQueueManager")
    /**
     * @param cookieFile 保存cookie的文件
     * @param period 请求间隔
     * @param retryWhen 重试条件 (Your config may be rewritten)
     * @param retryTimes 重试次数 (Your config may be rewritten)
     * @param beforeRetry 重试前的操作 (Your config may be rewritten)
     * */
    data class Config(
        val cookieFile: File,
        val period: Int,
        var retryWhen: (QueueResponse) -> Boolean = { false },
        var retryTimes: Int = 3,
        var beforeRetry: suspend () -> Unit = {}
    )

    class QueueResponse(val response: Response){
        val responseText: String by lazy {
            response.use {
                it.body!!.string()
            }
        }
        val isSuccessful: Boolean = response.isSuccessful

        val url: HttpUrl = response.request.url

        operator fun component1(): Response = response
        operator fun component2(): String = responseText
    }

    class RetryException(override val message: String?) : CancellationException()

    data class QueueRequest(val request: Request, var retryRemains: Int = -1, val priority: Int = 0)
    
    data class QueueItem(val request: QueueRequest, val response: CompletableDeferred<QueueResponse>):Comparable<QueueItem> {
        override fun compareTo(other: QueueItem): Int {
            return other.request.priority - this.request.priority
        }
    }

    private val client: OkHttpClient
    private val requestChannel: Channel<QueueItem>

    private val cookieJar: SimpleCookieJar = SimpleCookieJar(config.cookieFile)

    companion object {
        private val instances = mutableMapOf<Config, RequestQueueManager>()

        fun getInstance(config: Config): RequestQueueManager {
            return instances.getOrPut(config) { RequestQueueManager(config) }
        }
    }

    init {
        client = OkHttpClient.Builder().cookieJar(cookieJar).build()
        requestChannel = Channel()

        // 启动一个协程来处理队列中的请求
        CoroutineScope(Dispatchers.IO).launch {
            val priorityQueue = PriorityQueue<QueueItem>()
            while (true) {
                select<Unit> {
                    requestChannel.onReceive {
                        priorityQueue.add(it)
                    }
                }
                while (priorityQueue.isNotEmpty()) {
                    val (queueRequest, responseDeferred) = priorityQueue.poll()
                    val (request, remainRetry) = queueRequest
                    try {
                        val response = client.newCall(request).execute()
                        val queueResponse = QueueResponse(response)
                        if (config.retryWhen(queueResponse) && remainRetry != -1) {
                            if (remainRetry <= 0)  {
                                responseDeferred.completeExceptionally(RetryException("$request retry failed, no retry times remain"))
                            }else{
                                logger.info("Retry required, append to end of queue, remain retry times: $remainRetry")
                                queueRequest.retryRemains -= 1
                                CoroutineScope(Dispatchers.IO).launch {
                                    config.beforeRetry()
                                    requestChannel.send(QueueItem(queueRequest, responseDeferred))
                                }
                            }
                        } else {
                            responseDeferred.complete(queueResponse)
                        }
                    } catch (e: Exception) {
                        responseDeferred.completeExceptionally(e)
                    }
                    delay(config.period.toLong())
                }
            }
        }
    }


    fun enqueueRequest(request: Request, doNotRetry: Boolean = false): Deferred<QueueResponse> {
        val responseDeferred = CompletableDeferred<QueueResponse>()
        CoroutineScope(Dispatchers.IO).launch {
            requestChannel.send(QueueItem(QueueRequest(request,if (doNotRetry) -1 else config.retryTimes), responseDeferred))
        }
        return responseDeferred
    }

    fun enqueueRequest(url: String, doNotRetry: Boolean = false): Deferred<QueueResponse> {
        val request = Request.Builder().url(url).build()
        return enqueueRequest(request, doNotRetry)
    }
}

object ConnectUtil {
    private const val HOME_URL = "http://219.216.96.4/eams/homeExt.action"
    private const val COURSE_URL = "http://219.216.96.4/eams/courseTableForStd!courseTable.action"
    private const val COURSE_HOME_URL = "http://219.216.96.4/eams/courseTableForStd.action"
    private const val PUBLIC_COURSE_URL = "http://219.216.96.4/eams/studentPublicScheduleQuery!courseTable.action"
    private const val PUBLIC_QUERY_URL = "http://219.216.96.4/eams/studentPublicScheduleQuery!search.action"
    private val logger: Logger = Logger.getLogger("ConnectUtil")

    var username: String? = null
    var password: String? = null

    var config: RequestQueueManager.Config = RequestQueueManager.Config(File("cookie.json"), 1000)
        set(value) {
            value.beforeRetry = {
                getHome()
            }
            value.retryTimes = 3

            value.retryWhen = { response ->
                if (response.url.toString().contains("login")) {
                    logger.info("Login required: System will login and retry")
                    true
                }else false
            }

            _requestQueueManager = RequestQueueManager.getInstance(value)
            field = value
        }
        get() {
            config = field
            return field
        }

    private var ids: String? = null

    private var _requestQueueManager =
        RequestQueueManager.getInstance(config)

    /**
     * 请求主页，获取登录地址，构造登录表单，登录
     * 会自动判断是否需要登录
     */
    suspend fun getHome() {
        assert(username != null)
        assert(password != null)
        val request = _requestQueueManager.enqueueRequest(HOME_URL, true)
        val response = request.await()
        //获取地址
        val body = response.responseText
        val url = response.response.request.url

        if (url.toString().contains("login")) {
            logger.info("Login required")
            val document: Document = Jsoup.parse(body)
            var targetUrl = document.select("form").attr("action")
            //resolve url with base url
            targetUrl = url.resolve(targetUrl).toString()
            val lt = document.select("#lt").attr("value")
            val ul = username!!.length
            val pl = password!!.length
            val rsa = username + password + lt
            val execution = document.select("input[name=execution]").attr("value")
            val eventId = document.select("input[name=_eventId]").attr("value")
            val formBody =
                FormBody.Builder().add("rsa", rsa).add("lt", lt).add("ul", ul.toString()).add("pl", pl.toString())
                    .add("execution", execution).add("_eventId", eventId).build()
            //构造登录表单
            val loginRequest = Request.Builder().url(targetUrl).post(formBody).build()
            _requestQueueManager.enqueueRequest(loginRequest, true).await().let { loginResponse ->
                val loginUrl = loginResponse.url.toString()
                if (loginUrl.contains(HOME_URL)) {
                    logger.info("Login success")
                } else {
                    logger.info("Login failed")
                }
            }
        }
    }

    /**
     * 获取当前用户ids
     * */
    suspend fun getIds() {
        val request = _requestQueueManager.enqueueRequest(COURSE_HOME_URL + "?_=" + System.currentTimeMillis())
        this.ids = request.await().let { (_,responseText) ->
            val ids = Regex(""""ids","(\d+)"""").find(responseText)!!.groupValues[1]
            logger.info("已经获取到用户ids: $ids")
            ids
        }
    }

    /**
     * 获取学生本人课程表
     * @param ignoreHead 是否忽略表头
     * @param showPrintAndExport 是否显示打印和导出按钮
     * @param settingKind std
     * @param startWeek 开始周
     * @param projectId 1
     * @param semesterId 72
     */
    suspend fun getCourse(
        ignoreHead: Boolean = true,
        showPrintAndExport: Boolean = true,
        settingKind: String = "std",
        startWeek: String = "",
        projectId: String = "1",
        semesterId: String = "72",
    ) {
        assert(ids != null)
        //构造请求表单 form-data
        val formBody = FormBody.Builder().add("ignoreHead", ignoreHead.toString())
            .add("showPrintAndExport", showPrintAndExport.toString()).add("setting.kind", settingKind)
            .add("startWeek", startWeek).add("project.id", projectId).add("semester.id", semesterId).add("ids", ids!!)
            .build()
        val request = Request.Builder().url(COURSE_URL).post(formBody).addHeader("Referer", COURSE_HOME_URL).build()
        val response = _requestQueueManager.enqueueRequest(request)

        response.await().let { (courseResponse, body) ->
            if (courseResponse.isSuccessful) {
                logger.info("获取课程表成功")
                StudentCourseParser(semesterId).parseCourse(body)
            } else {
                logger.info("获取课程表失败")
            }
        }
    }

    /**
     * 从官网教师列表中获取数据后调用
     * @param teacher 教师
     * @param semesterId 学期id
     */
    suspend fun getCourseByTeacher(teacher: Teacher, semesterId: String = "72"):List<Course> {
        val url = "$PUBLIC_COURSE_URL?setting.kind=teacher&ids=${teacher.teacherId}&semester.id=$semesterId"
        val response = _requestQueueManager.enqueueRequest(url)
        response.await().let { (courseResponse, body) ->
            if (courseResponse.isSuccessful) {
                logger.info("获取课程表成功")
                return TeacherCourseParser(teacher, semesterId).parseCourse(body)
            } else {
                logger.info("获取课程表失败")
            }
        }
        return emptyList()
    }

    /**
     * 获取学生课程表（此处为vulnerability(漏洞)可能会导致泄露所有学生的课程表）
     * @param studentId 学生id
     * @param semesterId 学期id
     */
    suspend fun getCourseByStudent(studentId: Int, semesterId: String = "72") {
        val url = "$PUBLIC_COURSE_URL?setting.kind=std&ids=${studentId}&semester.id=$semesterId"
        val response = _requestQueueManager.enqueueRequest(url)
        response.await().let { courseResponse ->
            val body = courseResponse.responseText
            if (courseResponse.isSuccessful) {
                logger.info("获取学生${studentId}的课程表成功")
                StudentCourseParser(semesterId).parseCourse(body)
            } else {
                logger.info("获取学生${studentId}的课程表失败")
            }
        }
    }

    /**
     * 获取当前学期所有的教师列表
     * @param semesterId 学期id
     * 原请求格式：semester.id=72&courseTableType=teacher&_=1715861047012&pageNo=1&pageSize=3000 POST
     *
     * */
    suspend fun getAllTeacherList(semesterId: String = "72"): List<Teacher> {
        val formBody =
            FormBody.Builder().add("courseTableType", "teacher").add("project.id", "1").add("semester.id", semesterId)
                .add("pageNo", "1").add("pageSize", "3000").add("_", System.currentTimeMillis().toString()).build()
        val request = Request.Builder().url(PUBLIC_QUERY_URL).post(formBody).build()
        val response = _requestQueueManager.enqueueRequest(request)
        val teachers = mutableListOf<Teacher>()
        response.await().let{ courseResponse ->
            if (courseResponse.isSuccessful) {
                logger.info("获取教师列表成功")
                courseResponse.responseText.let {
                    logger.info("body: ${it.length}")
                    val doc = Jsoup.parse(it)
                    val teacherElements = doc.selectFirst("tbody")?.select("tr")
                    logger.info("teachers: ${teacherElements?.size}")
                    teacherElements?.forEach { teacher ->
                        val teacherId = teacher.select("td")[0].select("input").attr("value").toInt()
                        val teacherName = teacher.select("td")[1].text()
                        val gender =
                            teacher.select("td")[2].text().run { if (this == "男") 0 else if (this == "女") 1 else 2 }
                        val college = teacher.select("td")[3].text()
                        teachers += Teacher(teacherId, null, teacherName, gender, college).apply {
                            logger.info("teacherId: $teacherId, teacherNo: $teacherNo, teacherName: $teacherName, gender: $gender, college: $college")
                        }
                    }
                }
            } else {
                logger.info("获取教师列表失败")
            }
        }
        return teachers
    }
}

fun main() = runBlocking {

    if (ConnectUtil.username == null || ConnectUtil.password == null) {
        throw IllegalStateException("Please input your username and password")
    }
    ConnectUtil.getAllTeacherList()
    ConnectUtil.getIds()
    ConnectUtil.getCourse()
    ConnectUtil.getCourseByTeacher(Teacher(teacherId = 12099)).let { courses ->
        for (course in courses) {
            println(course.coursePeriods.map { it.getWeeks() to it.getDayTime() to it.getDayTimeStr() }.sortedBy { it.first.second.second }.sortedBy { it.first.second.first }.map { it.first.first to it.second })
        }
    }
}
