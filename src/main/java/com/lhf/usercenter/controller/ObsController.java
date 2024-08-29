package com.lhf.usercenter.controller;

import com.lhf.usercenter.common.BaseResponse;
import com.lhf.usercenter.common.ErrorCode;
import com.lhf.usercenter.common.utils.ObsUtil;
import com.lhf.usercenter.common.utils.ResultUtil;
import com.lhf.usercenter.config.ObsConfig;
import com.lhf.usercenter.exception.BusinessException;
import com.lhf.usercenter.model.domain.User;
import com.lhf.usercenter.service.UserService;
import com.obs.services.model.PutObjectResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.lhf.usercenter.contant.UserContant.USER_LOGIN_STATUS;

@RestController
@RequestMapping("/obs")
public class ObsController {
    @Resource
    private UserService userService;
    @Resource
    private ObsUtil obsUtil;
    @Resource
    private ObsConfig obsConfig;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 文件上传
     *
     * @param uploadFile         上传的文件
     * @param httpServletRequest 请求
     * @return 文件路径
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> fileUpload(@RequestPart("uploadFile") MultipartFile uploadFile, HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null || uploadFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Object attribute = httpServletRequest.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) attribute;
        Long userId = user.getId();
        //将文件转换为md5,实现唯一性
//        String fileStr = FileUtil.MultipartFileToString(uploadFile);
//        String fileMd5 = SecureUtil.md5(fileStr);
        // 上传头像
        PutObjectResult putObjectResult = obsUtil.fileUpload(uploadFile, userId.toString());
        if (putObjectResult == null) {
            throw new BusinessException(ErrorCode.ERROR, "上传头像失败");
        } else {
            String url = String.format("%s%s", obsConfig.getUrlPrefix(), putObjectResult.getObjectKey());
            user.setUserAvatar(url);
            // 更新数据库
            userService.updateById(user);
            // 响应数据
            return ResultUtil.success(url);
        }
    }

    /**
     * 查询上传进度
     *
     * @param fileName
     */
/*
    @GetMapping("realFilePlan")
    public void redalFilePlan(String fileName) {
        FileUploadStatus fileUploadPlan = hweiOBSUtil.getFileUploadPlan(fileName);
        webSocketHandler.sendAllMessage(fileUploadPlan.getPct());
    }
*/

    /**
     * 下载文件
     *
     * @param request
     * @param response
     * @param fileName
     * @return
     */
    @GetMapping("fileDownload")
    public BaseResponse fileDownload(HttpServletRequest request, HttpServletResponse response, String fileName) {
        int i = obsUtil.fileDownload(request, response, fileName);
        if (i == 0) {
            return null;
        }
        return ResultUtil.error(ErrorCode.ERROR);
    }


}