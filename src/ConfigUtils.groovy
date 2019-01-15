
@Singleton
class ConfigUtils {
    public static final String CONFIG_FILENAME = "dbvcs.properties";
    public static Properties CONFIG = readConfiguration();

    private static Properties readConfiguration() {
        def properties = new Properties();
        File propertiesFile = FileUtils.projectSpecificFile(CONFIG_FILENAME).toFile()
        propertiesFile.withInputStream {
            properties.load(it)
        }
        return properties;
    }
}
