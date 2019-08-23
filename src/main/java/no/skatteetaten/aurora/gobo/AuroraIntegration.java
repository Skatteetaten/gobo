package no.skatteetaten.aurora.gobo;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//TODO: This class can be ported to Kotlin after spring boot 2.2
@ConfigurationProperties("integrations")
@Component
public class AuroraIntegration {

    private Map<String, DockerRegistry> docker;

    public Map<String, DockerRegistry> getDocker() {
        return docker;
    }

    public void setDocker(Map<String, DockerRegistry> docker) {
        this.docker = docker;
    }

    public enum AuthType {
        None,
        Basic,
        Bearer
    }

    public static class DockerRegistry {

        private String url;
        private String guiUrlPattern = null;
        private AuthType auth = AuthType.None;
        private boolean https = true;
        private boolean readOnly = true;
        private boolean enabled = true;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public AuthType getAuth() {
            return auth;
        }

        public void setAuth(AuthType auth) {
            this.auth = auth;
        }

        public boolean isHttps() {
            return https;
        }

        public void setHttps(boolean https) {
            this.https = https;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getGuiUrlPattern() {
            return guiUrlPattern;
        }

        public void setGuiUrlPattern(String guiUrlPattern) {
            this.guiUrlPattern = guiUrlPattern;
        }
    }
}