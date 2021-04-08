package com.szeastroc.gateway.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@Slf4j
@RestController
@RequestMapping("/system")
public class SystemController {


    @Resource
    private JedisClient jedisClient;


    @GetMapping("/heart")
    public CommonResponse<String> getSystemStatus() {
        String stringDate = DateTime.now().toString("yyyy-MM-dd");
        // 获取进入小程序次数
        String enterCount = jedisClient.get("pxt_cloud_xcx_sfa_enter_count_" + stringDate);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, enterCount);
    }
}
