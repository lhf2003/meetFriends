package com.lhf.usercenter.common.utils;

import com.lhf.usercenter.config.ObsConfig;
import com.obs.services.ObsClient;
import com.obs.services.model.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class ObsUtil {
    @Autowired
    private ObsConfig obsConfig;

    /**
     * 文件上传
     *
     * @param uploadFile 上传的文件
     * @param fileName   文件名称
     * @return 返回的路径
     */
    public PutObjectResult fileUpload(MultipartFile uploadFile, String fileName) {
        ObsClient obsClient = null;
        try {
            //创建实例
            obsClient = obsConfig.getInstance();
            //获取文件信息
            InputStream inputStream = uploadFile.getInputStream();
            String bucketName = obsConfig.getBucketName();
//            UploadFileRequest request1 = new UploadFileRequest(bucketName, fileName);
            long available = inputStream.available();
            String uploadFileName = uploadFile.getOriginalFilename();
            // 设置文件存储路径，存储路径为 avatars/{userId}/{fileName}
            String uploadFilePath = String.format("%s/%s", fileName, uploadFileName);
            PutObjectRequest request = new PutObjectRequest(bucketName, uploadFilePath, inputStream);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(available);
            request.setMetadata(objectMetadata);
            //设置公共读
            request.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            PutObjectResult putObjectResult = obsClient.putObject(request);
            return putObjectResult;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //销毁实例
            obsConfig.destroy(obsClient);
        }
        return null;
    }

    /**
     * 获取文件上传进度
     *
     * @param objectName
     * @return
     */
   /* public FileUploadStatus getFileUploadPlan(String objectName) {
        ObsClient obsClient = null;
        FileUploadStatus fileUploadStatus = new FileUploadStatus();
        try {
            obsClient = obsConfig.getInstance();
            GetObjectRequest request = new GetObjectRequest(obsConfig.getBucketName(), objectName);
            request.setProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(ProgressStatus status) {
                    //上传的平均速度
                    fileUploadStatus.setAvgSpeed(status.getAverageSpeed());
                    //上传的百分比
                    fileUploadStatus.setPct(String.valueOf(status.getTransferPercentage()));
                }
            });
            // 每下载1MB数据反馈下载进度
            request.setProgressInterval(1024 * 1024L);
            ObsObject obsObject = obsClient.getObject(request);
            // 读取对象内容
            InputStream input = obsObject.getObjectContent();
            byte[] b = new byte[1024];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len;
            while ((len = input.read(b)) != -1) {
                //将每一次的数据写入缓冲区
                byteArrayOutputStream.write(b, 0, len);
            }
            byteArrayOutputStream.close();
            input.close();
            return fileUploadStatus;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            obsConfig.destroy(obsClient);
        }
        return null;
    }*/

    /**
     * 文件下载
     *
     * @param request
     * @param response
     * @param fileName 文件名称
     * @return
     */
    public int fileDownload(HttpServletRequest request, HttpServletResponse response, String fileName) {
        try {
            ObsClient obsClient = obsConfig.getInstance();
            ObsObject obsObject = obsClient.getObject(obsConfig.getBucketName(), fileName);
            InputStream input = obsObject.getObjectContent();
            //缓冲文件输出流
            BufferedOutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            //设置让浏览器弹出下载提示框，而不是直接在浏览器中打开
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            IOUtils.copy(input, outputStream);
            outputStream.flush();
            outputStream.close();
            input.close();
            return 0;
        } catch (Exception e) {
            log.error("文件下载失败：{}", e.getMessage());
            return 1;
        }

    }
}