package core

class ConfigUtils {
    public static final String CONFIG_FILENAME = "cfg.properties";
    public static Properties CONFIG = readConfiguration();

    private static Properties readConfiguration() {
        def properties = new Properties();
        File propertiesFile = FileUtils.getInternalFile(CONFIG_FILENAME)
        propertiesFile.withInputStream {
            properties.load(it)
        }
        return properties;
    }
}
