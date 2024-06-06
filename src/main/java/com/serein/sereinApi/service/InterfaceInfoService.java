package com.serein.sereinApi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.serein.sereinApi.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.serein.sereinApi.model.entity.InterfaceInfo;
import com.serein.sereinApi.model.vo.InterfaceInfoVO;

import javax.servlet.http.HttpServletRequest;


/**
 * @author cao32
 * @description 针对表【interface_info(接口信息)】的数据库操作Service
 * @createDate 2024-06-06 10:15:19
 */
public interface InterfaceInfoService extends IService<InterfaceInfo> {
    /**
     * 校验参数
     *
     * @param interfaceInfo
     * @param add
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);

    /**
     * 获取查询条件
     *
     * @param interfaceInfoQueryRequest 接口信息查询条件
     * @return
     */
    QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest);


    /**
     * 获取封装
     *
     * @param interfaceInfo
     * @param request
     * @return
     */
    InterfaceInfoVO getInterfaceInfoVO(InterfaceInfo interfaceInfo, HttpServletRequest request);


}
