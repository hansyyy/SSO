package com.example.SSO.service.impl;

import com.example.SSO.constant.ConstantKit;
import com.example.SSO.dao.UserDao;
import com.example.SSO.domain.dto.UserDto;
import com.example.SSO.domain.entity.User;
import com.example.SSO.domain.entity.userDirection;
import com.example.SSO.domain.po.UserPo;
import com.example.SSO.service.UserService;
import com.example.SSO.util.TokenUtil;
import com.example.SSO.util.VerifyUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.net.*;


/**
 * @Author HanSiyue
 * @Date 2019/9/18 下午3:39
 */
@Service
public class UserServiceImpl implements UserService {

    @Value("${spring.mail.username}")
    private String from;
    @Resource
    private UserDao userDao;
    @Autowired
    private JavaMailSender javaMailSender;

    //绝对地址
    public static final String ABSOLUTE_PATH=":8081/Project/img/";


    @Autowired
    Configuration configuration;

    @Override
    public User login(HttpServletRequest request, UserDto userDto) {
        System.out.println(userDto);
        if (userDto.getStudentId() == null || userDto.getPassword() == null || userDto
                .getVerifyCode() == null) {
            return null;
        } else {
            User user = userDao.login(userDto.getStudentId(), userDto.getPassword());
            if (user != null) {
                Jedis jedis = new Jedis("127.0.0.1", 6379);
                //生成token
                String token = TokenUtil.generateToken(userDto.getStudentId().toString(), userDto.getPassword());
                request.getSession(false).setAttribute("token", token);
                request.getSession(false).setAttribute("studentId",user.getStudentId());
                jedis.set(userDto.getStudentId().toString(), token);
                //设置key生存时间，当key过期时，它会被自动删除，时间是秒
                jedis.expire(userDto.getStudentId().toString(), ConstantKit.TOKEN_EXPIRE_TIME);
                jedis.set(token, userDto.getStudentId().toString());
                jedis.expire(token, ConstantKit.TOKEN_EXPIRE_TIME);
                Long currentTime = System.currentTimeMillis();
                jedis.set(token + userDto.getStudentId(), currentTime.toString());
                //用完关闭
                jedis.close();
                return user;
            } else {
                System.out.println("-----");
                return null;
            }
        }
    }

    @Override
    public User login2(HttpServletRequest request, String password, Integer studentId) {
        if (password == null || studentId == null) {
            return null;
        } else {
            User user = userDao.login(studentId,password);
            if (user != null) {
                Jedis jedis = new Jedis("127.0.0.1", 6379);
                //生成token
                String token = TokenUtil.generateToken(studentId.toString(), password);
                request.getSession(false).setAttribute("token", token);
                request.getSession(false).setAttribute("studentId",user.getStudentId());
                jedis.set(studentId.toString(), token);
                //设置key生存时间，当key过期时，它会被自动删除，时间是秒
                jedis.expire(studentId.toString(), ConstantKit.TOKEN_EXPIRE_TIME);
                jedis.set(token, studentId.toString());
                jedis.expire(token, ConstantKit.TOKEN_EXPIRE_TIME);
                Long currentTime = System.currentTimeMillis();
                jedis.set(token + studentId, currentTime.toString());
                //用完关闭
                jedis.close();
                return user;
            } else {
                return null;
            }
        }
    }

    @Override
    public Boolean addUser(String userName, String password, Integer studentId, String mail, String major, List<Integer> directions) {
        if (userDao.selectUserByStudentId(studentId)!=null){
            return false;
        }else {
            Integer identifier = 3;
            String headUrl = "public.png";
            Boolean result1 = userDao.addUser(userName, password, studentId,mail,major,identifier,headUrl);
            List<userDirection> list = new ArrayList<>();
            for(Integer i:directions){
                list.add(new userDirection(studentId,i));
            }
            Boolean result2 = userDao.insertDirection(list);
            return result1&&result2;
        }
    }

    @Override
    public User selectUserByStudentId(Integer studentId) {
        User user = userDao.selectUserByStudentId(studentId);
        if (user!=null){
            return user;
        }else {
            return null;
        }
    }

    @Override
    public Boolean sendMail(HttpServletRequest request) {
        try{
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
            Integer studentId = (Integer)request.getSession().getAttribute("studentId");
            User user = userDao.selectUserByStudentId(studentId);
            Object[] objects = VerifyUtil.createImage();
            request.getSession().setAttribute("mailVerifyCode",objects[0]);
            helper.setFrom(from);
            helper.setTo(user.getMail());
            helper.setSubject("验证码");
            String content = "<html>\n"+
                    "<body>" +
                    "<h1 style=\"color: black\">亲爱的"+user.getUserName()+"同学，你好!</h1><p style=\"color: black\">这是你用于修改密码的验证码：</p><br>"+"<h1 style=\"color: MediumPurple\" align=\"center\">"+objects[0]+"</h1>"+
                    "</body>\n"+
                    "</html>";
            /*
            Map<String, Object> map = new HashMap();
            map.put("UserName", user.getUserName());
            map.put("captcha",objects[0]);

            Template template = configuration.getTemplate("mailTemplate.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template,map);
             */
            helper.setText(content,true);
            javaMailSender.send(mimeMessage);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public Object verifycode(HttpServletRequest request, HttpServletResponse response) {
        try {
            Object[] objects = VerifyUtil.createImage();
            request.getSession().setAttribute("verifyCode", objects[0]);
            System.out.println("验证码sessionid---"+request.getSession().getId());
            BufferedImage image = (BufferedImage) objects[1];
            response.setContentType("image/png");
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
            os.close();
            System.out.println(objects[0]);
            return objects[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Boolean updatePassword(HttpServletRequest request, String password,String mailVerifyCode) {
        Integer studentId = (Integer) request.getSession().getAttribute("studentId");
        if (mailVerifyCode.equals(request.getSession().getAttribute("mailVerifyCode")) && userDao.selectUserByStudentId(studentId)!=null){
            return userDao.updatePassword(studentId,password);
        }else {
            return false;
        }
    }

    @Override
    public Boolean updateInfo(HttpServletRequest request, String major, String userName, String headUrl, List<Integer> directions) {
        Integer studentId = (Integer) request.getSession().getAttribute("studentId");
        if (userDao.selectUserByStudentId(studentId)!=null){
            Boolean result1 = userDao.updateInfo(studentId,major,userName,headUrl);
            Boolean result2 = userDao.deleteDirection(studentId);
            List<userDirection> list = new ArrayList<>();
            for(Integer i:directions){
                list.add(new userDirection(studentId,i));
            }
            Boolean result3 = userDao.insertDirection(list);
            return result1&&result2&&result3;
        }else {
            return false;
        }
    }

    @Override
    public UserPo displayInfo(HttpServletRequest request) {
        Integer studentId = (Integer) request.getSession().getAttribute("studentId");
        User user = userDao.selectUserByStudentId(studentId);
        try {
            //LocalHostIP Address
            InetAddress ia= InetAddress.getLocalHost();
            String ip = ia.getHostAddress();
            String path = "http://"+ip+ABSOLUTE_PATH;
            if (user!=null){
                user.setDirections(userDao.displayDirection(studentId));
                UserPo userPo = new UserPo();
                userPo.setHeadUrl(path+user.getHeadUrl());
                userPo.setStudentId(user.getStudentId());
                userPo.setDirections(user.getDirections());
                userPo.setUserName(user.getUserName());
                userPo.setMajor(user.getMajor());
                return userPo;
            }else {
                return null;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
}
