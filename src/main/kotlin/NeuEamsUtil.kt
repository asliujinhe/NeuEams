package org.jetos

import kotlinx.coroutines.*
import org.jetos.data.Course
import org.jetos.data.Teacher
import org.jetos.net.ConnectUtil
import org.jetos.net.RequestQueueManager
import java.io.File
import java.util.function.Consumer

/**
 * 一个工具类，用于获取东北大学教务系统的课程表信息
 * @see ConnectUtil
 * @see RequestQueueManager
 * @see Course
 * @see Teacher
 * @see RequestQueueManager.Config
 */
object NeuEamsUtil {
    private val coroutineScope = Dispatchers.IO
    private val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println(throwable.message)
    }

    /**
     * （可选）设置配置文件默认使用运行所在目录进行cookie文件的创建，以此来进行登录或保持登录状态
     * @param config 配置文件
     *
     * 其中：
     * `cookieFile` cookie文件（默认为工作目录下的`cookie.json`）
     * `period` 请求间隔
     *
     * 对于Kotlin:
     * ```kotlin
     * RequestQueueManager.Config(
     *    cookieFile = File("cookie.txt"),
     *    period = 1000
     * )
     * ```
     * 对于Java:
     * ```java
     * RequestQueueManager.Config config = new RequestQueueManager.Config(
     *   new File("cookie.txt"),
     *   1000
     * );
     * @see RequestQueueManager.Config
     *
     */
    fun setConfig(config: RequestQueueManager.Config) {
        ConnectUtil.config = config
    }

    /**
     * （可选）设置配置文件默认使用运行所在目录进行cookie文件的创建，以此来进行登录或保持登录状态
     * @see setConfig
     */
    fun setConfig(cookieFile: File, period: Int) {
        ConnectUtil.config = RequestQueueManager.Config(cookieFile, period)
    }

    /**
     * 注销
     * @see ConnectUtil.logout
     */
    fun logout() {
        ConnectUtil.logout()
    }

    /**
     * （必须）设置用户名和密码否则一定报错
     * @throws IllegalArgumentException 如果用户名密码格式错误
     */
    fun setNameAndPassword(name: String, password: String) {
        if (name.isBlank() || password.isBlank() || name.trim().length != 8)
            throw IllegalArgumentException("Username or Password is not properly formatted")
        ConnectUtil.username = name
        ConnectUtil.password = password
    }

    /**
     * 获取当前登录用户的真实姓名
     * @param callback 代码调用回调
     * @return 通过回调返回真实姓名，有问题则返回null Formatted as "张三(2021xxxx)"
     */
    fun getUserActualName(callback: Consumer<String?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getHome())
            }.join()
        }
    }

    /**
     * 学生
     * 获取当前学生的Ids，仅学生端有效（教师端未知）
     * @see ConnectUtil.getIds
     */
    fun getIds(callback: Consumer<String?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getIds())
            }.join()
        }
    }

    /**
     * 获取当前登录学生的所有课程
     * @see ConnectUtil.getCourse
     * @param semesterId 学期Id，2023-2024春季学期为`72`
     * @return 课程列表，如果网络或鉴权异常会返回null
     * @throws
     */
    fun getCourse(semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getCourse(semesterId = semesterId))
            }.join()
        }
    }

    /**
     * @see ConnectUtil.getCourseByTeacher
     * @param teacher 教师
     * @param semesterId 学期Id
     * @see getCourse
     * @see Teacher
     * @see getTeacherCourse
     */
    fun getTeacherCourse(teacher: Teacher, semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getCourseByTeacher(teacher, semesterId))
            }.join()
        }
    }

    /**
     * A simplified version for `getTeacherCourse`
     * @see getTeacherCourse
     */
    fun getTeacherCourse(teacherId: Int, semesterId: String, callback: Consumer<List<Course>?>) {
        return getTeacherCourse(Teacher(teacherId), semesterId, callback)
    }

    /**
     * 获取学生的课表（通过Id）
     * @param studentId 学生Id
     * @param semesterId 学期Id
     * @param callback 代码调用回调
     * @return 通过回调返回课程列表，有问题则返回null
     * @see getTeacherCourse
     */
    fun getStudentCourse(studentId: Int, semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getCourseByStudent(studentId, semesterId))
            }.join()
        }
    }

    /**
     * 获取某个学期下教师的列表
     * @param semesterId 学期Id
     * @param callback 代码调用回调
     * @return 通过回调返回教师列表，有问题则返回null
     */
    fun getTeacherList(semesterId: String, callback: Consumer<List<Teacher>?>) {
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback.accept(ConnectUtil.getAllTeacherList(semesterId))
            }
        }
    }
}