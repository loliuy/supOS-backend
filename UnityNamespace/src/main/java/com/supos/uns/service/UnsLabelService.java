package com.supos.uns.service;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supos.common.event.RemoveTopicsEvent;
import com.supos.common.exception.vo.ResultVO;
import com.supos.common.utils.I18nUtils;
import com.supos.common.utils.PathUtil;
import com.supos.uns.dao.mapper.UnsLabelMapper;
import com.supos.uns.dao.mapper.UnsMapper;
import com.supos.uns.dao.po.AlarmPo;
import com.supos.uns.dao.po.UnsLabelPo;
import com.supos.uns.dao.po.UnsLabelRefPo;
import com.supos.uns.dao.po.UnsPo;
import com.supos.uns.vo.FileVo;
import com.supos.uns.vo.LabelVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.supos.uns.service.UnsManagerService.genIdForPath;

@Service
public class UnsLabelService extends ServiceImpl<UnsLabelMapper, UnsLabelPo> {

    @Resource
    private UnsMapper unsMapper;

    @Resource
    private UnsLabelRefService unsLabelRefService;

    /**
     * 标签列表
     */
    public ResultVO<List<UnsLabelPo>> allLabels(String key) {
        List<UnsLabelPo> list = this.baseMapper.selectList(new LambdaQueryWrapper<UnsLabelPo>().like(StringUtils.isNotBlank(key),UnsLabelPo::getLabelName,key));
        return ResultVO.successWithData(list);
    }


    public ResultVO<LabelVo> detail(Long id){
        UnsLabelPo po = getById(id);
        if (null == po){
            return ResultVO.fail(I18nUtils.getMessage("uns.label.not.exists"));
        }
        LabelVo vo = BeanUtil.copyProperties(po, LabelVo.class);
        //模板引用的模型和实例列表
        List<UnsPo> templateRefs = this.baseMapper.getUnsByLabel(id);
        if (CollectionUtils.isNotEmpty(templateRefs)){
            List<FileVo> fileList = templateRefs.stream().map(uns -> {
                FileVo fileVo = new FileVo();
                fileVo.setUnsId(uns.getId());
                fileVo.setPath(uns.getPath());
                fileVo.setName(PathUtil.getName(uns.getPath()));
                return fileVo;
            }).collect(Collectors.toList());
            vo.setFileVoList(fileList);
        }
        return ResultVO.successWithData(vo);
    }

    public ResultVO<UnsLabelPo> create(String name){
        long c = count(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getLabelName,name));
        if (c > 0){
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        UnsLabelPo po = new UnsLabelPo(name);
        save(po);
        return ResultVO.successWithData(po);
    }

    public ResultVO delete(Long id){
        removeById(id);
        this.baseMapper.deleteRefByLabelId(id);
        return ResultVO.success("ok");
    }

    public ResultVO deleteByName(String name){
        UnsLabelPo unsLabelPo = getOne(new LambdaQueryWrapper<UnsLabelPo>().eq(UnsLabelPo::getLabelName,name));
        if (null != unsLabelPo){
            this.baseMapper.deleteRefByLabelId(unsLabelPo.getId());
            baseMapper.deleteById(unsLabelPo.getId());
        }
        return ResultVO.success("ok");
    }

