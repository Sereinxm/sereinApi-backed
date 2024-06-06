package com.serein.sereinApi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.serein.sereinApi.common.ErrorCode;
import com.serein.sereinApi.constant.CommonConstant;
import com.serein.sereinApi.exception.BusinessException;
import com.serein.sereinApi.exception.ThrowUtils;
import com.serein.sereinApi.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.serein.sereinApi.model.entity.*;
import com.serein.sereinApi.model.vo.UserVO;
import com.serein.sereinApi.service.InterfaceInfoService;
import com.serein.sereinApi.service.UserService;
import com.serein.sereinApi.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;
import com.serein.sereinApi.mapper.InterfaceInfoMapper;
import com.serein.sereinApi.model.vo.InterfaceInfoVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
* @author cao32
* @description 针对表【interface_info(接口信息)】的数据库操作Service实现
* @createDate 2024-06-06 10:20:45
*/
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
    implements InterfaceInfoService {
    @Resource
    private UserService userService;


    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {


        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String description = interfaceInfo.getDescription();
        String url = interfaceInfo.getUrl();
        String requestHeader = interfaceInfo.getRequestHeader();
        String responseHeader = interfaceInfo.getResponseHeader();
        String method = interfaceInfo.getMethod();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(name, url, requestHeader,responseHeader,method), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(name) && name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
        if (StringUtils.isNotBlank(method) && method.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求方法过长");
        }
        if (StringUtils.isNotBlank(name) && description.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述过长");
        }
        if (StringUtils.isNotBlank(url) && url.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "url过长");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {

        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (interfaceInfoQueryRequest == null) {
            return queryWrapper;
        }
        String name = interfaceInfoQueryRequest.getName();
        String url = interfaceInfoQueryRequest.getUrl();
        String requestHeader = interfaceInfoQueryRequest.getRequestHeader();
        String responseHeader = interfaceInfoQueryRequest.getResponseHeader();
        String description = interfaceInfoQueryRequest.getDescription();
        Long id = interfaceInfoQueryRequest.getId();
        Long userId = interfaceInfoQueryRequest.getUserId();
        Integer status = interfaceInfoQueryRequest.getStatus();
        String method = interfaceInfoQueryRequest.getMethod();

        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.eq(StringUtils.isNotBlank(url), "url", url);
        queryWrapper.eq(StringUtils.isNotBlank(requestHeader), "requestHeader", requestHeader);
        queryWrapper.eq(StringUtils.isNotBlank(responseHeader), "responseHeader", responseHeader);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.eq(StringUtils.isNotBlank(method), "method", method);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }



    @Override
    public InterfaceInfoVO getInterfaceInfoVO(InterfaceInfo interfaceInfo, HttpServletRequest request) {
        InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
        long interfaceInfoId = interfaceInfo.getId();
        // 1. 关联查询用户信息
        Long userId = interfaceInfo.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        interfaceInfoVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            // 获取点赞
//            QueryWrapper<InterfaceInfoThumb> interfaceInfoThumbQueryWrapper = new QueryWrapper<>();
//            interfaceInfoThumbQueryWrapper.in("interfaceInfoId", interfaceInfoId);
//            interfaceInfoThumbQueryWrapper.eq("userId", loginUser.getId());
//            InterfaceInfoThumb interfaceInfoThumb = interfaceInfoThumbMapper.selectOne(interfaceInfoThumbQueryWrapper);
//            interfaceInfoVO.setHasThumb(interfaceInfoThumb != null);
//            // 获取收藏
//            QueryWrapper<InterfaceInfoFavour> interfaceInfoFavourQueryWrapper = new QueryWrapper<>();
//            interfaceInfoFavourQueryWrapper.in("interfaceInfoId", interfaceInfoId);
//            interfaceInfoFavourQueryWrapper.eq("userId", loginUser.getId());
//            InterfaceInfoFavour interfaceInfoFavour = interfaceInfoFavourMapper.selectOne(interfaceInfoFavourQueryWrapper);
//            interfaceInfoVO.setHasFavour(interfaceInfoFavour != null);
//        }
        return interfaceInfoVO;
    }


}




