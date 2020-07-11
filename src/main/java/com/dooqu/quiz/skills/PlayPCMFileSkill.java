package com.dooqu.quiz.skills;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.dooqu.quiz.common.Client;
import com.dooqu.quiz.common.Skill;
import com.dooqu.quiz.utils.StreamUtils;

public class PlayPCMFileSkill extends Skill {
    protected String filePath;

    public PlayPCMFileSkill(String localFilePath, Client... clients) {
        super(0, clients);
        this.filePath = localFilePath;
    }

    @Override
    protected boolean onStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(filePath);
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(file);
                    setContentSize(false, file.length());
                    byte[] buffer = new byte[320];
                    int bytesRead = 0;
                    do {
                        bytesRead = fileInputStream.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            //System.out.println("write=" + bytesRead);
                            produceSkillData(buffer, 0, bytesRead);
                        } else {
                            setContentSize(true, file.length());
                        }
                    } while (isRunning() && bytesRead > 0);
                } catch (FileNotFoundException ex) {
                } catch (IOException ex2) {
                } finally {
                    StreamUtils.safeClose(fileInputStream);
                }
            }
        }).start();
        return true;
    }

    @Override
    protected void onStop() {
    }
}
