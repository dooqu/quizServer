package com.dooqu.quiz.skills;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SkillInputStream extends InputStream {


    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return super.read(b);
    }


    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }
}