    public ResultVO update(LabelVo labelVo){
        long c = count(new LambdaQueryWrapper<UnsLabelPo>()
                .eq(UnsLabelPo::getLabelName, labelVo.getLabelName())
                .ne(UnsLabelPo::getId, labelVo.getId()));
        if (c > 0) {
            return ResultVO.fail(I18nUtils.getMessage("uns.label.already.exists"));
        }
        if (CollectionUtils.isNotEmpty(labelVo.getFileVoList())){
            this.baseMapper.deleteRefByLabelId(labelVo.getId());
            //去重
            List<FileVo> distList = labelVo.getFileVoList().stream()
                    .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(FileVo::getUnsId))), ArrayList<FileVo>::new));
            distList.forEach(x ->{
                UnsLabelRefPo ref = new UnsLabelRefPo(labelVo.getId(),x.getUnsId());
                this.baseMapper.saveRef(ref);
            });
        }
        LambdaUpdateWrapper<UnsLabelPo> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(UnsLabelPo::getId, labelVo.getId());
        lambdaUpdateWrapper.set(UnsLabelPo::getLabelName, labelVo.getLabelName());
        update(lambdaUpdateWrapper);
        return ResultVO.success("ok");
    }

    public ResultVO makeLabel(String alias, List<LabelVo> labelList) {
        UnsPo uns = unsMapper.getByAlias(alias);
        if (null == uns){
            return ResultVO.success("ok");
        }
        this.baseMapper.deleteRefByUnsId(uns.getId());
        if (CollectionUtils.isNotEmpty(labelList)) {
            for (LabelVo labelVo : labelList) {
                UnsLabelRefPo ref = null;
                if (null != labelVo.getId()){
                    ref = new UnsLabelRefPo(labelVo.getId(),uns.getId());
                } else {
                    //不存在标签，先创建
                    Long labelId = create(labelVo.getLabelName()).getData().getId();
                    ref = new UnsLabelRefPo(labelId,uns.getId());
                }
                this.baseMapper.saveRef(ref);
            }
        }
        return ResultVO.success("ok");
    }

    /**
     * 批量新增标签绑定关系。
     * 标签不存在时，先新增标签
     * @param labelListMap
     * @return
     */
    @Transactional(timeout = 300, rollbackFor = Throwable.class)
    public ResultVO makeLabel(Map<String, String[]> labelListMap) {
        if (labelListMap != null) {
            Set<String> labels = new HashSet<>();
            Map<String, Set<String>> labelUnsMap = new HashMap<>();//label绑定了哪些uns节点
            for (Map.Entry<String, String[]> e : labelListMap.entrySet()) {
                if (ArrayUtils.isNotEmpty(e.getValue())) {
                    for(String label : e.getValue()) {
                        labels.add(label);
                        labelUnsMap.computeIfAbsent(label, k -> new HashSet<>()).add(e.getKey());
                    }
                }
            }

            List<UnsLabelPo> saveLabels = new ArrayList<>();
            List<UnsLabelRefPo> saveLabelRef = new ArrayList<>();
            List<Pair<String, String>> tempLabelUns = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(labels)) {
                List<UnsLabelPo> existLabels = baseMapper.selectList(Wrappers.lambdaQuery(UnsLabelPo.class).in(UnsLabelPo::getLabelName, labels));
                Map<String, UnsLabelPo> existLabelMap = existLabels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
                for (String label : labels) {
                    Set<String> unsIds = labelUnsMap.get(label);
                    UnsLabelPo existLabel = existLabelMap.get(label);
                    if (existLabel == null) {
                        // 新增标签
                        UnsLabelPo po = new UnsLabelPo(label);
                        saveLabels.add(po);
                        tempLabelUns.addAll(unsIds.stream().map(unsId -> Pair.of(label, unsId)).collect(Collectors.toList()));
                    } else {
                        saveLabelRef.addAll(unsIds.stream().map(unsId -> new UnsLabelRefPo(existLabel.getId(), unsId)).collect(Collectors.toList()));
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(saveLabels)) {
                saveBatch(saveLabels);
                Map<String, UnsLabelPo> existLabelMap = saveLabels.stream().collect(Collectors.toMap(UnsLabelPo::getLabelName, Function.identity(), (k1, k2) -> k2));
                for (Pair<String, String> tempLabelUn : tempLabelUns) {
                    UnsLabelPo existLabel = existLabelMap.get(tempLabelUn.getKey());
                    saveLabelRef.add(new UnsLabelRefPo(existLabel.getId(), tempLabelUn.getValue()));
                }
            }

            if (CollectionUtils.isNotEmpty(saveLabelRef)) {
                unsLabelRefService.saveBatch(saveLabelRef);
            }
        }
        return ResultVO.success("ok");
    }

    @EventListener(classes = RemoveTopicsEvent.class)
    @Order(2000)
    void onRemoveTopicsEvent(RemoveTopicsEvent event) {
        List<String> unsIds = event.topics.keySet().stream().map(UnsManagerService::genIdForPath).toList();
        LambdaQueryWrapper<UnsLabelRefPo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CollectionUtils.isNotEmpty(unsIds),UnsLabelRefPo::getUnsId,unsIds);
        unsLabelRefService.remove(queryWrapper);
    }
}
