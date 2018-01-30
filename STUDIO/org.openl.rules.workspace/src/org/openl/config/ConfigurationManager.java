package org.openl.config;

import org.apache.commons.configuration.*;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Configuration manager.
 *
 * @author Andrei Astrouski
 *         <p/>
 *         TODO Separate configuration sets from the manager
 */
public class ConfigurationManager {
    public static String REPO_PASS_KEY = "repository.encode.decode.key";

    private final Logger log = LoggerFactory.getLogger(ConfigurationManager.class);

    private boolean useSystemProperties;
    private String propsLocation;
    private String propsInContextLocation;
    private String defaultPropsLocation;
    private boolean autoSave;

    private Configuration systemConfiguration;
    private FileConfiguration configurationToSave;
    private FileConfiguration defaultConfiguration;
    private CompositeConfiguration compositeConfiguration;

    public ConfigurationManager(boolean useSystemProperties, String propsLocation) {
        this(useSystemProperties, propsLocation, null, false);
    }

    public ConfigurationManager(boolean useSystemProperties, String propsLocation, String defaultPropsLocation) {
        this(useSystemProperties, propsLocation, defaultPropsLocation, false);
    }

    public ConfigurationManager(boolean useSystemProperties, String propsLocation, String defaultPropsLocation,
                                boolean autoSave) {
        this(useSystemProperties, propsLocation, null, defaultPropsLocation, autoSave);
    }

    public ConfigurationManager(boolean useSystemProperties, String propsLocation, String propsInContextLocation, String defaultPropsLocation,
                                boolean autoSave) {
        this.useSystemProperties = useSystemProperties;
        this.propsLocation = propsLocation;
        this.propsInContextLocation = propsInContextLocation;
        this.defaultPropsLocation = defaultPropsLocation;
        this.autoSave = autoSave;

        init();
    }

    private void init() {
        compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.setDelimiterParsingDisabled(true);

        if (useSystemProperties) {
            SystemConfiguration configuration = new SystemConfiguration();
            configuration.setDelimiterParsingDisabled(true);
            systemConfiguration = configuration;
            compositeConfiguration.addConfiguration(systemConfiguration);
        }

        configurationToSave = createFileConfiguration(propsLocation, true);
        if (configurationToSave != null) {
            compositeConfiguration.addConfiguration(configurationToSave);
            if (autoSave) {
                configurationToSave.setAutoSave(true);
            }
        }

        FileConfiguration propsInContext = createFileConfiguration(propsInContextLocation);
        if (propsInContext != null) {
            compositeConfiguration.addConfiguration(propsInContext);
        }

        defaultConfiguration = createFileConfiguration(defaultPropsLocation);
        if (defaultConfiguration != null) {
            compositeConfiguration.addConfiguration(defaultConfiguration);
        }
    }

    private FileConfiguration createFileConfiguration(String configLocation, boolean createIfNotExist) {
        PropertiesConfiguration configuration = null;
        if (configLocation != null) {
            try {
                if (createIfNotExist) {
                    configuration = new PropertiesConfiguration();
                    configuration.setDelimiterParsingDisabled(true);
                    File file = new File(configLocation);
                    configuration.setFile(file);
                    if (file.exists()) {
                        configuration.load();
                    }
                } else {
                    try {
                        configuration = new PropertiesConfiguration();
                        configuration.setDelimiterParsingDisabled(true);
                        configuration.setFileName(configLocation);
                        configuration.load();
                    } catch (ConfigurationException ignored) {
                        // Configuration isn't found. Skip it
                        return null;
                    }
                }
            } catch (Exception e) {
                log.error("Error when initializing configuration: {}", configLocation, e);
            }
        }
        return configuration;
    }

    private FileConfiguration createFileConfiguration(String configLocation) {
        return createFileConfiguration(configLocation, false);
    }

    public String getStringProperty(String key) {
        return compositeConfiguration.getString(key);
    }

    public String[] getStringArrayProperty(String key) {
        return compositeConfiguration.getStringArray(key);
    }

    public boolean getBooleanProperty(String key) {
        return compositeConfiguration.getBoolean(key);
    }

    public int getIntegerProperty(String key) {
        return compositeConfiguration.getInt(key);
    }

    public Long getLongProperty(String key, Long defaultValue) {
        return compositeConfiguration.getLong(key, defaultValue);
    }

    public Map<String, Object> getProperties() {
        return getProperties(false);
    }

    public Map<String, Object> getProperties(boolean cross) {
        Map<String, Object> properties = new HashMap<>();
        for (Iterator<?> iterator = compositeConfiguration.getKeys(); iterator.hasNext(); ) {
            String key = (String) iterator.next();

            if (!cross || configurationToSave.getProperty(key) != null) {
                Object value = compositeConfiguration.getProperty(key);
                if (value instanceof Collection || value != null && value.getClass().isArray()) {
                    properties.put(key, getStringArrayProperty(key));
                } else {
                    properties.put(key, getStringProperty(key));
                }
            }
        }
        return properties;
    }

    public void setProperty(String key, Object value) {
        if (key != null && value != null) {
            if (!(value instanceof Collection) && !value.getClass().isArray()) {
                String defaultValue = compositeConfiguration.getString(key);
                if (defaultValue == null || !defaultValue.equals(value.toString())) {
                    getConfigurationToSave().setProperty(key, value.toString());
                }
            } else {
                String[] defaultValue = compositeConfiguration.getStringArray(key);
                if (defaultValue != null) {
                    if (value instanceof Collection) {
                        @SuppressWarnings("unchecked")
                        Collection<String> v = (Collection<String>) value;
                        value = v.toArray(new String[v.size()]);
                    }
                    if (!defaultValue.equals(value)) {
                        getConfigurationToSave().setProperty(key, value);
                    }
                }
            }
        } else if (key != null) {
            removeProperty(key);
        }
    }

    public void removeProperty(String key) {
        getConfigurationToSave().clearProperty(key);
    }

    private FileConfiguration getConfigurationToSave() {
        if (configurationToSave == null) {
            configurationToSave = createFileConfiguration(propsLocation, true);
        }
        return configurationToSave;
    }

    public boolean isSystemProperty(String name) {
        return systemConfiguration != null && systemConfiguration.getString(name) != null;
    }

    public boolean save() {
        if (configurationToSave != null) {
            try {
                configurationToSave.save();
                return true;
            } catch (Exception e) {
                log.error("Error when saving configuration: {}", configurationToSave.getBasePath(), e);
            }
        }
        return false;
    }

    public boolean restoreDefaults() {
        if (configurationToSave != null && !configurationToSave.isEmpty()) {
            configurationToSave.clear();
            return save();
        }

        return false;
    }

    public boolean delete() {
        boolean deleted = false;

        if (configurationToSave != null) {
            deleted = configurationToSave.getFile().delete();
            configurationToSave = null;
        }

        return deleted;
    }

    public void setPassword(String key, String pass) {
        try {
            String repoPassKey = getRepoPassKey();
            setProperty(key, StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.encode(pass, repoPassKey));
        } catch (Exception e) {
            log.error("Error when setting password property: {}", key, e);
        }
    }

    public String getPassword(String key) {
        try {
            String repoPassKey = getRepoPassKey();
            String pass = getStringProperty(key);
            return StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.decode(pass, repoPassKey);
        } catch (Exception e) {
            log.error("Error when getting password property: {}", key, e);
            return "";
        }
    }

    private String getRepoPassKey() {
        return compositeConfiguration.containsKey(REPO_PASS_KEY) ? compositeConfiguration.getString(REPO_PASS_KEY) : "";
    }

}
