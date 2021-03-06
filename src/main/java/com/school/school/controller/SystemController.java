package com.school.school.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.school.pojo.Admin;
import com.school.school.pojo.LoginForm;
import com.school.school.pojo.Student;
import com.school.school.pojo.Teacher;
import com.school.school.service.AdminService;
import com.school.school.service.StudentService;
import com.school.school.service.TeacherService;
import com.school.school.util.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/sms/system")
public class SystemController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private TeacherService teacherService;


    @GetMapping("/getInfo")
    public Result getInfoByToken(@RequestHeader("token") String token){
        boolean expiration = JwtHelper.isExpiration(token);
        if (expiration){
            return Result.build(null, ResultCodeEnum.TOKEN_ERROR);
        }
        Long userId=JwtHelper.getUserId(token);
        Integer userType=JwtHelper.getUserType(token);
        Map<String,Object> map=new LinkedHashMap<>();
        switch (userType){
            case 1:
                Admin admin=adminService.getAdminByid(userId);
                map.put("userType",1);
                map.put("user",admin);
                break;
            case 2:
                Student student=studentService.getstudentByid(userId);
                map.put("userType",2);
                map.put("user",student);
                break;
            case 3:
                Teacher teacher=teacherService.getteacherByid(userId);
                map.put("userType",3);
                map.put("user",teacher);
                break;
        }
        return Result.ok(map);
    }


    @PostMapping("/login")
    public Result login(@RequestBody LoginForm loginForm,HttpServletRequest request){
        System.out.println("?????????+"+loginForm.getUserType());
        HttpSession session = request.getSession();
        String sessionverifiCode= (String) session.getAttribute("verifiCode");
        String loginsession=loginForm.getVerifiCode();

        if ("".equals(sessionverifiCode) || null==sessionverifiCode){
            return Result.fail().message("???????????????");
        }
        if (!sessionverifiCode.equalsIgnoreCase(loginsession)){
            return Result.fail().message("???????????????");
        }
//??????seeeion
        session.removeAttribute("verifiCode");

        Map<String,Object> map=new LinkedHashMap<>();
        switch (loginForm.getUserType()){
            case 1:
                try {
                    Admin admin=adminService.login(loginForm);
                    if (null !=admin){
                        map.put("token",JwtHelper.createToken(admin.getId().longValue(),1));
                    }else {
                        throw new RuntimeException("???????????????????????????");
                    }
                 return Result.ok(map);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return Result.fail().message(e.getMessage());
                }
            case 2:
                try {
                    Student student= studentService.login(loginForm);

                    if (null !=student){
                        //                    String token= JwtHelper.createToken(admin.getId().longValue())
                        map.put("token",JwtHelper.createToken(student.getId().longValue(),2));
                    }else {
                        throw new RuntimeException("???????????????????????????");
                    }
                    return Result.ok(map);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return Result.fail().message(e.getMessage());
                }
            case 3:
                try {
                    Teacher teacher=teacherService.login(loginForm);
                    if (null !=teacher){
                        map.put("token",JwtHelper.createToken(teacher.getId().longValue(),3));
                    }else {
                        throw new RuntimeException("???????????????????????????");
                    }
                    return Result.ok(map);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return Result.fail().message(e.getMessage());
                }
        }
        return Result.fail().message("????????????");
    }

    @GetMapping("/getVerifiCodeImage")
     public void getVerifiCodeImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
//    ????????????
    BufferedImage verifiCodeImage = CreateVerifiCodeImage.getVerifiCodeImage();
//    ?????????????????????
    String verifiCode = new String(CreateVerifiCodeImage.getVerifiCode());
        System.out.println("??????????????????+"+verifiCode);
//    ???????????????seeeion???
    HttpSession session = request.getSession();
    session.setAttribute("verifiCode", verifiCode);
//    ??????????????????
ImageIO.write(verifiCodeImage,"JPEG",response.getOutputStream());
    }



    @ApiOperation("????????????????????????")
    @PostMapping("/headerImgUpload")
    public Result headerImgUpload(
            @ApiParam("?????????????????????") @RequestPart("multipartFile") MultipartFile multipartFile
    ){

        //??????UUID?????????????????????
        String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        //????????????????????????
        String filename = uuid.concat(multipartFile.getOriginalFilename());
        //???????????????????????????(???????????????????????????????????????????????????????????????)
        String portraitPath ="D:/??????/school/xm/target/classes/public/upload/".concat(filename);
        //????????????
        try {
            multipartFile.transferTo(new File(portraitPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String headerImg ="upload/"+filename;
        return Result.ok(headerImg);
    }

    @ApiOperation("????????????")
    @PostMapping("/updatePwd/{oldPwd}/{newPwd}")
    public Result updatePwd(@RequestHeader("token") String token,
                            @PathVariable("oldPwd") String oldPwd,
                            @PathVariable("newPwd") String newPwd){
        boolean yOn = JwtHelper.isExpiration(token);
        if(yOn){
            //token??????
            return Result.fail().message("token??????!");
        }
        //??????token???????????????????????????id
        Long userId = JwtHelper.getUserId(token);
        //??????token?????????????????????????????????
        Integer userType = JwtHelper.getUserType(token);
        // ??????????????????????????????
        oldPwd= MD5.encrypt(oldPwd);
        newPwd= MD5.encrypt(newPwd);
        if(userType == 1){
            QueryWrapper<Admin> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("id",userId.intValue()).eq("password",oldPwd);
            Admin admin = adminService.getOne(queryWrapper);
            if (null!=admin) {
                admin.setPassword(newPwd);
                adminService.saveOrUpdate(admin);
            }else{
                return Result.fail().message("????????????????????????");
            }
        }else if(userType == 2){
            QueryWrapper<Student> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("id",userId.intValue()).eq("password",oldPwd);
            Student student = studentService.getOne(queryWrapper);
            if (null!=student) {
                student.setPassword(newPwd);
                studentService.saveOrUpdate(student);
            }else{
                return Result.fail().message("????????????????????????");
            }
        }
        else if(userType == 3){
            QueryWrapper<Teacher> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("id",userId.intValue()).eq("password",oldPwd);
            Teacher teacher = teacherService.getOne(queryWrapper);
            if (null!=teacher) {
                teacher.setPassword(newPwd);
                teacherService.saveOrUpdate(teacher);
            }else{
                return Result.fail().message("????????????????????????");
            }
        }
        return Result.ok();
    }

}
