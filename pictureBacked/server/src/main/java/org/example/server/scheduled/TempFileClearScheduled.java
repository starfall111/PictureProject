package org.example.server.scheduled;

import cn.hutool.core.util.ObjUtil;
import org.example.common.constants.PictureConstant;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

@Slf4j
public class TempFileClearScheduled {

    private static int MAX_CLEAR_TIME = 60 * 60 * 2400;

    //每小时清理已存在1小时的临时文件
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void tempFileClear() {
        File dir = new File(PictureConstant.TEMP_FILE_URL);
        if (dir.exists()) {
            return;
        }

        File[] files = dir.listFiles();
        int deletedCount = 0;
        long now = System.currentTimeMillis();

        if (ObjUtil.isEmpty(files)) {
            return;
        }

        for (File file : files) {
            if (now - file.lastModified() >= MAX_CLEAR_TIME)
                if (file.delete()) {
                    deletedCount++;
                }
        }

        if (deletedCount > 0) {
            log.info("清理临时预览文件: 删除 {} 个过期文件", deletedCount);
        }
    }
}
