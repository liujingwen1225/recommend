package com.recommend.controller;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recommend.common.Result;
import com.recommend.controller.dto.PageQuery;
import com.recommend.entity.Course;
import com.recommend.entity.StudentCourse;
import com.recommend.entity.User;
import com.recommend.service.ICourseService;
import com.recommend.service.IStudentCourseService;
import com.recommend.service.IUserService;
import com.recommend.utils.TokenUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private ICourseService courseService;
    @Resource
    private IUserService userService;
    @Resource
    private IStudentCourseService studentCourseService;

    // 新增或者更新
    @PostMapping
    public Result save(@RequestBody Course course) {
        courseService.saveOrUpdate(course);
        return Result.success();
    }

    @ApiOperation("点击选课")
    @PostMapping("/studentCourse/{courseId}/{studentId}")
    public Result studentCourse(@PathVariable Integer courseId, @PathVariable Integer studentId) {
        courseService.setStudentCourse(courseId, studentId);
        return Result.success();
    }

    @ApiOperation("取消选课")
    @PostMapping("/cancelCourseSelection/{courseId}/{studentId}")
    public Result cancelCourseSelection(@PathVariable Integer courseId, @PathVariable Integer studentId) {
        courseService.cancelCourseSelection(courseId, studentId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        courseService.removeById(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        courseService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping
    public Result findAll() {
        return Result.success(courseService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(courseService.getById(id));
    }

    @ApiOperation("全部课程列表")
    @GetMapping("/page")
    public Result findPage(Course course, PageQuery pageQuery) {
        Page<Course> page = courseService.findPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), course);
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        List<Course> records = page.getRecords();
        for (Course record : records) {
            //选课状态：【1=未选，2=已选】
            Integer courseStatus = courseService.courseStatus(userId, record.getId());
            record.setCourseStatus(courseStatus);
        }
        return Result.success(page);
    }

    @ApiOperation("我的课程列表")
    @GetMapping("/myCourseList")
    public Result myCourseList(Course course, PageQuery pageQuery) {
        Page<Course> page = courseService.myCourseList(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), course);
        return Result.success(page);
    }

    @ApiOperation("首页课程推荐列表")
    @GetMapping("/indexCourse")
    public Result indexCourse() {
        List<Course> courseList = courseService.indexCourse();
        return Result.success(courseList);
    }

    @ApiOperation("课程类型列表")
    @GetMapping("/courseTypeList")
    public Result courseTypeList() {
        List<Course> courseList = courseService.courseTypeList();
        return Result.success(courseList);
    }

    @ApiOperation("课程类型列表")
    @GetMapping("/schoolTypeList")
    public Result schoolTypeList() {
        List<Course> courseList = courseService.schoolTypeList();
        return Result.success(courseList);
    }

    @ApiOperation("用户类型：【1=新用户，2=老用户】")
    @GetMapping("/userType")
    public Result userType() {
        HashMap<Object, Object> res = new HashMap<>(8);
        Integer userType = courseService.userType();
        res.put("userType", userType);
        return Result.success(res);
    }

    @ApiOperation("保存课程类型")
    @PostMapping("/saveCourseType")
    public Result saveCourseType(@RequestBody Course course) {
        //用户id
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        LambdaUpdateWrapper<User> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(User::getId, userId)
                .set(User::getCourseType, course.getTypeList())
                .set(User::getSchoolNames, course.getSchool())
                .set(User::getLabels, course.getTypeList());
        userService.update(null, lambdaUpdateWrapper);
        return Result.success();
    }

    @ApiOperation("我的评分")
    @PostMapping("/myRating")
    public Result myRating(@RequestBody StudentCourse bo) {
        //用户id
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        LambdaUpdateWrapper<StudentCourse> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(StudentCourse::getStudentId, userId).eq(StudentCourse::getCourseId, bo.getCourseId()).set(StudentCourse::getRating, bo.getRating());
        studentCourseService.update(null, lambdaUpdateWrapper);
        return Result.success();
    }


    @PostMapping("/update")
    public Result update(@RequestBody Course course) {
        courseService.saveOrUpdate(course);
        return Result.success();
    }

}

