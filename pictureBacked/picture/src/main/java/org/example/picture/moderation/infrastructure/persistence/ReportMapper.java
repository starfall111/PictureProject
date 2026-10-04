package org.example.picture.moderation.infrastructure.persistence;

import org.example.picture.moderation.domain.model.Report;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【report(举报)】的数据库操作Mapper
 * @Entity org.example.picture.moderation.domain.model.Report
 */
public interface ReportMapper extends BaseMapper<Report> {
}
