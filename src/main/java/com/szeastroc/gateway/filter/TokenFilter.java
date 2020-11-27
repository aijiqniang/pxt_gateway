package com.szeastroc.gateway.filter;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.szeastroc.common.constant.CookieConstant;
import com.szeastroc.common.constant.RedisConstant;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SessionMenuInfoVo;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Created by tulane
 */
@Slf4j
@Component
public class TokenFilter extends ZuulFilter {

    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private FeignCacheClient feignCacheClient;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        String key = "pxt_cloud_new_cache_menuinfo_gateway";
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String json = jedisClient.get(key);
        List<String> pageUrls = new ArrayList<>();
        if (StringUtils.isNotBlank(json)) {
            pageUrls = JSON.parseArray(json, String.class);
        } else {
            jedisClient.del(key);
            CommonResponse<List<SessionMenuInfoVo>> listForMenumInfoVo = feignCacheClient.getListForMenumInfoVo();
            List<SessionMenuInfoVo> menuInfoVos = listForMenumInfoVo.getData();
            pageUrls = menuInfoVos.stream().map(SessionMenuInfoVo::getPageUrl).collect(Collectors.toList());
            jedisClient.set(key, JSON.toJSONString(pageUrls), 300, TimeUnit.SECONDS);
        }
        if (CollectionUtil.isNotEmpty(pageUrls) && pageUrls.contains(request.getRequestURI())) {
            return true;
        }
        return false;
    }

    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();

        String token = request.getHeader(CookieConstant.MANAGE_TOKEN);

        /**
         * 验证cookie中token是否存在
         */
        if (StringUtils.isNotBlank(token)) {

            String json = jedisClient.get(String.format(RedisConstant.MANAGE_TOKEN_TEMPLATE, token));

            /**
             * 验证token是否有对应redis的值
             */
            if (StringUtils.isNotBlank(json)) {
                UserManageVo userManageVo = JSON.parseObject(json, UserManageVo.class);

                log.info("数据UserManageVo -> [{}]", JSON.toJSON(userManageVo));
                /**
                 * 验证所含菜单是否有当前请求所需资源
                 */
                if (userManageVo != null && CollectionUtil.isNotEmpty(userManageVo.getSessionMenuInfoVos())) {

                    List<SessionMenuInfoVo> menuInfoVos = userManageVo.getSessionMenuInfoVos();
                    List<String> pageUrls = menuInfoVos.stream().map(SessionMenuInfoVo::getPageUrl).collect(Collectors.toList());

                    if (pageUrls.contains(request.getRequestURI())) {
                        return null;
                    }
                }
            }
        }

        requestContext.setSendZuulResponse(false);
        requestContext.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        return null;
    }
}
