package com.lhf.usercenter.common.utils;
 
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
 
@Slf4j
public class FileUtil {
    /**
     * MultipartFile转成String
     * @param file
     * @return
     */
    public static String MultipartFileToString(MultipartFile file){
        InputStreamReader isr;
        BufferedReader br;
        StringBuilder txtResult = new StringBuilder();
        try {
            isr = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String lineTxt;
            while ((lineTxt = br.readLine()) != null) {
                txtResult.append(lineTxt);
            }
            isr.close();
            br.close();
            return txtResult.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}