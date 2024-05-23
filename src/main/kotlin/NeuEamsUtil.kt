package org.jetos

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetos.data.Course
import org.jetos.data.Teacher
import org.jetos.net.ConnectUtil
import org.jetos.net.RequestQueueManager
import java.io.File
import java.util.function.Consumer
import java.util.logging.Logger

/**
 * 一个工具类，用于获取东北大学教务系统的课程表信息
 * @see ConnectUtil
 * @see RequestQueueManager
 * @see Course
 * @see Teacher
 * @see RequestQueueManager.Config
 */
@Suppress("unused")
object NeuEamsUtil {
    private val logger = Logger.getLogger("NeuEamsUtil")
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
    @JvmStatic
    fun setConfig(config: RequestQueueManager.Config) {
        ConnectUtil.config = config
    }

    /**
     * （可选）设置配置文件默认使用运行所在目录进行cookie文件的创建，以此来进行登录或保持登录状态
     * @see setConfig
     */
    @JvmStatic
    fun setConfig(cookieFile: File, period: Int) {
        ConnectUtil.config = RequestQueueManager.Config(cookieFile, period)
    }

    /**
     * 已经改为自动调用，请使用[login]方法进行用户名密码设置，完成登录
     *
     * 未设置用户名和密码否则一定报错
     * @throws IllegalArgumentException 如果用户名密码格式错误
     */
    private fun setNameAndPassword(name: String, password: String) {
        if (name.isBlank() || password.isBlank() || name.trim().length != 8)
            throw IllegalArgumentException("Username or Password is not properly formatted")
        ConnectUtil.username = name
        ConnectUtil.password = password
    }

    /**
     * 登录
     * @param name 用户名
     * @param password 密码
     * @param callback 代码调用回调
     * @return 通过回调返回登录成功Boolean，有问题则返回null
     * @throws IllegalArgumentException 如果用户名密码格式错误
     */
    @JvmStatic
    fun login(name: String, password: String, callback: Consumer<Boolean?>) {
        logout()
        setNameAndPassword(name, password)
        getUserActualName {
            callback.accept(it != null)
        }
    }

    /**
     * 注销：清除cookie，退出登录，清除用户名密码以及用户Id，必须使用[login]方法重新登录
     * @see ConnectUtil.logout
     */
    @JvmStatic
    fun logout() {
        ConnectUtil.logout()
    }

    /**
     * 检查网络
     * @param callback 代码调用回调
     */
    @JvmStatic
    fun checkNetwork(callback: Consumer<Boolean?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.checkNetwork())
            } catch (e: Exception) {
                callback.accept(false)
                logger.warning(e.message)
            }
        }
    }

    /**
     * 获取当前登录用户的真实姓名
     * @param callback 代码调用回调
     * @return 通过回调返回真实姓名，有问题则返回null Formatted as "张三(2021xxxx)"
     */
    @JvmStatic
    fun getUserActualName(callback: Consumer<String?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getHome())
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
        }
    }

    /**
     * 学生
     * 获取当前学生的Ids，仅学生端有效（教师端未知）
     * @see ConnectUtil.getIds
     */
    @JvmStatic
    fun getIds(callback: Consumer<String?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getIds())
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
        }
    }

    /**
     * 获取当前登录学生的所有课程
     * @see ConnectUtil.getCourse
     * @param semesterId 学期Id，2023-2024春季学期为`72`
     * @return 课程列表，如果网络或鉴权异常会返回null
     * @throws
     */
    @JvmStatic
    fun getCourse(semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getCourse(semesterId = semesterId))
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
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
    @JvmStatic
    fun getTeacherCourse(teacher: Teacher, semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getCourseByTeacher(teacher, semesterId))
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
        }
    }

    /**
     * A simplified version for `getTeacherCourse`
     * @see getTeacherCourse
     */
    @JvmStatic
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
    @JvmStatic
    fun getStudentCourse(studentId: Int, semesterId: String, callback: Consumer<List<Course>?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getCourseByStudent(studentId, semesterId))
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
        }
    }

    /**
     * 获取某个学期下教师的列表
     * @param semesterId 学期Id
     * @param callback 代码调用回调
     * @return 通过回调返回教师列表，有问题则返回null
     */
    @JvmStatic
    fun getTeacherList(semesterId: String, callback: Consumer<List<Teacher>?>) {
        CoroutineScope(coroutineScope).launch {
            try {
                callback.accept(ConnectUtil.getAllTeacherList(semesterId))
            } catch (e: Exception) {
                callback.accept(null)
                logger.warning(e.message)
            }
        }
    }
}