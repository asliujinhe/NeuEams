package org.jetos.assets

const val TaskActivity_New = """
    var delimiter = "<br>"
var weekCycle = [];
weekCycle[1] = "";// "%u8FDE";
weekCycle[2] = "%u5355";
weekCycle[3] = "%u53CC";
var weekCycle_en = [];
weekCycle_en[1] = "";
weekCycle_en[2] = "Odd";
weekCycle_en[3] = "Even";
var result = new String("");
var weeksPerYear = 53;

// 输出教学活动信息
function activityInfo() {
    return "teacherId:" + this.teacherId + "\n"
        + "teacherName:" + this.teacherName + "\n"
        + "courseId:" + this.courseId + "\n"
        + "courseName:" + this.courseName + "\n"
        + "roomId:" + this.roomId + "\n"
        + "roomName:" + this.roomName + "\n"
        + "vaildWeeks:" + this.vaildWeeks;
}

/**
 * 判断是否相同的活动 same acitivity [teacherId,courseId,roomId,vaildWeeks]
 */
function isSameActivity(other) {
    return this.canMergeWith(other) && (this.vaildWeeks == other.vaildWeeks);
}

/**
 * 合并相同的教学活动 same [teacherId,courseId,roomId,remark] can merge
 */
function canMergeWith(other) {
    return (
        this.teacherId == other.teacherId
        &&
        this.courseId == other.courseId
        &&
        this.roomId == other.roomId
        &&
        this.courseName == other.courseName
        &&
        this.courseHourTypeForSchGroup == other.courseHourTypeForSchGroup
        &&
        this.schGroupNo == other.schGroupNo
    );
}

// utility for repeat char
function repeatChar(str, length) {
    if (length <= 1) {
        return str;
    }
    var rs = "";
    for (var k = 0; k < length; k++) {
        rs += str;
    }
    return rs;
}

/**
 * 添加缩略表示 add a abbreviation to exists result; Do not use it directly. a
 * white space will delimate the weeks For example:odd1-18 even3-20
 */
function addAbbreviate(cycle, begin, end) {
    if (result !== "") {
        result += " ";
    }
    if (begin == end) { // only one week
        result += begin;
    } else {
        if (this.language && "en" == this.language) {
            result += begin + "-" + end + unescape(weekCycle_en[cycle]);
        } else {
            result += begin + "-" + end + unescape(weekCycle[cycle]);
        }
    }
    return result;
}

// 缩略单周,例如"10101010"
function mashalOdd(result, weekOccupy, from, start) {
    var cycle = 0;
    if ((start - from + 2) % 2 === 0) {
        cycle = 3;
    } else {
        cycle = 2;
    }
    var i = start + 2;
    for (; i < weekOccupy.length; i += 2) {
        if (weekOccupy.charAt(i) == '1') {
            if (weekOccupy.charAt(i + 1) == '1') {
                addAbbreviate(cycle, start - from + 2, i - 2 - from + 2);
                return i;
            }
        } else {
            if (i - 2 == start) {
                cycle = 1;
            }
            addAbbreviate(cycle, start - from + 2, i - 2 - from + 2);
            return i + 1;
        }
    }
    return i;
}

// 缩略连续周
function mashalContinue(result, weekOccupy, from, start) {
    var cycle = 1;
    var i = start + 2;
    for (; i < weekOccupy.length; i += 2) {
        if (weekOccupy.charAt(i) == '1') {
            if (weekOccupy.charAt(i + 1) != '1') {
                addAbbreviate(cycle, start - from + 2, i - from + 2);
                return i + 2;
            }
        } else {
            addAbbreviate(cycle, start - from + 2, i - 1 - from + 2);
            return i + 1;
        }
    }
    return i;
}

/**
 * 对教学周占用串进行缩略表示 marsh a string contain only '0' or '1' which named
 * "vaildWeeks" with length 53
 * 00000000001111111111111111100101010101010101010100000 |
 * |--------------------------------------| (from) (startWeek) (endWeek)
 * from is start position with minimal value 1,in login it's calendar week
 * start startWeek is what's start position you want to mashal baseed on
 * start,it also has minimal value 1 endWeek is what's end position you want
 * to mashal baseed on start,it also has minimal value 1
 */
function marshal(weekOccupy, from, startWeek, endWeek) {
    result = "";
    if (null == weekOccupy) {
        return "";
    }
    var initLength = weekOccupy.length;

    if (from > 1) {
        var before = weekOccupy.substring(0, from - 1);
        if (before.indexOf('1') != -1) {
            weekOccupy = weekOccupy + before;
        }
    }
    var tmpOccupy = repeatChar("0", from + startWeek - 2);
    tmpOccupy += weekOccupy.substring(from + startWeek - 2, from + endWeek - 1);
    tmpOccupy += repeatChar("0", initLength - weekOccupy.length);
    weekOccupy = tmpOccupy;

    if (endWeek > weekOccupy.length) {
        endWeek = weekOccupy.length;
    }
    if (weekOccupy.indexOf('1') == -1) {
        return "";
    }
    weekOccupy += "000";
    var start = 0;
    while ('1' != weekOccupy.charAt(start)) {
        start++;
    }
    var i = start + 1;
    while (i < weekOccupy.length) {
        var post = weekOccupy.charAt(start + 1);
        if (post == '0') {
            start = mashalOdd(result, weekOccupy, from, start);
        }
        if (post == '1') {
            start = mashalContinue(result, weekOccupy, from, start);
        }
        while (start < weekOccupy.length && '1' != weekOccupy.charAt(start)) {
            start++;
        }
        i = start;
    }
    return result;
}

/**
 * mashal style is or --------------------------- -------------------- |
 * odd3-18 even19-24,room | | odd3-18 | --------------------------
 * --------------------
 */
function marshalValidWeeks(from, startWeek, endWeek) {
    // alert(this.vaildWeeks);
    var result = "";
    if (this.roomName !== "") {
        result = marshal(this.vaildWeeks, from, startWeek, endWeek) + "," + this.roomName;
    } else {
        result = marshal(this.vaildWeeks, from, startWeek, endWeek);
    }
    if (this.schGroupNo != '') {
        result = result + " " + this.courseHourTypeForSchGroup + "组" + this.schGroupNo;
    }
    return result;
}

function or(first, second) {
    var newStr = "";
    for (var i = 0; i < first.length; i++) {
        if (first.charAt(i) == '1' || second.charAt(i) == '1') {
            newStr += "1";
        } else {
            newStr += "0";
        }
    }
    // alert(first+":first\n"+second+":second\n"+newStr+":result");
    return newStr;
}

// merger activity in every unit.
function mergeAll() {
    for (var i = 0; i < this.unitCounts; i++) {
        if (this.activities[i].length > 1) {
            for (var j = 1; j < this.activities[i].length; j++) {
                this.activities[i][0].vaildWeeks = or(this.activities[i][0].vaildWeeks, this.activities[i][j].vaildWeeks);
                this.activities[i][j] = null;
            }
        }
    }
}

// merger activity in every unit by course.
function mergeByCourse() {
    for (var i = 0; i < this.unitCounts; i++) {
        if (this.activities[i].length > 1) {
            // O(n^2)
            for (var j = 0; j < this.activities[i].length; j++) {
                if (null != this.activities[i][j]) {
                    for (var k = j + 1; j < this.activities[i].length; k++) {
                        if (null != this.activities[i][k]) {
                            if (this.activities[i][j].courseName == this.activities[i][k].courseName) {
                                this.activities[i][j].vaildWeeks = or(this.activities[i][j].vaildWeeks, this.activities[i][k].vaildWeeks);
                                this.activities[i][k] = null;
                            }
                        }
                    }
                }
            }
        }
    }
}

function isTimeConflictWith(otherTable) {
    for (var i = 0; i < this.unitCounts; i++) {
        if (this.activities[i].length !== 0 && otherTable.activities[i].length !== 0) {
            for (var m = 0; m < this.activities[i].length; m++) {
                for (var n = 0; n < otherTable.activities[i].length; n++) {
                    for (var k = 0; k < this.activities[i][m].vaildWeeks.length; k++) {
                        if (this.activities[i][m].vaildWeeks.charAt(k) == '1'
                            && otherTable.activities[i][n].vaildWeeks.charAt(k) == '1') {
                            return true;
                        }
                    }
                }
            }
        }
    }
    return false;
}

/**
 * aggreagate activity of same course. first merge the activity of same
 * (teacher,course,room). then output mashal vaildWeek string . if course is
 * null. the course name will be ommited in last string. style is
 * -------------------------------- | teacher1Name course1Name | |
 * (odd1-2,room1Name) | | (even2-4,room2Name) | | teacher2Name course1Name | |
 * (odd3-6,room1Name) | | (even5-8,room2Name) |
 * ----------------------------------
 *
 * @param index
 *            time unit index
 * @param from
 *            start position in year occupy week
 * @param startWeek
 *            bengin position from [from]
 * @param endWeek
 *            end position from [from]
 */
function marshalByTeacherCourse(index, from, startWeek, endWeek) {
    var validStart = from + startWeek - 2;
    if (this.activities[index].length === 0) {
        return "";
    }
    if (this.activities[index].length == 1) {
        var cname = this.activities[index][0].courseName;
        var tname = "";
        if (this.activities[index][0].teacherName != "") {
            tname += "(" + this.activities[index][0].teacherName + ")";
        }
        var assistantName = "";
        if (this.activities[index][0].assistantName != "") {
            assistantName += "(" + this.activities[index][0].assistantName + ")";
        }
        var experiItemName = "";
        if (this.activities[index][0].experiItemName != "") {
            experiItemName += "(" + this.activities[index][0].experiItemName + ")";
        }
        return cname + " " + tname + delimiter + assistantName + delimiter + experiItemName + delimiter + "(" + this.activities[index][0].adjustClone(this.endAtSat, validStart, false).marshal(from, startWeek, endWeek) + ")";
    } else {
        var marshalString = "";
        var tempActivities = new Array();
        tempActivities[0] = this.activities[index][0].adjustClone(this.endAtSat, validStart, true);
        // merge this.activities to tempActivities by same courseName and
        // roomId .start with 1.
        for (var i = 1; i < this.activities[index].length; i++) {
            var merged = false;
            for (var j = 0; j < tempActivities.length; j++) {
                if (this.activities[index][i].canMergeWith(tempActivities[j])) {
                    // alert(tempActivities[j]+"\n"
                    // +this.activities[index][i]);
                    merged = true;
                    var secondWeeks = this.activities[index][i].vaildWeeks;
                    if (this.activities[index][i].needLeftShift(this.endAtSat, validStart)) {
                        secondWeeks = this.activities[index][i].vaildWeeks.substring(1, 53) + "0";
                    }
                    tempActivities[j].vaildWeeks = or(tempActivities[j].vaildWeeks, secondWeeks);
                }
            }
            if (!merged) {
                tempActivities[tempActivities.length] = this.activities[index][i].adjustClone(this.endAtSat, validStart, false);
            }
        }

        // marshal tempActivities
        for (var m = 0; m < tempActivities.length; m++) {
            if (tempActivities[m] === null) {
                continue;
            }
            var courseName = tempActivities[m].courseName;
            var teacherName = "";
            if (tempActivities[m].teacherName != "") {
                teacherName += "(" + tempActivities[m].teacherName + ")";
            }
            var assistantName = "";
            if (tempActivities[m].assistantName != "") {
                assistantName += "(" + tempActivities[m].assistantName + ")";
            }
            var experiItemName = "";
            if (tempActivities[m].experiItemName != "") {
                experiItemName += "(" + tempActivities[m].experiItemName + ")";
            }
            // add teacherName and courseName
            if (courseName !== null) {
                marshalString += delimiter + courseName + " " + teacherName;/* alert(courseName); */
            }
            marshalString += delimiter + "(" + tempActivities[m].marshal(from, startWeek, endWeek);
            //截取前段
            var forward1 = tempActivities[m].marshal(from, startWeek, endWeek).split(".")[0];

            for (var n = m + 1; n < tempActivities.length; n++) {
                // marshal same courseName activity
                if (tempActivities[n] !== null && courseName == tempActivities[n].courseName && teacherName == tempActivities[n].teacherName && assistantName == tempActivities[n].assistantName && experiItemName == tempActivities[n].experiItemName) {
                    //marshalString += delimiter+"(" +tempActivities[n].marshal(from,startWeek,endWeek)+")";
                    var forward2 = tempActivities[n].marshal(from, startWeek, endWeek).split(".")[0]
                    var sequence = tempActivities[n].marshal(from, startWeek, endWeek).split(".")[1]
                    if (forward2 == forward1) {
                        /**
                         * BUG #69042 同一个班级 同一个课程存在多个任务，而且排课的时候这个任务 没有排老师也没有排地点,班级课表显示异常
                         * 没有安排地点，sequence是 undefined 拼接字符串出错
                         * 修改：添加对 sequence 的判断
                         */
                        if (undefined != sequence) {
                            marshalString += "/" + sequence;
                        }
                    } else {
                        marshalString += ")";
                        marshalString += delimiter + "(" + tempActivities[n].marshal(from, startWeek, endWeek);
                    }
                    forward1 = forward2;
                    tempActivities[n] = null;
                }
            }
            //看拼接过后最后一个是否有括号
            var aa = marshalString.substr(marshalString.length - 1, 1);
            if (aa != ")") {
                marshalString += ")";
            }
        }

        if (marshalString.indexOf(delimiter) === 0) {
            return marshalString.substring(delimiter.length);
        } else {
            return marshalString;
        }
    }
}

// return true,if this.activities[first] and this.activities[second] has
// same activities .
function isSameActivities(first, second) {
    if (this.activities[first].length != this.activities[second].length) {
        return false;
    }
    if (this.activities[first].length == 1) {
        return this.activities[first][0].isSame(this.activities[second][0]);
    }
    for (var i = 0; i < this.activities[first].length; i++) {
        var find = false;
        for (var j = 0; j < this.activities[second].length; j++) {
            if (this.activities[first][i].isSame(this.activities[second][j])) {
                find = true;
                break;
            }
        }
        if (find === false) {
            return false;
        }
    }
    return true;
}

/**
 * 检查是否需要进行左移动
 */
function needLeftShift(endAtSat, start) {
    return (!endAtSat && this.vaildWeeks.substring(0, start).indexOf("1") != -1 && this.vaildWeeks.substring(start).indexOf("1") == -1)
}

/**
 * 根据年份是否以周六结束,调整占用周. 如果在起始周之前有占用周,只有没有则表示可以进行调节.
 */
function leftShift() {
    this.vaildWeeks = this.vaildWeeks.substring(1, 53) + "0";
    // alert("leftShift:"+this.vaildWeeks);
}

/**
 * 根据该年份是否结束于星期六，调整教学州的占用串。 如果没有调整则返回原来的activity.否则返回调整后的新的activity。
 *
 * @activity 要调整的教学活动
 * @endAtStat 该活动的年份是否结束于星期六
 * @start 从何为止检查有效的教学周
 * @mustClone 是否必须克隆
 */
function adjustClone(endAtSat, start, mustClone) {
    if (mustClone) {
        var newActivity = this.clone();
        if (newActivity.needLeftShift(endAtSat, start)) {
            newActivity.leftShift();
        }
        return newActivity;
    } else {
        if (this.needLeftShift(endAtSat, start)) {
            var activity = this.clone();
            activity.leftShift(start);
            return activity;
        } else {
            return this;
        }
    }
}

// new taskAcitvity
function TaskActivity(teacherId, teacherName, courseId, courseName, roomId, roomName, vaildWeeks, taskId, remark, assistantName, experiItemName, schGroupNo, courseHourTypeForSchGroup, lessonId) {
    this.teacherId = teacherId;
    this.teacherName = teacherName;
    this.courseId = courseId;
    this.courseName = courseName;
    this.roomId = roomId;
    this.roomName = roomName;
    this.vaildWeeks = vaildWeeks;	// 53个01组成的字符串，代表了一年的53周
    this.taskId = taskId;
    this.marshal = marshalValidWeeks;
    this.addAbbreviate = addAbbreviate;
    this.clone = cloneTaskActivity;
    this.canMergeWith = canMergeWith;
    this.isSame = isSameActivity;
    this.toString = activityInfo;
    this.adjustClone = adjustClone;
    this.leftShift = leftShift;
    this.needLeftShift = needLeftShift;
    this.remark = remark;
    this.assistantName = assistantName;
    this.experiItemName = experiItemName;
    this.schGroupNo = schGroupNo;
    this.courseHourTypeForSchGroup = courseHourTypeForSchGroup;
    this.lessonId = lessonId;
}

// clone a activity
function cloneTaskActivity() {
    return new TaskActivity(this.teacherId, this.teacherName, this.courseId, this.courseName, this.roomId, this.roomName, this.vaildWeeks, this.taskId, this.remark, this.assistantName, this.experiItemName, this.schGroupNo, this.courseHourTypeForSchGroup, this.lessonId);
}

//
function marshalTable(from, startWeek, endWeek) {
    for (var k = 0; k < this.unitCounts; k++) {
        if (this.activities[k].length > 0) {
            this.marshalContents[k] = this.marshal(k, from, startWeek, endWeek);
        }
    }
}


function marshalTableForAdminclass(from, startWeek, endWeek) {
    for (var k = 0; k < this.unitCounts; k++) {
        if (this.activities[k].length > 0) {
            this.marshalContents[k] = this.marshalForAdminclass(k, from, startWeek, endWeek);
        }
    }
}

function marshalForAdminclass(index, from, startWeek, endWeek) {
    var validStart = from + startWeek - 2;
    if (this.activities[index].length === 0) {
        return "";
    }
    if (this.activities[index].length == 1) {
        var cname = this.activities[index][0].courseName;
        var tname = this.activities[index][0].teacherName;
        var roomOccupancy = "(" + this.activities[index][0].adjustClone(this.endAtSat, validStart, false).marshal(from, startWeek, endWeek) + ")";
        return tname + " " + cname + roomOccupancy;
    } else {
        var marshalString = "";
        var tempActivities = new Array();
        tempActivities[0] = this.activities[index][0].adjustClone(this.endAtSat, validStart, true);
        // merge this.activities to tempActivities by same courseName and
        // roomId .start with 1.
        for (var i = 1; i < this.activities[index].length; i++) {
            var merged = false;
            for (var j = 0; j < tempActivities.length; j++) {
                if (this.activities[index][i].canMergeWith(tempActivities[j])) {
                    // alert(tempActivities[j]+"\n"
                    // +this.activities[index][i]);
                    merged = true;
                    var secondWeeks = this.activities[index][i].vaildWeeks;
                    if (this.activities[index][i].needLeftShift(this.endAtSat, validStart)) {
                        secondWeeks = this.activities[index][i].vaildWeeks.substring(1, 53) + "0";
                    }
                    tempActivities[j].vaildWeeks = or(tempActivities[j].vaildWeeks, secondWeeks);
                }
            }
            if (!merged) {
                tempActivities[tempActivities.length] = this.activities[index][i].adjustClone(this.endAtSat, validStart, false);
            }
        }

        // marshal tempActivities
        for (var m = 0; m < tempActivities.length; m++) {
            if (tempActivities[m] === null) {
                continue;
            }
            var courseName = tempActivities[m].courseName;
            var teacherName = tempActivities[m].teacherName;
            // add teacherName and courseName
            var tipStr = "";
            if (courseName !== null) {
                tipStr = courseName + "(" + tempActivities[m].marshal(from, startWeek, endWeek) + ")";
            }
            if (marshalString.indexOf(tipStr) == -1) {
                marshalString += delimiter + tipStr;
            }
        }

        if (marshalString.indexOf(delimiter) === 0) {
            return marshalString.substring(delimiter.length);
        } else {
            return marshalString;
        }
    }
}

/*Banned*/
function fillTable(tableId, weeks, units) {}

function initCourseTableNew(x1,x2){}

/***************************************************************************
 * course table dispaly occupy of teacher,room and andminClass. It also
 * represent data model of any course arrangement. For example student's
 * course table,single course's table,teacher's course table,and
 * adminClass's course table,even major's .
 **************************************************************************/
function CourseTable(year, unitCounts, type) {
    this.unitCounts = unitCounts;
    this.activities = [unitCounts];
    this.type = type;
    this.year = year;
    var date = new Date();
    // 日期中的月份为该月份的数字减一
    date.setFullYear(year, 11, 31);
    this.endAtSat = false;
    if (6 == date.getDay()) {
        this.endAtSat = true;
    }

    this.marshalContents = new Array(unitCounts);
    for (var k = 0; k < unitCounts; k++) {
        this.activities[k] = [];
    }

    this.mergeAll = mergeAll;
    this.marshal = marshalByTeacherCourse;
    // return true,if this.activities[first] and this.activities[second] has
    // same vaildWeeks and roomId pair set.
    this.isSame = isSameActivities;
    this.isTimeConflictWith = isTimeConflictWith;
    this.marshalTable = marshalTable;
    this.marshalTableForAdminclass = marshalTableForAdminclass;
    this.fill = fillTable;
    this.marshalForAdminclass = marshalForAdminclass;
}

/***************************************************************************
 * 添加一个小节中的教学活动组成一个活动集. * *
 **************************************************************************/
// add acitity to cluster.and weekInex from 0 to weeks-1
function addActivityToCluster(teacherId, teacherName, roomId, roomName, weekIndex) {
    // alert("addActivityToCluster:"+weekIndex)
    if (null == this.weeksMap[teacherId + roomId]) {
        this.weeksMap[teacherId + roomId] = new Array(this.weeks);
        this.activityMap[teacherId + roomId] = new TaskActivity(teacherId, teacherName, this.courseId, this.courseName, roomId, roomName, "");
    }
    this.weeksMap[teacherId + roomId][weekIndex] = "1";
}

/**
 * 合并课程表中相同的单元格
 */
function mergeCellOfCourseTable(weeks, units) {
    for (var i = 0; i < weeks; i++) {
        for (var j = 0; j < units - 1; j++) {
            var index = units * i + j;
            var preTd = document.getElementById("TD" + index);
            var nextTd = document.getElementById("TD" + (index + 1));
            while (preTd.innerHTML !== "" && nextTd.innerHTML !== "" && preTd.innerHTML == nextTd.innerHTML) {
                preTd.parentNode.removeChild(nextTd);
                var spanNumber = new Number(preTd.colSpan);
                spanNumber++;
                preTd.colSpan = spanNumber;
                j++;
                if (j >= units - 1) {
                    break;
                }
                index = index + 1;
                nextTd = document.getElementById("TD" + (index + 1));
            }
        }
    }
}

/*
 * construct a valid Weeks from this.weeksMap by key teacherRoomId this
 * startweek is the position of this.weeksMap[teacherRoomId] in return
 * validWeekStr also it has mininal value 1;
 */
function constructValidWeeks(startWeek, teacherRoomId) {
    // alert("enter constructValidWeeks")
    // as many as possible weeks with in a year
    var firstWeeks = new Array(weeksPerYear);
    var secondWeeks = null;
    var weeksThisYear = "";
    for (var i = 0; i < weeksPerYear - 1; i++) {
        firstWeeks[i] = "0";
    }
    for (var weekIndex = 0; weekIndex < this.weeksMap[teacherRoomId].length; weekIndex++) {
        var occupy = "0";
        if (this.weeksMap[teacherRoomId][weekIndex] === undefined) occupy == "0";
        else occupy = "1";
        // 计算占用周的位置
        var weekIndexNum = new Number(weekIndex);
        weekIndexNum += startWeek - 1;

        if (weekIndexNum < weeksPerYear) {
            firstWeeks[weekIndexNum] = occupy;
        } else {
            if (null == secondWeeks) {
                // 生成下一年的占用情况
                secondWeeks = new Array();
                for (var i = 0; i < weeksPerYear - 1; i++) {
                    secondWeeks[i] = "0";
                }
            }
            secondWeeks[(weekIndexNum + (this.endAtSat ? 0 : 1)) % weeksPerYear] = occupy;
        }
    }
    for (i = 0; i < weeksPerYear; i++) {
        weeksThisYear += (firstWeeks[i] == null) ? "0" : firstWeeks[i];
    }
    // alert(weeksThisYear)
    var weekState = new Array();

    if (weeksThisYear.indexOf("1") != -1) {
        weekState[weekState.length] = weeksThisYear;
    }
    var weeksNextYear = "";
    if (null != secondWeeks) {
        for (i = 0; i < weeksPerYear; i++) {
            weeksNextYear += (secondWeeks[i] === undefined) ? "0" : secondWeeks[i];
        }
        if (weeksNextYear.indexOf("1") != -1) {
            weekState[weekState.length] = weeksNextYear;
        }
        // alert(weeksNextYear);
    }
    // alert(weekState)
    return weekState;
}

/**
 * 构造教学活动
 *
 */
function constructActivities(startWeek) {
    // alert("enter constructActivities")
    var activities = new Array();
    for (var teacherRoomId in this.activityMap) {
        var weekState = this.constructValidWeeks(startWeek, teacherRoomId);
        this.activityMap[teacherRoomId].vaildWeeks = weekState[0];
        this.activityMap[teacherRoomId].remark = this.remark;
        activities[activities.length] = this.activityMap[teacherRoomId];
        if (weekState.length == 2) {
            var cloned = this.activityMap[teacherRoomId].clone();
            cloned.vaildWeeks = weekState[1];
            activities[activities.length] = cloned;
            // alert(cloned)
        }
        // alert(this.activityMap[teacherRoomId]);
    }
    return activities;
}

/**
 * all activities in each unit consists a ActivityCluster
 */
function ActivityCluster(year, courseId, courseName, weeks, remark) {
    this.year = year;
    var date = new Date();
    date.setFullYear(year, 11, 31);
    this.endAtSat = false;
    if (6 == date.getDay()) {
        this.endAtSat = true;
    }
    this.courseId = courseId;
    this.courseName = courseName;
    this.weeks = weeks;
    this.remark = remark;
    this.weeksMap = {};
    this.activityMap = {};
    this.add = addActivityToCluster;
    this.constructValidWeeks = constructValidWeeks;
    this.genActivities = constructActivities;
}

"""

