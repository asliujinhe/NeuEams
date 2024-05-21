package org.jetos.data

/**
 * 课程表数据类
{
    "courseId": "103055(A070835)",
    "courseName": "工程伦理学(A070835)",
    "experiItemName": "",
    "remark": null,
    "roomId": "454",
    "roomName": "信息A109(浑南校区)",
    "schGroupNo": "",
    "taskId": null,
    "teacherId": "9863,10341,11272,13741",
    "teacherName": "任涛,李昕,刘益先,曾荣飞",
    "vaildWeeks": "00100000000000000000000000000000000000000000000000000" //注意这里有笔误
*/
data class MetaCourse(
    val courseId: String,//"103055(A070835)"
    val courseName: String,//"工程伦理学(A070835)"
    val experiItemName: String,//""
    val remark: String?,//null
    val roomId: String,//"454"
    val roomName: String,//"信息A109(浑南校区)"
    val schGroupNo: String,//""
    val taskId: String?,//null
    val teacherId: String,//"9863,10341,11272,13741"
    val teacherName: String,//"任涛,李昕,刘益先,曾荣飞"
    //validWeeks, misspelled
    val vaildWeeks: String,//"00100000000000000000000000000000000000000000000000000"

)

data class Student(
    val studentId: Int,//78649
    val studentNo: String,//"2018xxxx"
    val studentName: String,//"张三"
    val clazz: String//"软件工程2101"
)

data class Teacher(
    val teacherId: Int,//9863
    var teacherNo: String?,//"0000xxxx"
    var teacherName: String?,//"任涛"
    val gender: Int?,//"男0女1未知2"
    val college: String//"软件学院"
) {
    constructor(teacherId: Int) : this(teacherId, null, null, null, "")
}

data class Room(
    val roomId: Int,//454
    val roomName: String//"信息A109(浑南校区)"
)

data class Course(
    val lessonId: Int,//103055
    val courseId: String,//"A070835"
    var courseName: String,//"工程伦理学"
    val teachers: List<Teacher>,//[(9863,"任涛"),(10341,"李昕"),(11272,"刘益先"),(13741,"曾荣飞")]
    val metaCourses: MutableList<MetaCourse>,
    val semesterId: String,//"72"
    val coursePeriods: MutableList<CoursePeriod>,

    //addition
    var category: String?,//"公共选修课"
    var courseCode: String?,//"A1001040070
    var credit: String?,//"2.0"
    var className: String?,//"采矿2101-02"
) {
    override fun equals(other: Any?): Boolean {
        return if (other is Course) {
            this.lessonId == other.lessonId && this.courseId == other.courseId
        } else false
    }

    override fun hashCode(): Int {
        var result = lessonId.hashCode()
        result = 31 * result + courseId.hashCode()
        return result
    }
}

data class CoursePeriod(
    val time: Int,//0
    val room: Room,//(454,"信息A109(浑南校区)")
    val validWeeks: String//"00100000000000000000000000000000000000000000000000000"
){
    fun getWeeks(): List<Int> {
        return validWeeks.mapIndexed { index, c -> index to c }
            .filter { it.second == '1' }
            .map { it.first }
    }

    fun getDayTime(): Pair<Int, Int> {
        //7-1
        //1~12
        val day = time / 12
        val time = time % 12
        return Pair(day, time)
    }

    fun getDayTimeStr(): String {
        val (day, time) = getDayTime()
        return when (day) {
            0 -> "周日${time + 1}-${time + 2}"
            1 -> "周一${time + 1}-${time + 2}"
            2 -> "周二${time + 1}-${time + 2}"
            3 -> "周三${time + 1}-${time + 2}"
            4 -> "周四${time + 1}-${time + 2}"
            5 -> "周五${time + 1}-${time + 2}"
            6 -> "周六${time + 1}-${time + 2}"
            else -> "未知"
        }
    }
}
