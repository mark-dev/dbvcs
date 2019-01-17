
@Singleton
class ConfigUtils {
    public static final String DEFAULT_CONFIG_FILENAME = "dbvcs.properties";
    public static Properties CONFIG = readConfiguration();

    private static Properties readConfiguration() {
        def properties = new Properties();
        def configFileName = System.getProperty("dbVCSConfigFile",DEFAULT_CONFIG_FILENAME)
        File propertiesFile = FileUtils.projectSpecificFile(configFileName).toFile()
        propertiesFile.withInputStream {
            properties.load(it)
        }
        return properties;
    }
}