const val UnderScore = """
(function(){var n=this,t=n._,r={},e=Array.prototype,u=Object.prototype,i=Function.prototype,a=e.push,o=e.slice,c=e.concat,l=u.toString,f=u.hasOwnProperty,s=e.forEach,p=e.map,h=e.reduce,v=e.reduceRight,d=e.filter,g=e.every,m=e.some,y=e.indexOf,b=e.lastIndexOf,x=Array.isArray,_=Object.keys,j=i.bind,w=function(n){return n instanceof w?n:this instanceof w?(this._wrapped=n,void 0):new w(n)};"undefined"!=typeof exports?("undefined"!=typeof module&&module.exports&&(exports=module.exports=w),exports._=w):n._=w,w.VERSION="1.4.4";var A=w.each=w.forEach=function(n,t,e){if(null!=n)if(s&&n.forEach===s)n.forEach(t,e);else if(n.length===+n.length){for(var u=0,i=n.length;i>u;u++)if(t.call(e,n[u],u,n)===r)return}else for(var a in n)if(w.has(n,a)&&t.call(e,n[a],a,n)===r)return};w.map=w.collect=function(n,t,r){var e=[];return null==n?e:p&&n.map===p?n.map(t,r):(A(n,function(n,u,i){e[e.length]=t.call(r,n,u,i)}),e)};var O="Reduce of empty array with no initial value";w.reduce=w.foldl=w.inject=function(n,t,r,e){var u=arguments.length>2;if(null==n&&(n=[]),h&&n.reduce===h)return e&&(t=w.bind(t,e)),u?n.reduce(t,r):n.reduce(t);if(A(n,function(n,i,a){u?r=t.call(e,r,n,i,a):(r=n,u=!0)}),!u)throw new TypeError(O);return r},w.reduceRight=w.foldr=function(n,t,r,e){var u=arguments.length>2;if(null==n&&(n=[]),v&&n.reduceRight===v)return e&&(t=w.bind(t,e)),u?n.reduceRight(t,r):n.reduceRight(t);var i=n.length;if(i!==+i){var a=w.keys(n);i=a.length}if(A(n,function(o,c,l){c=a?a[--i]:--i,u?r=t.call(e,r,n[c],c,l):(r=n[c],u=!0)}),!u)throw new TypeError(O);return r},w.find=w.detect=function(n,t,r){var e;return E(n,function(n,u,i){return t.call(r,n,u,i)?(e=n,!0):void 0}),e},w.filter=w.select=function(n,t,r){var e=[];return null==n?e:d&&n.filter===d?n.filter(t,r):(A(n,function(n,u,i){t.call(r,n,u,i)&&(e[e.length]=n)}),e)},w.reject=function(n,t,r){return w.filter(n,function(n,e,u){return!t.call(r,n,e,u)},r)},w.every=w.all=function(n,t,e){t||(t=w.identity);var u=!0;return null==n?u:g&&n.every===g?n.every(t,e):(A(n,function(n,i,a){return(u=u&&t.call(e,n,i,a))?void 0:r}),!!u)};var E=w.some=w.any=function(n,t,e){t||(t=w.identity);var u=!1;return null==n?u:m&&n.some===m?n.some(t,e):(A(n,function(n,i,a){return u||(u=t.call(e,n,i,a))?r:void 0}),!!u)};w.contains=w.include=function(n,t){return null==n?!1:y&&n.indexOf===y?n.indexOf(t)!=-1:E(n,function(n){return n===t})},w.invoke=function(n,t){var r=o.call(arguments,2),e=w.isFunction(t);return w.map(n,function(n){return(e?t:n[t]).apply(n,r)})},w.pluck=function(n,t){return w.map(n,function(n){return n[t]})},w.where=function(n,t,r){return w.isEmpty(t)?r?null:[]:w[r?"find":"filter"](n,function(n){for(var r in t)if(t[r]!==n[r])return!1;return!0})},w.findWhere=function(n,t){return w.where(n,t,!0)},w.max=function(n,t,r){if(!t&&w.isArray(n)&&n[0]===+n[0]&&65535>n.length)return Math.max.apply(Math,n);if(!t&&w.isEmpty(n))return-1/0;var e={computed:-1/0,value:-1/0};return A(n,function(n,u,i){var a=t?t.call(r,n,u,i):n;a>=e.computed&&(e={value:n,computed:a})}),e.value},w.min=function(n,t,r){if(!t&&w.isArray(n)&&n[0]===+n[0]&&65535>n.length)return Math.min.apply(Math,n);if(!t&&w.isEmpty(n))return 1/0;var e={computed:1/0,value:1/0};return A(n,function(n,u,i){var a=t?t.call(r,n,u,i):n;e.computed>a&&(e={value:n,computed:a})}),e.value},w.shuffle=function(n){var t,r=0,e=[];return A(n,function(n){t=w.random(r++),e[r-1]=e[t],e[t]=n}),e};var k=function(n){return w.isFunction(n)?n:function(t){return t[n]}};w.sortBy=function(n,t,r){var e=k(t);return w.pluck(w.map(n,function(n,t,u){return{value:n,index:t,criteria:e.call(r,n,t,u)}}).sort(function(n,t){var r=n.criteria,e=t.criteria;if(r!==e){if(r>e||r===void 0)return 1;if(e>r||e===void 0)return-1}return n.index<t.index?-1:1}),"value")};var F=function(n,t,r,e){var u={},i=k(t||w.identity);return A(n,function(t,a){var o=i.call(r,t,a,n);e(u,o,t)}),u};w.groupBy=function(n,t,r){return F(n,t,r,function(n,t,r){(w.has(n,t)?n[t]:n[t]=[]).push(r)})},w.countBy=function(n,t,r){return F(n,t,r,function(n,t){w.has(n,t)||(n[t]=0),n[t]++})},w.sortedIndex=function(n,t,r,e){r=null==r?w.identity:k(r);for(var u=r.call(e,t),i=0,a=n.length;a>i;){var o=i+a>>>1;u>r.call(e,n[o])?i=o+1:a=o}return i},w.toArray=function(n){return n?w.isArray(n)?o.call(n):n.length===+n.length?w.map(n,w.identity):w.values(n):[]},w.size=function(n){return null==n?0:n.length===+n.length?n.length:w.keys(n).length},w.first=w.head=w.take=function(n,t,r){return null==n?void 0:null==t||r?n[0]:o.call(n,0,t)},w.initial=function(n,t,r){return o.call(n,0,n.length-(null==t||r?1:t))},w.last=function(n,t,r){return null==n?void 0:null==t||r?n[n.length-1]:o.call(n,Math.max(n.length-t,0))},w.rest=w.tail=w.drop=function(n,t,r){return o.call(n,null==t||r?1:t)},w.compact=function(n){return w.filter(n,w.identity)};var R=function(n,t,r){return A(n,function(n){w.isArray(n)?t?a.apply(r,n):R(n,t,r):r.push(n)}),r};w.flatten=function(n,t){return R(n,t,[])},w.without=function(n){return w.difference(n,o.call(arguments,1))},w.uniq=w.unique=function(n,t,r,e){w.isFunction(t)&&(e=r,r=t,t=!1);var u=r?w.map(n,r,e):n,i=[],a=[];return A(u,function(r,e){(t?e&&a[a.length-1]===r:w.contains(a,r))||(a.push(r),i.push(n[e]))}),i},w.union=function(){return w.uniq(c.apply(e,arguments))},w.intersection=function(n){var t=o.call(arguments,1);return w.filter(w.uniq(n),function(n){return w.every(t,function(t){return w.indexOf(t,n)>=0})})},w.difference=function(n){var t=c.apply(e,o.call(arguments,1));return w.filter(n,function(n){return!w.contains(t,n)})},w.zip=function(){for(var n=o.call(arguments),t=w.max(w.pluck(n,"length")),r=Array(t),e=0;t>e;e++)r[e]=w.pluck(n,""+e);return r},w.object=function(n,t){if(null==n)return{};for(var r={},e=0,u=n.length;u>e;e++)t?r[n[e]]=t[e]:r[n[e][0]]=n[e][1];return r},w.indexOf=function(n,t,r){if(null==n)return-1;var e=0,u=n.length;if(r){if("number"!=typeof r)return e=w.sortedIndex(n,t),n[e]===t?e:-1;e=0>r?Math.max(0,u+r):r}if(y&&n.indexOf===y)return n.indexOf(t,r);for(;u>e;e++)if(n[e]===t)return e;return-1},w.lastIndexOf=function(n,t,r){if(null==n)return-1;var e=null!=r;if(b&&n.lastIndexOf===b)return e?n.lastIndexOf(t,r):n.lastIndexOf(t);for(var u=e?r:n.length;u--;)if(n[u]===t)return u;return-1},w.range=function(n,t,r){1>=arguments.length&&(t=n||0,n=0),r=arguments[2]||1;for(var e=Math.max(Math.ceil((t-n)/r),0),u=0,i=Array(e);e>u;)i[u++]=n,n+=r;return i},w.bind=function(n,t){if(n.bind===j&&j)return j.apply(n,o.call(arguments,1));var r=o.call(arguments,2);return function(){return n.apply(t,r.concat(o.call(arguments)))}},w.partial=function(n){var t=o.call(arguments,1);return function(){return n.apply(this,t.concat(o.call(arguments)))}},w.bindAll=function(n){var t=o.call(arguments,1);return 0===t.length&&(t=w.functions(n)),A(t,function(t){n[t]=w.bind(n[t],n)}),n},w.memoize=function(n,t){var r={};return t||(t=w.identity),function(){var e=t.apply(this,arguments);return w.has(r,e)?r[e]:r[e]=n.apply(this,arguments)}},w.delay=function(n,t){var r=o.call(arguments,2);return setTimeout(function(){return n.apply(null,r)},t)},w.defer=function(n){return w.delay.apply(w,[n,1].concat(o.call(arguments,1)))},w.throttle=function(n,t){var r,e,u,i,a=0,o=function(){a=new Date,u=null,i=n.apply(r,e)};return function(){var c=new Date,l=t-(c-a);return r=this,e=arguments,0>=l?(clearTimeout(u),u=null,a=c,i=n.apply(r,e)):u||(u=setTimeout(o,l)),i}},w.debounce=function(n,t,r){var e,u;return function(){var i=this,a=arguments,o=function(){e=null,r||(u=n.apply(i,a))},c=r&&!e;return clearTimeout(e),e=setTimeout(o,t),c&&(u=n.apply(i,a)),u}},w.once=function(n){var t,r=!1;return function(){return r?t:(r=!0,t=n.apply(this,arguments),n=null,t)}},w.wrap=function(n,t){return function(){var r=[n];return a.apply(r,arguments),t.apply(this,r)}},w.compose=function(){var n=arguments;return function(){for(var t=arguments,r=n.length-1;r>=0;r--)t=[n[r].apply(this,t)];return t[0]}},w.after=function(n,t){return 0>=n?t():function(){return 1>--n?t.apply(this,arguments):void 0}},w.keys=_||function(n){if(n!==Object(n))throw new TypeError("Invalid object");var t=[];for(var r in n)w.has(n,r)&&(t[t.length]=r);return t},w.values=function(n){var t=[];for(var r in n)w.has(n,r)&&t.push(n[r]);return t},w.pairs=function(n){var t=[];for(var r in n)w.has(n,r)&&t.push([r,n[r]]);return t},w.invert=function(n){var t={};for(var r in n)w.has(n,r)&&(t[n[r]]=r);return t},w.functions=w.methods=function(n){var t=[];for(var r in n)w.isFunction(n[r])&&t.push(r);return t.sort()},w.extend=function(n){return A(o.call(arguments,1),function(t){if(t)for(var r in t)n[r]=t[r]}),n},w.pick=function(n){var t={},r=c.apply(e,o.call(arguments,1));return A(r,function(r){r in n&&(t[r]=n[r])}),t},w.omit=function(n){var t={},r=c.apply(e,o.call(arguments,1));for(var u in n)w.contains(r,u)||(t[u]=n[u]);return t},w.defaults=function(n){return A(o.call(arguments,1),function(t){if(t)for(var r in t)null==n[r]&&(n[r]=t[r])}),n},w.clone=function(n){return w.isObject(n)?w.isArray(n)?n.slice():w.extend({},n):n},w.tap=function(n,t){return t(n),n};var I=function(n,t,r,e){if(n===t)return 0!==n||1/n==1/t;if(null==n||null==t)return n===t;n instanceof w&&(n=n._wrapped),t instanceof w&&(t=t._wrapped);var u=l.call(n);if(u!=l.call(t))return!1;switch(u){case"[object String]":return n==t+"";case"[object Number]":return n!=+n?t!=+t:0==n?1/n==1/t:n==+t;case"[object Date]":case"[object Boolean]":return+n==+t;case"[object RegExp]":return n.source==t.source&&n.global==t.global&&n.multiline==t.multiline&&n.ignoreCase==t.ignoreCase}if("object"!=typeof n||"object"!=typeof t)return!1;for(var i=r.length;i--;)if(r[i]==n)return e[i]==t;r.push(n),e.push(t);var a=0,o=!0;if("[object Array]"==u){if(a=n.length,o=a==t.length)for(;a--&&(o=I(n[a],t[a],r,e)););}else{var c=n.constructor,f=t.constructor;if(c!==f&&!(w.isFunction(c)&&c instanceof c&&w.isFunction(f)&&f instanceof f))return!1;for(var s in n)if(w.has(n,s)&&(a++,!(o=w.has(t,s)&&I(n[s],t[s],r,e))))break;if(o){for(s in t)if(w.has(t,s)&&!a--)break;o=!a}}return r.pop(),e.pop(),o};w.isEqual=function(n,t){return I(n,t,[],[])},w.isEmpty=function(n){if(null==n)return!0;if(w.isArray(n)||w.isString(n))return 0===n.length;for(var t in n)if(w.has(n,t))return!1;return!0},w.isElement=function(n){return!(!n||1!==n.nodeType)},w.isArray=x||function(n){return"[object Array]"==l.call(n)},w.isObject=function(n){return n===Object(n)},A(["Arguments","Function","String","Number","Date","RegExp"],function(n){w["is"+n]=function(t){return l.call(t)=="[object "+n+"]"}}),w.isArguments(arguments)||(w.isArguments=function(n){return!(!n||!w.has(n,"callee"))}),"function"!=typeof/./&&(w.isFunction=function(n){return"function"==typeof n}),w.isFinite=function(n){return isFinite(n)&&!isNaN(parseFloat(n))},w.isNaN=function(n){return w.isNumber(n)&&n!=+n},w.isBoolean=function(n){return n===!0||n===!1||"[object Boolean]"==l.call(n)},w.isNull=function(n){return null===n},w.isUndefined=function(n){return n===void 0},w.has=function(n,t){return f.call(n,t)},w.noConflict=function(){return n._=t,this},w.identity=function(n){return n},w.times=function(n,t,r){for(var e=Array(n),u=0;n>u;u++)e[u]=t.call(r,u);return e},w.random=function(n,t){return null==t&&(t=n,n=0),n+Math.floor(Math.random()*(t-n+1))};var M={escape:{"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#x27;","/":"&#x2F;"}};M.unescape=w.invert(M.escape);var S={escape:RegExp("["+w.keys(M.escape).join("")+"]","g"),unescape:RegExp("("+w.keys(M.unescape).join("|")+")","g")};w.each(["escape","unescape"],function(n){w[n]=function(t){return null==t?"":(""+t).replace(S[n],function(t){return M[n][t]})}}),w.result=function(n,t){if(null==n)return null;var r=n[t];return w.isFunction(r)?r.call(n):r},w.mixin=function(n){A(w.functions(n),function(t){var r=w[t]=n[t];w.prototype[t]=function(){var n=[this._wrapped];return a.apply(n,arguments),D.call(this,r.apply(w,n))}})};var N=0;w.uniqueId=function(n){var t=++N+"";return n?n+t:t},w.templateSettings={evaluate:/<%([\s\S]+?)%>/g,interpolate:/<%=([\s\S]+?)%>/g,escape:/<%-([\s\S]+?)%>/g};var T=/(.)^/,q={"'":"'","\\":"\\","\r":"r","\n":"n","	":"t","\u2028":"u2028","\u2029":"u2029"},B=/\\|'|\r|\n|\t|\u2028|\u2029/g;w.template=function(n,t,r){var e;r=w.defaults({},r,w.templateSettings);var u=RegExp([(r.escape||T).source,(r.interpolate||T).source,(r.evaluate||T).source].join("|")+"|${'$'}","g"),i=0,a="__p+='";n.replace(u,function(t,r,e,u,o){return a+=n.slice(i,o).replace(B,function(n){return"\\"+q[n]}),r&&(a+="'+\n((__t=("+r+"))==null?'':_.escape(__t))+\n'"),e&&(a+="'+\n((__t=("+e+"))==null?'':__t)+\n'"),u&&(a+="';\n"+u+"\n__p+='"),i=o+t.length,t}),a+="';\n",r.variable||(a="with(obj||{}){\n"+a+"}\n"),a="var __t,__p='',__j=Array.prototype.join,"+"print=function(){__p+=__j.call(arguments,'');};\n"+a+"return __p;\n";try{e=Function(r.variable||"obj","_",a)}catch(o){throw o.source=a,o}if(t)return e(t,w);var c=function(n){return e.call(this,n,w)};return c.source="function("+(r.variable||"obj")+"){\n"+a+"}",c},w.chain=function(n){return w(n).chain()};var D=function(n){return this._chain?w(n).chain():n};w.mixin(w),A(["pop","push","reverse","shift","sort","splice","unshift"],function(n){var t=e[n];w.prototype[n]=function(){var r=this._wrapped;return t.apply(r,arguments),"shift"!=n&&"splice"!=n||0!==r.length||delete r[0],D.call(this,r)}}),A(["concat","join","slice"],function(n){var t=e[n];w.prototype[n]=function(){return D.call(this,t.apply(this._wrapped,arguments))}}),w.extend(w.prototype,{chain:function(){return this._chain=!0,this},value:function(){return this._wrapped}})}).call(this);
"""