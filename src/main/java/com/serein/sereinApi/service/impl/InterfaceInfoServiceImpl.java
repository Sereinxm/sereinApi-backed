package com.serein.sereinApi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import com.serein.sereinApi.mapper.InterfaceInfoMapper;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

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
        String searchText = interfaceInfoQueryRequest.getSearchText();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        Long id = interfaceInfoQueryRequest.getId();
        String title = interfaceInfoQueryRequest.getTitle();
        String content = interfaceInfoQueryRequest.getContent();
        List<String> tagList = interfaceInfoQueryRequest.getTags();
        Long userId = interfaceInfoQueryRequest.getUserId();
        Long notId = interfaceInfoQueryRequest.getNotId();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<InterfaceInfo> searchFromEs(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        Long id = interfaceInfoQueryRequest.getId();
        Long notId = interfaceInfoQueryRequest.getNotId();
        String searchText = interfaceInfoQueryRequest.getSearchText();
        String title = interfaceInfoQueryRequest.getTitle();
        String content = interfaceInfoQueryRequest.getContent();
        List<String> tagList = interfaceInfoQueryRequest.getTags();
        List<String> orTagList = interfaceInfoQueryRequest.getOrTags();
        Long userId = interfaceInfoQueryRequest.getUserId();
        // es 起始页为 0
        long current = interfaceInfoQueryRequest.getCurrent() - 1;
        long pageSize = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
        if (notId != null) {
            boolQueryBuilder.mustNot(QueryBuilders.termQuery("id", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        // 必须包含所有标签
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }
        // 包含任何一个标签即可
        if (CollUtil.isNotEmpty(orTagList)) {
            BoolQueryBuilder orTagBoolQueryBuilder = QueryBuilders.boolQuery();
            for (String tag : orTagList) {
                orTagBoolQueryBuilder.should(QueryBuilders.termQuery("tags", tag));
            }
            orTagBoolQueryBuilder.minimumShouldMatch(1);
            boolQueryBuilder.filter(orTagBoolQueryBuilder);
        }
        // 按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("description", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 按标题检索
        if (StringUtils.isNotBlank(title)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", title));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 按内容检索
        if (StringUtils.isNotBlank(content)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", content));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StringUtils.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        PageRequest pageRequest = PageRequest.of((int) current, (int) pageSize);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                .withPageable(pageRequest).withSorts(sortBuilder).build();
        SearchHits<InterfaceInfoEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, InterfaceInfoEsDTO.class);
        Page<InterfaceInfo> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<InterfaceInfo> resourceList = new ArrayList<>();
        // 查出结果后，从 db 获取最新动态数据（比如点赞数）
        if (searchHits.hasSearchHits()) {
            List<SearchHit<InterfaceInfoEsDTO>> searchHitList = searchHits.getSearchHits();
            List<Long> interfaceInfoIdList = searchHitList.stream().map(searchHit -> searchHit.getContent().getId())
                    .collect(Collectors.toList());
            List<InterfaceInfo> interfaceInfoList = baseMapper.selectBatchIds(interfaceInfoIdList);
            if (interfaceInfoList != null) {
                Map<Long, List<InterfaceInfo>> idInterfaceInfoMap = interfaceInfoList.stream().collect(Collectors.groupingBy(InterfaceInfo::getId));
                interfaceInfoIdList.forEach(interfaceInfoId -> {
                    if (idInterfaceInfoMap.containsKey(interfaceInfoId)) {
                        resourceList.add(idInterfaceInfoMap.get(interfaceInfoId).get(0));
                    } else {
                        // 从 es 清空 db 已物理删除的数据
                        String delete = elasticsearchRestTemplate.delete(String.valueOf(interfaceInfoId), InterfaceInfoEsDTO.class);
                        log.info("delete interfaceInfo {}", delete);
                    }
                });
            }
        }
        page.setRecords(resourceList);
        return page;
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
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<InterfaceInfoThumb> interfaceInfoThumbQueryWrapper = new QueryWrapper<>();
            interfaceInfoThumbQueryWrapper.in("interfaceInfoId", interfaceInfoId);
            interfaceInfoThumbQueryWrapper.eq("userId", loginUser.getId());
            InterfaceInfoThumb interfaceInfoThumb = interfaceInfoThumbMapper.selectOne(interfaceInfoThumbQueryWrapper);
            interfaceInfoVO.setHasThumb(interfaceInfoThumb != null);
            // 获取收藏
            QueryWrapper<InterfaceInfoFavour> interfaceInfoFavourQueryWrapper = new QueryWrapper<>();
            interfaceInfoFavourQueryWrapper.in("interfaceInfoId", interfaceInfoId);
            interfaceInfoFavourQueryWrapper.eq("userId", loginUser.getId());
            InterfaceInfoFavour interfaceInfoFavour = interfaceInfoFavourMapper.selectOne(interfaceInfoFavourQueryWrapper);
            interfaceInfoVO.setHasFavour(interfaceInfoFavour != null);
        }
        return interfaceInfoVO;
    }

    @Override
    public Page<InterfaceInfoVO> getInterfaceInfoVOPage(Page<InterfaceInfo> interfaceInfoPage, HttpServletRequest request) {
        List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords();
        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
        if (CollUtil.isEmpty(interfaceInfoList)) {
            return interfaceInfoVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = interfaceInfoList.stream().map(InterfaceInfo::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> interfaceInfoIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> interfaceInfoIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> interfaceInfoIdSet = interfaceInfoList.stream().map(InterfaceInfo::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<InterfaceInfoThumb> interfaceInfoThumbQueryWrapper = new QueryWrapper<>();
            interfaceInfoThumbQueryWrapper.in("interfaceInfoId", interfaceInfoIdSet);
            interfaceInfoThumbQueryWrapper.eq("userId", loginUser.getId());
            List<InterfaceInfoThumb> interfaceInfoInterfaceInfoThumbList = interfaceInfoThumbMapper.selectList(interfaceInfoThumbQueryWrapper);
            interfaceInfoInterfaceInfoThumbList.forEach(interfaceInfoInterfaceInfoThumb -> interfaceInfoIdHasThumbMap.put(interfaceInfoInterfaceInfoThumb.getInterfaceInfoId(), true));
            // 获取收藏
            QueryWrapper<InterfaceInfoFavour> interfaceInfoFavourQueryWrapper = new QueryWrapper<>();
            interfaceInfoFavourQueryWrapper.in("interfaceInfoId", interfaceInfoIdSet);
            interfaceInfoFavourQueryWrapper.eq("userId", loginUser.getId());
            List<InterfaceInfoFavour> interfaceInfoFavourList = interfaceInfoFavourMapper.selectList(interfaceInfoFavourQueryWrapper);
            interfaceInfoFavourList.forEach(interfaceInfoFavour -> interfaceInfoIdHasFavourMap.put(interfaceInfoFavour.getInterfaceInfoId(), true));
        }
        // 填充信息
        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream().map(interfaceInfo -> {
            InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
            Long userId = interfaceInfo.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            interfaceInfoVO.setUser(userService.getUserVO(user));
            interfaceInfoVO.setHasThumb(interfaceInfoIdHasThumbMap.getOrDefault(interfaceInfo.getId(), false));
            interfaceInfoVO.setHasFavour(interfaceInfoIdHasFavourMap.getOrDefault(interfaceInfo.getId(), false));
            return interfaceInfoVO;
        }).collect(Collectors.toList());
        interfaceInfoVOPage.setRecords(interfaceInfoVOList);
        return interfaceInfoVOPage;
    }

}




