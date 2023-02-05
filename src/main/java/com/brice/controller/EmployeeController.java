package com.brice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.brice.common.R;
import com.brice.entity.Employee;
import com.brice.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //1.将密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.根据用户名查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        // 3.如果没有该用户登陆失败
        if (emp == null) {
            return R.error("未找到相应的用户名");
        }

        //4.将密码与数据库比对,不一致登陆失败
        if (!emp.getPassword().equals(password)) {
            return R.error("登陆失败！");
        }

        //5.判断该用户是否被禁用，被禁用，登陆失败
        if (emp.getStatus() == 0) {
            return R.error("该用户已被禁用");
        }

        // 6.没有禁用，将用户id存入session,登陆成功
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 添加员工
     */
    @PostMapping
    public R<String> save(@RequestBody Employee employee) {
        log.info("新增员工");
        //设置初始没密码123456，进行MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        /*设置创建时间和更新事件
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        设置创建人和更新人
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);*/

        employeeService.save(employee);
        return R.success("添加员工成功");
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {
        //添加分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);
        //添加条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getUsername, name);
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //添加分页条件
        employeeService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 修改员工信息
     */
    @PutMapping
    public R<String> update(@RequestBody Employee employee) {
        /*更新修改员工的信息
        Long id = (Long) request.getSession().getAttribute("employee");

        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(id);*/

        employeeService.updateById(employee);
        return R.success("修改成功");
    }

    /**
     * 根据id查询员工
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee);
        }
        return R.error("没有找到此用户");
    }
}
