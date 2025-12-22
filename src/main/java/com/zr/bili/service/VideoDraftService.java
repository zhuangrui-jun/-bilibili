package com.zr.bili.service;

import com.zr.bili.entity.VideoDraft;

import java.util.List;

public interface VideoDraftService {

    /**
     * 保存草稿
     */
    Long saveDraft(VideoDraft draft);

    /**
     * 根据ID获取草稿
     */
    VideoDraft getDraftById(Long id);

    /**
     * 根据创建者ID获取草稿列表
     */
    VideoDraft getDraftsByCreatorId(Long creatorId);

    /**
     * 更新草稿
     */
    void updateDraft(VideoDraft draft);

    /**
     * 删除草稿
     */
    void deleteDraft(Long id);
}



















