package com.recommend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.recommend.controller.dto.ChartDataVo;
import com.recommend.entity.Course;
import com.recommend.entity.CourseRecommend;
import com.recommend.entity.StudentCourse;
import com.recommend.entity.User;
import com.recommend.mapper.CourseMapper;
import com.recommend.mapper.CourseRecommendMapper;
import com.recommend.mapper.StudentCourseMapper;
import com.recommend.mapper.UserMapper;
import com.recommend.service.ICourseService;
import com.recommend.utils.TokenUtils;
import io.swagger.annotations.ApiModelProperty;
import org.aspectj.weaver.ast.Var;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private StudentCourseMapper studentCourseMapper;
    @Resource
    private CourseRecommendMapper recommendMapper;

    /**
     * 找到页面
     *
     * @param page   页面
     * @param course 课程
     * @return {@link Page}<{@link Course}>
     */
    @Override
    public Page<Course> findPage(Page<Course> page, Course course) {
        //条件查询
        LambdaQueryWrapper<Course> lqw = buildQueryWrapper(course);
        return courseMapper.selectPage(page, lqw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStudentCourse(Integer courseId, Integer studentId) {
        courseMapper.deleteStudentCourse(courseId, studentId);
        courseMapper.setStudentCourse(courseId, studentId);
    }

    /**
     * 取消选课
     *
     * @param courseId  进程id
     * @param studentId 学生证
     */
    @Override
    public void cancelCourseSelection(Integer courseId, Integer studentId) {
        courseMapper.deleteStudentCourse(courseId, studentId);
    }

    /**
     * 首页课程推荐列表
     *
     * @return {@link List}<{@link Course}>
     */
    @Override
    public List<Course> indexCourse() {
        //不为空是新用户，否则是老用户
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        List<Course> courseList = new ArrayList<>();
        //查询推荐表是否有他数据
        CourseRecommend courseRecommend = recommendMapper.selectOne(new LambdaQueryWrapper<CourseRecommend>().eq(CourseRecommend::getUserId, userId).last("limit 1"));
        if (ObjectUtil.isNull(courseRecommend)) {
            //获取课程类型
            User user = userMapper.selectById(userId);
            String courseType = user.getCourseType();
            //string转list
            String cleanBlank = StrUtil.cleanBlank(courseType);
            List<String> typeList = Arrays.asList(cleanBlank.split(","));
            courseList = courseMapper.indexCourse(typeList);
        } else {
            List<CourseRecommend> recommendList = recommendMapper.selectList(new LambdaQueryWrapper<CourseRecommend>().eq(CourseRecommend::getUserId, userId));
            if (CollUtil.isNotEmpty(recommendList)) {
                List<Integer> collect = recommendList.stream().map(CourseRecommend::getCourseId).collect(Collectors.toList());
                courseList = courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, collect));
            }
        }
        //选课状态：【1=未选，2=已选】
        if (CollUtil.isNotEmpty(courseList)) {
            for (Course course : courseList) {
                Integer courseStatus = courseStatus(userId, course.getId());
                course.setCourseStatus(courseStatus);
            }
        }
        return courseList;
    }

    /**
     * 课程类型列表
     *
     * @return {@link List}<{@link Course}>
     */
    @Override
    public List<Course> courseTypeList() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>().select(Course::getType).ne(Course::getType, "").groupBy(Course::getType));
    }

    /**
     * 用户类型
     *
     * @return {@link Integer}
     */
    @Override
    public Integer userType() {
        //获取角色标识符，用户id
        String role = Objects.requireNonNull(TokenUtils.getCurrentUser()).getRole();
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        //用户类型：【1=新用户，2=老用户】
        int userType = 2;
        if (!StrUtil.equals(role, "ROLE_ADMIN")) {
            User user = userMapper.selectById(userId);
            String courseType = user.getCourseType();
            if (StrUtil.isBlank(courseType)) {
                userType = 1;
            }
        }
        return userType;
    }

    /**
     * 选课状态：【1=未选，2=已选】
     *
     * @return {@link Integer}
     */
    @Override
    public Integer courseStatus(Integer userId, Integer courseId) {
        //选课状态：【1=未选，2=已选】
        int courseStatus = 1;
        StudentCourse studentCourseServiceOne = studentCourseMapper.selectOne(new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, userId).eq(StudentCourse::getCourseId, courseId).last("limit 1"));
        if (ObjectUtil.isNotNull(studentCourseServiceOne)) {
            courseStatus = 2;
        }
        return courseStatus;
    }

    /**
     * 我课程列表
     *
     * @param page   页面
     * @param course 课程
     * @return {@link Page}<{@link Course}>
     */
    @Override
    public Page<Course> myCourseList(Page<Course> page, Course course) {
        //
        Page<Course> coursePage = new Page<>();
        //获取用户id
        Integer userId = Objects.requireNonNull(TokenUtils.getCurrentUser()).getId();
        //我的课程数据
        List<StudentCourse> selectList = studentCourseMapper.selectList(new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, userId));
        if (CollUtil.isNotEmpty(selectList)) {
            List<Integer> collect = selectList.stream().map(StudentCourse::getCourseId).collect(Collectors.toList());
            course.setCourseIds(collect);
            //条件查询
            LambdaQueryWrapper<Course> lqw = buildQueryWrapper(course);
            coursePage = courseMapper.selectPage(page, lqw);
        }
        return coursePage;
    }

    private LambdaQueryWrapper<Course> buildQueryWrapper(Course course) {
        LambdaQueryWrapper<Course> lqw = Wrappers.lambdaQuery();
        lqw.in(CollUtil.isNotEmpty(course.getCourseIds()), Course::getId, course.getCourseIds());
        lqw.like(StrUtil.isNotBlank(course.getInstructor()), Course::getInstructor, course.getInstructor());
        lqw.like(StrUtil.isNotBlank(course.getName()), Course::getName, course.getName());
        lqw.like(StrUtil.isNotBlank(course.getSchool()), Course::getSchool, course.getSchool());
        lqw.like(StrUtil.isNotBlank(course.getType()), Course::getType, course.getType());
        lqw.last("order by participants_number * 1 desc, grading * 1 desc");
        return lqw;
    }

    /**
     * 图表数据
     *
     * @return {@link ChartDataVo}
     */
    @Override
    public ChartDataVo chartData() {
        ChartDataVo chartDataVo = new ChartDataVo();
        //今年选课用户
        Long userNumber = courseMapper.userNumber();
        chartDataVo.setUserNumber(userNumber);
        //课程数量
        Long courseNumber = courseMapper.courseNumber();
        chartDataVo.setCourseNumber(courseNumber);
        //授课教师数量
        Long teacherNumber = courseMapper.teacherNumber();
        chartDataVo.setTeacherNumber(teacherNumber);
        //授课学校数量
        Long schoolNumber = courseMapper.schoolNumber();
        chartDataVo.setSchoolNumber(schoolNumber);
        //热门课程
        List<Course> topCourseList = courseMapper.topCourseList();
        List<String> popularCourseNameList = new ArrayList<>();
        List<Long> popularCourseNumberList = new ArrayList<>();
        if (CollUtil.isNotEmpty(topCourseList)) {
            popularCourseNameList = topCourseList.stream().map(Course::getName).collect(Collectors.toList());
            popularCourseNumberList = topCourseList.stream().map(Course::getNum).collect(Collectors.toList());
        }
        chartDataVo.setPopularCourseNameList(popularCourseNameList);
        chartDataVo.setPopularCourseNumberList(popularCourseNumberList);
        //热门学校
        List<Course> topSchoolList = courseMapper.topSchoolList();
        List<String> popularSchoolNameList = new ArrayList<>();
        List<Long> popularSchoolNumberList = new ArrayList<>();
        if (CollUtil.isNotEmpty(topSchoolList)) {
            popularSchoolNameList = topSchoolList.stream().map(Course::getSchool).collect(Collectors.toList());
            popularSchoolNumberList = topSchoolList.stream().map(Course::getNum).collect(Collectors.toList());
        }
        chartDataVo.setPopularSchoolNameList(popularSchoolNameList);
        chartDataVo.setPopularSchoolNumberList(popularSchoolNumberList);
        //热门老师
        List<Course> topTeacherList = courseMapper.topTeacherList();
        List<String> popularTeacherNameList = new ArrayList<>();
        List<Long> popularTeacherNumberList = new ArrayList<>();
        if (CollUtil.isNotEmpty(topTeacherList)) {
            popularTeacherNameList = topTeacherList.stream().map(Course::getInstructor).collect(Collectors.toList());
            popularTeacherNumberList = topTeacherList.stream().map(Course::getNum).collect(Collectors.toList());
        }
        chartDataVo.setPopularTeacherNameList(popularTeacherNameList);
        chartDataVo.setPopularTeacherNumberList(popularTeacherNumberList);
        return chartDataVo;
    }

}
