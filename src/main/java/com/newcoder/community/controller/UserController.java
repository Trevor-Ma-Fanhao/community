package com.newcoder.community.controller;

import com.newcoder.community.annotation.LoginRequired;
import com.newcoder.community.entity.User;
import com.newcoder.community.service.LikeService;
import com.newcoder.community.service.UserService;
import com.newcoder.community.util.CommunityUtil;
import com.newcoder.community.util.HostHolder;
import jakarta.mail.Multipart;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${commmunity.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
        public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件的格式不正确！");
        }
        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败： " + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！",e);
        }
        // 更新当前用户头像的路径(Web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        }catch (IOException e){
            logger.error("读取头像失败：" + e.getMessage());
        }
    }
    //修改密码
    @RequestMapping(value = "/updatePassword",method = RequestMethod.POST)//@CookieValue("ticket") String ticket：从浏览器器中得到cookie
    public String updatePassword(String oldPassword, String newPassword, String confirmNewPassword, Model model,@CookieValue("ticket") String ticket){
        //对传入参数进行判断，不能为空
        if(StringUtils.isBlank(oldPassword)){
            model.addAttribute("oldPasswordMsg","请输入原始密码！");
            return "site/setting";
        }
        if(StringUtils.isBlank(newPassword)){
            model.addAttribute("newPasswordMsg","请输入新密码！");
            return "site/setting";
        }
        if(StringUtils.isBlank(confirmNewPassword)){
            model.addAttribute("confirmNewPasswordMsg","请输入确认密码！");
            return "site/setting";
        }
        //首先通过所持有的的用户对象获取当前用户
        User user = hostHolder.getUser();
        //判断用户输入的原密码是否与存储的原密码一致
        //首先对用户输入的原密码进行加密处理
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if(!oldPassword.equals(user.getPassword())){
            model.addAttribute("oldPasswordMsg","该密码与原密码不符!");
            return "site/setting";
        }
        //判断新输入密码与原密码是否一致
        //对新密码进行加密
        newPassword=CommunityUtil.md5(newPassword+user.getSalt());
        if(newPassword.equals(user.getPassword())){//判断
            model.addAttribute("newPasswordMsg","新密码与原密码一致!");
            return "site/setting";
        }
        //对确认密码进行加密
        confirmNewPassword=CommunityUtil.md5(confirmNewPassword+user.getSalt());
        if(!newPassword.equals(confirmNewPassword)){//判断
            model.addAttribute("confirmNewPasswordMsg","两次密码不一致!");
            return "site/setting";
        }
        userService.updatePassword(user.getId(),newPassword);
        //修改密码后，用户需要重新登陆，所以在本次持有中释放用户
        userService.logout(ticket);
        return "redirect:/login";
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        return "/site/profile";
    }



}
