package com.supos.app.rest;

import com.supos.app.service.AppManagerService;
import com.supos.app.util.Constants;
import com.supos.app.vo.AppHtmlVO;
import com.supos.app.vo.AppVO;
import com.supos.app.vo.CreateAppVO;
import com.supos.app.vo.CreateHomepageVO;
import com.supos.common.exception.AppException;
import com.supos.common.exception.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
public class AppManagerController {

    @Autowired
    private AppManagerService appManagerService;

    /**
     * 查询app列表，支持搜索
     * 暂时不支持分页
     * @return
     */
    @GetMapping("/inter-api/supos/apps")
    @ResponseBody
    public ResultVO<List> queryApps(@Nullable @RequestParam("k") String fuzzyName) {
        List<AppVO> appVos = appManagerService.searchApps(fuzzyName);
        return ResultVO.successWithData(appVos);
    }

    /**
     * 创建app
     * @param requestBody
     * @return
     */
    @PostMapping("/inter-api/supos/app/create")
    @ResponseBody
    public ResultVO createApp(@Valid  @RequestBody CreateAppVO requestBody) {
        appManagerService.createApp(requestBody.getName());
        return ResultVO.success("ok");
    }

    @PostMapping("/inter-api/supos/app/homepage")
    @ResponseBody
    public ResultVO setHomepage(@Valid  @RequestBody CreateHomepageVO requestBody) {
        appManagerService.setHomepage(requestBody.getAppName(), requestBody.getHtmlId());
        return ResultVO.success("ok");
    }

    /**
     * 获取单个app的html列表
     * @param name
     * @return
     */
    @GetMapping("/inter-api/supos/app/{name}")
    @ResponseBody
    public ResultVO<AppVO> getApp(@PathVariable("name") String name) {
        AppVO appVo = appManagerService.getApp(name);
        return ResultVO.successWithData(appVo);
    }

    /**
     * 获取单个app的单个html内容
     * @param name
     * @param htmlName
     * @return
     */
    @GetMapping("/inter-api/supos/app/{name}/html/{htmlName}")
    @ResponseBody
    public ResultVO<AppHtmlVO> getHtmlContent(@PathVariable("name") String name, @PathVariable("htmlName") String htmlName) {
        if (!htmlName.endsWith(".html")) {
            throw new AppException(500, "nodered.invalid.parameter");
        }
        AppHtmlVO htmlVo = appManagerService.getHtmlContent(name, htmlName);
        return ResultVO.successWithData(htmlVo);
    }

    /**
     * 预览html
     * @param name
     * @param htmlName
     * @return
     */
    @GetMapping(Constants.APP_PREVIEW_URL)
    public String previewHtml(@PathVariable("name") String name, @PathVariable("htmlName") String htmlName) {
        AppHtmlVO htmlVo = appManagerService.getHtmlContent(name, htmlName);
        return htmlVo.getContent();
    }

    /**
     * 根据名称删除指定app
     * @param name
     * @return
     */
    @DeleteMapping("/inter-api/supos/app/{name}/destroy")
    @ResponseBody
    public ResultVO deleteApp(@PathVariable("name") String name) throws IOException {
        appManagerService.deleteApp(name);
        return ResultVO.success("ok");
    }

    /**
     * delete single html file
     * @param name
     * @param id
     * @return
     * @throws IOException
     */
    @DeleteMapping("/inter-api/supos/app/{name}/html/{id}")
    @ResponseBody
    public ResultVO deleteHtml(@PathVariable("name") String name, @PathVariable("id") String id) throws IOException {
        appManagerService.deleteHtml(Long.parseLong(id));
        return ResultVO.success("ok");
    }

    /**
     * 上传html文件
     */
    @PutMapping("/inter-api/supos/app/{name}")
    @ResponseBody
    public ResultVO uploadEntranceFile(@RequestParam MultipartFile file, @PathVariable("name") String name) throws IOException {
        if (!file.getOriginalFilename().endsWith(".html")) {
            throw new AppException(500, "nodered.upload.invalid.format");
        }
        File target = new File(Constants.APP_ROOT + name);
        if (!target.exists()) {
            throw new AppException(500, "nodered.app.not.exist");
        }
        String targetPath = String.format("%s%s/%s", Constants.APP_ROOT, name, file.getOriginalFilename());
        File targetFile = new File(targetPath);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        file.transferTo(targetFile);
        appManagerService.addAppHtml(name, targetPath, file.getOriginalFilename());
        return ResultVO.success("ok");
    }


}
