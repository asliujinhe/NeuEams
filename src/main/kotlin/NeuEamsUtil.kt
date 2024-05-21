package org.jetos

import kotlinx.coroutines.*
import org.jetos.data.Course
import org.jetos.data.Teacher
import org.jetos.net.ConnectUtil
import org.jetos.net.RequestQueueManager


object NeuEamsUtil {
    private val coroutineScope = Dispatchers.IO
    private val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println(throwable.message)
    }
    /**
     * （可选）设置配置文件默认使用运行所在目录进行cookie文件的创建
     * @see RequestQueueManager.Config
     *
     */
    fun setConfig(config: RequestQueueManager.Config){
        ConnectUtil.config = config
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
     * 学生
     * 获取当前学生的Ids，仅学生端有效（教师端未知）
     * @see ConnectUtil.getIds
     */
    fun getIds(callback:(String?)->Unit){
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback(ConnectUtil.getIds())
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
    fun getCourse(semesterId:String, callback: (List<Course>?) -> Unit){
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback(ConnectUtil.getCourse(semesterId = semesterId))
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
    fun getTeacherCourse(teacher: Teacher, semesterId: String, callback: (List<Course>?) -> Unit){
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback(ConnectUtil.getCourseByTeacher(teacher, semesterId))
            }.join()
        }
    }

    /**
     * A simplified version for `getTeacherCourse`
     * @see getTeacherCourse
     */
    fun getTeacherCourse(teacherId: Int, semesterId: String, callback: (List<Course>?) -> Unit){
        return getTeacherCourse(Teacher(teacherId),semesterId,callback)
    }

    /**
     * 获取学生的课表（通过Id）
     * @param studentId 学生Id
     * @param semesterId 学期Id
     * @param callback 代码调用回调
     * @return 通过回调返回课程列表，有问题则返回null
     * @see getTeacherCourse
     */
    fun getStudentCourse(studentId: Int, semesterId: String, callback: (List<Course>?) -> Unit){
        CoroutineScope(coroutineScope).launch {
            launch(handler) {
                callback(ConnectUtil.getCourseByStudent(studentId, semesterId))
            }.join()
        }
    }

    /**
     * 获取某个学期下教师的列表
     * @param semesterId 学期Id
     * @param callback 代码调用回调
     * @return 通过回调返回教师列表，有问题则返回null
     */
    fun getTeacherList(semesterId: String, callback: (List<Teacher>?)->Unit){
        CoroutineScope(coroutineScope).launch {
            launch(handler){
                callback(ConnectUtil.getAllTeacherList(semesterId))
            }
        }
    }
}