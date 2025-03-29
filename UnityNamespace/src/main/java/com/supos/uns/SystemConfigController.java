package com.supos.uns;

import com.supos.common.dto.SysModuleDto;
import com.supos.common.enums.SysModuleEnum;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.config.SystemConfig;
import com.supos.common.utils.I18nUtils;
import com.supos.uns.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置
 */
@Slf4j
@RestController
@RequestMapping("/inter-api/supos/systemConfig")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取系统配置
     */
    @GetMapping
    public ResultVO<SystemConfig> systemConfig(){
        return systemConfigService.getSystemConfig();
    }


    @GetMapping("/moduleList")
    public ResultVO<List<SysModuleDto>> moduleList(){
       return systemConfigService.moduleList();
    }
}
