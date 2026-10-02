package com.uiu.examintegrity.exam.infrastructure.code;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Limits for the local executor, and where each toolchain lives. These are the only thing standing between a submitted
 * program and the server, so every one of them matters; the defaults are also set
 * explicitly in {@code application.properties} so they are discoverable.
 */
@Component
@ConfigurationProperties(prefix = "app.code")
public class CodeExecutionProperties {
    /** The C compiler to invoke. A bare name is resolved on PATH. */
    private String gccPath = "gcc";

    /** The C++ compiler to invoke. */
    private String gppPath = "g++";

    /**
     * The Python interpreter to invoke. Not {@code python3}: on Windows that name is an App
     * Execution Alias pointing at the Microsoft Store, which is not an interpreter.
     */
    private String pythonPath = "python";

    private String cStandard = "c11";

    /**
     * Configurable rather than fixed because old GCC rejects a flag it does not know outright:
     * GCC 6 has no {@code c++17}, only {@code c++1z}. The default is the one every compiler from
     * GCC 5 onwards accepts.
     */
    private String cppStandard = "c++14";

    /** Parent directory for the throwaway per-run working directories. */
    private String workDir = System.getProperty("java.io.tmpdir") + "/examintegrity-code";

    private long compileTimeoutMs = 10_000;

    private long runTimeoutMs = 2_000;

    private int maxOutputBytes = 64 * 1024;

    private int maxSourceChars = 20_000;

    /** Runs happen on the request thread, so this bounds how much compiling the server does at once. */
    private int maxConcurrentRuns = 2;

    /** How long a request waits for a slot before giving up with a 409. */
    private long queueTimeoutMs = 15_000;

    public String getGccPath() {
        return gccPath;
    }

    public void setGccPath(String gccPath) {
        this.gccPath = gccPath;
    }

    public String getGppPath() {
        return gppPath;
    }

    public void setGppPath(String gppPath) {
        this.gppPath = gppPath;
    }

    public String getPythonPath() {
        return pythonPath;
    }

    public void setPythonPath(String pythonPath) {
        this.pythonPath = pythonPath;
    }

    public String getCStandard() {
        return cStandard;
    }

    public void setCStandard(String cStandard) {
        this.cStandard = cStandard;
    }

    public String getCppStandard() {
        return cppStandard;
    }

    public void setCppStandard(String cppStandard) {
        this.cppStandard = cppStandard;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public long getCompileTimeoutMs() {
        return compileTimeoutMs;
    }

    public void setCompileTimeoutMs(long compileTimeoutMs) {
        this.compileTimeoutMs = compileTimeoutMs;
    }

    public long getRunTimeoutMs() {
        return runTimeoutMs;
    }

    public void setRunTimeoutMs(long runTimeoutMs) {
        this.runTimeoutMs = runTimeoutMs;
    }

    public int getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(int maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public int getMaxSourceChars() {
        return maxSourceChars;
    }

    public void setMaxSourceChars(int maxSourceChars) {
        this.maxSourceChars = maxSourceChars;
    }

    public int getMaxConcurrentRuns() {
        return maxConcurrentRuns;
    }

    public void setMaxConcurrentRuns(int maxConcurrentRuns) {
        this.maxConcurrentRuns = maxConcurrentRuns;
    }

    public long getQueueTimeoutMs() {
        return queueTimeoutMs;
    }

    public void setQueueTimeoutMs(long queueTimeoutMs) {
        this.queueTimeoutMs = queueTimeoutMs;
    }
}
