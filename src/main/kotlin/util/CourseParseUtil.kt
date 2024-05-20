package util

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.TypeReference
import data.*
import org.jsoup.Jsoup
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import java.io.File
import java.net.URI
import java.util.logging.Logger


private val logger: Logger = Logger.getLogger("CourseParser")

abstract class CourseParser(val semesterId: String){
    abstract fun parseCourse(document: String):List<Course>
}

private fun CourseParser.parseActivities(scriptString: String): List<List<MetaCourse>> {
    val context = Context.enter()
    val scope: Scriptable = context.initStandardObjects()
    //resources/TaskActivity_New.js
    val activityUrl = this::class.java.getResource("/TaskActivity_New.js")
    val reader = File(URI(activityUrl!!.toString())).bufferedReader()
    context.evaluateReader(scope, reader, "TaskActivity", 1, null)

    val underscoreUrl = this::class.java.getResource("/underscore.min.js")
    val underscoreReader = File(URI(underscoreUrl!!.toString())).bufferedReader()
    context.evaluateReader(scope, underscoreReader, "underscore", 1, null)

    context.evaluateString(scope, scriptString, "script", 1, null)

    val result = context.evaluateString(scope, "JSON.stringify(table0.activities)", "script", 1, null)

    val typeRef = object : TypeReference<List<List<MetaCourse>>>() {}
    /*List<List<MetaCourse>>*/
    val json = JSON.parseObject(Context.toString(result), typeRef)

    return json
}


fun CourseParser.convertMeta2Course(metas: List<List<MetaCourse>>):List<Course>{
    val courses = mutableListOf<Course>()
    metas.forEachIndexed { times, metaCourses ->
        metaCourses.forEach { metaCourse ->
            val lessonId = metaCourse.courseId.split("(")[0].toInt()
            val courseId = metaCourse.courseId.split("(")[1].replace(")","")
            val courseName = metaCourse.courseName.let {
                //replace (***)
                Regex("""\([^)]+\)""").replace(it,"")
            }
            val teachers = mutableListOf<Teacher>().apply {
                metaCourse.teacherId.split(",").forEach {
                    add(Teacher(it.toInt()))
                }
            }
            val room = Room(metaCourse.roomId.toInt(), metaCourse.roomName)
            val validWeeks = metaCourse.vaildWeeks

            val coursePeriod = CoursePeriod(
                time = (times + 12) % 84,
                room = room,
                validWeeks = validWeeks.removePrefix("0")
            )
            courses.find { it.lessonId == lessonId && it.courseId == courseId }?.apply {
                this.coursePeriods += coursePeriod
                this.metaCourses += metaCourse
            } ?:run {
                courses += Course(
                    lessonId = lessonId,
                    courseId = courseId,
                    courseName = courseName,
                    teachers = teachers,
                    metaCourses = mutableListOf(metaCourse),
                    semesterId = semesterId,
                    coursePeriods = mutableListOf(coursePeriod),
                    category = null,
                    courseCode = null,
                    credit = null,
                    className = null
                )
            }
        }
    }
    logger.info("parsed courses: $courses")
    return courses
}

class StudentCourseParser(semesterId: String): CourseParser(semesterId){
    override fun parseCourse(document:String): List<Course> {
        val doc = Jsoup.parse(document)
        val script = doc.select("script").filter { it-> it.data().contains("var activity=null;")}[0]
        parseActivities(script.data())
        return emptyList()
    }


}

class TeacherCourseParser(
    private val teacher: Teacher,
    semesterId: String
): CourseParser(semesterId){
    override fun parseCourse(document: String): List<Course> {
        //匹配教师: 王青 [00000152]的课程表中的王青，00000152
        var (teacherName, teacherNo) =Regex("""教师:\s*([^\s\[]+)\s*\[\s*(\d+)\s*]""").find(document)!!.destructured
        teacherName = teacherName.replace("&nbsp;","").trim()
        teacherNo = teacherNo.replace("&nbsp;","").trim()
        teacher.teacherName = teacherName
        teacher.teacherNo = teacherNo
        logger.info("teacherName: $teacherName, teacherNo: $teacherNo")

        val doc = Jsoup.parse(document)
        val script = doc.select("script").filter { it-> it.data().contains("var activity=null;")}[0]
//        logger.info("script: $script")
        val metas = parseActivities(script.data())
        val parsedCourses = convertMeta2Course(metas)
        //#tasklesson > table:nth-child(9)
        val table = doc.select("#tasklesson > table:nth-child(9)")[0]
        val rows = table.select("tbody > tr")
        rows.drop(1).forEach { element ->
            val tds = element.select("td")
            val courseId = tds[1].text()
            val courseCode = tds[2].text()
            val courseName = tds[3].text().replace("(", "（").replace(")", "）")
            val category = tds[4].text()
            val className = tds[5].text().replace("班级:","")
            val credit = tds[7].text()

            parsedCourses.find { it.courseId == courseId }?.apply {
                this.courseCode = courseCode
                this.courseName = courseName
                this.category = category
                this.className = className
                this.credit = credit
            }

        }

        parsedCourses.forEach {
            println(it)
        }
        return parsedCourses
    }
}

fun List<Course>.filterByWeekAndPeriod(week: Int = -1, period: Int = -1):List<Course>{
    return this.filter { course ->
        course.coursePeriods.let { periods ->
            if(week == -1 && period == -1) return@let true
            if(week == -1) return@let periods.any { it.time == period }
            if(period == -1) return@let periods.any { it.validWeeks[week] == '1' }
            periods.any { it.time == period && it.validWeeks[week] == '1' }
        }
    }
}