package com.architectai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "architectai.static-analysis")
public class StaticAnalysisProperties {

    private final Tools tools = new Tools();
    private final Thresholds thresholds = new Thresholds();

    public Tools getTools() {
        return tools;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public static class Tools {
        private final ToolSettings spotbugs = new ToolSettings();
        private final ToolSettings pmd = new ToolSettings();
        private final ToolSettings checkstyle = new ToolSettings();
        private final ToolSettings semgrep = new ToolSettings();

        public ToolSettings getSpotbugs() {
            return spotbugs;
        }

        public ToolSettings getPmd() {
            return pmd;
        }

        public ToolSettings getCheckstyle() {
            return checkstyle;
        }

        public ToolSettings getSemgrep() {
            return semgrep;
        }
    }

    public static class ToolSettings {
        private boolean enabled = true;
        private String path = "";
        private long timeoutSeconds = 60L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Thresholds {
        private int maxLineLength = 120;
        private int maxFileLines = 400;
        private int maxMethodLines = 80;

        public int getMaxLineLength() {
            return maxLineLength;
        }

        public void setMaxLineLength(int maxLineLength) {
            this.maxLineLength = maxLineLength;
        }

        public int getMaxFileLines() {
            return maxFileLines;
        }

        public void setMaxFileLines(int maxFileLines) {
            this.maxFileLines = maxFileLines;
        }

        public int getMaxMethodLines() {
            return maxMethodLines;
        }

        public void setMaxMethodLines(int maxMethodLines) {
            this.maxMethodLines = maxMethodLines;
        }
    }
}
