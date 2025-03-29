package com.supos.adpter.kong.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.adpter.kong.dao.mapper.UserMenuMapper;
import com.supos.adpter.kong.dao.po.UserMenuPo;
import com.supos.adpter.kong.vo.ResultVO;
import com.supos.adpter.kong.vo.RouteVO;
import com.supos.common.config.SystemConfig;
import com.supos.common.dto.UserMenuDto;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.UserContext;
import com.supos.common.vo.UserInfoVo;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xinwangji@supos.com
 * @date 2024/12/10 10:31
 * @description
 */
@Service
public class UserMenuService extends ServiceImpl<UserMenuMapper, UserMenuPo> {

    @Resource
    private KongAdapterService kongAdapterService;
    @Resource
    private SystemConfig systemConfig;

    public List<RouteVO> getUserRouteList(){
        List<RouteVO> routeList = kongAdapterService.queryRoutes();

        if (BooleanUtil.isTrue(systemConfig.getAuthEnable())) {
            UserInfoVo userInfoVo = UserContext.get();
            if (null == userInfoVo){
                return routeList;
            }
            Date now = new Date();
            List<UserMenuPo> userMenuList = getUserMenuList(userInfoVo.getSub());
            if (CollectionUtil.isEmpty(userMenuList)) {
                userMenuList = routeList.stream().map(route -> {
                    UserMenuPo po = new UserMenuPo();
                    po.setId(IdUtil.getSnowflake().nextId());
                    po.setUserId(userInfoVo.getSub());
                    po.setMenuName(route.getName());
                    po.setPicked(true);
                    po.setUpdateTime(now);
                    po.setCreateTime(now);
                    return po;
                }).collect(Collectors.toList());
                saveBatch(userMenuList);
            }
            Map<String, Boolean> menuPickedMap = userMenuList.stream().collect(Collectors.toMap(UserMenuPo::getMenuName, UserMenuPo::getPicked));
            routeList.forEach(routeVO -> {
                if (ObjectUtil.isNotNull(routeVO.getMenu())){
                    routeVO.getMenu().setPicked(menuPickedMap.getOrDefault(routeVO.getName(),true));
                }
            });
        } else {
            routeList.forEach(routeVO -> {
                if (ObjectUtil.isNotNull(routeVO.getMenu())){
                    routeVO.getMenu().setPicked(true);
                }
            });
        }
        return routeList;
    }


    public List<UserMenuPo> getUserMenuList(String userId){
        LambdaQueryWrapper<UserMenuPo> qw = new LambdaQueryWrapper<>();
        qw.eq(UserMenuPo::getUserId,userId);
        return list(qw);
    }

    public ResultVO setUserMenu(List<UserMenuDto> userMenuList){
        UserInfoVo userInfoVo = UserContext.get();
        if (null == userInfoVo){
            return ResultVO.fail(I18nUtils.getMessage("user.not.login"));
        }

        Date now = new Date();
        for (UserMenuDto userMenuDto : userMenuList) {
            UserMenuPo po = this.baseMapper.getByMenuName(userInfoVo.getSub(),userMenuDto.getMenuName());
            if (null == po){
                po = new UserMenuPo();
                po.setId(IdUtil.getSnowflake().nextId());
                po.setUserId(userInfoVo.getSub());
                po.setMenuName(userMenuDto.getMenuName());
            }
            po.setPicked(userMenuDto.getPicked());
            po.setUpdateTime(now);
            saveOrUpdate(po);
        }
        return ResultVO.success("ok");
    }

}
