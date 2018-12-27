package core

import groovy.io.FileType
import groovy.sql.Sql
import groovy.text.SimpleTemplateEngine

import java.nio.file.Paths
import java.util.regex.Pattern

import static ConfigUtils.CONFIG

class DBUtils {
    public static final String DBVCS_TABLENAME = "dbvcs";
    public static final String DEFAULT_SCHEMA = "public"; //TODO: Это бы в конфигурацию

    static Sql ROOT_CONNECTION = makeConnection("postgres");
    static Sql TARGET_DB_CONNECTION = null;

    static Sql makeConnection(String db) {
        //TODO: в конфиг, как подключиться к БД(логин пароль итп)
        def dbUrl = "jdbc:postgresql://${CONFIG.host}/${db}?user=postgres&password=postgres"
        return Sql.newInstance(dbUrl, [
                'driverClassName'  : "org.postgresql.Driver",
                'allowMultiQueries': "true"
        ] as Properties)
    }

    //Может быть NULL
    static Integer currentDBVersion() {
        String query = "select max(version) as current_version from $DBVCS_TABLENAME";
        return TARGET_DB_CONNECTION.firstRow(query)["current_version"] as Integer
    }

    static boolean ensureVCSDBTableExists() {
        String query = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = ?1 AND table_name = ?2)"
        def row = TARGET_DB_CONNECTION.firstRow(query, [DEFAULT_SCHEMA, DBVCS_TABLENAME]);
        if (!row.exists) {
            def createRes = evalInternalFile(TARGET_DB_CONNECTION, "create-vcsdb-table.sql", [:])
            println "dbvcs table is missing, but created automatically"
        }
        return true;
    }

    static boolean isTargetDBExists() {
        def rows = ROOT_CONNECTION.rows("SELECT 1 FROM pg_catalog.pg_database WHERE datname=?1", [CONFIG.targetDB])
        return rows.asBoolean();
    }

    static String createTargetDB() {
        def res = evalInternalFile(ROOT_CONNECTION, "create-db.sql", [
                "db"    : CONFIG.targetDB,
                "dbuser": CONFIG.targetDBUser
        ])
        if (res)
            return CONFIG.targetDB;
        else
            throw new RuntimeException("Failed to create target DB");
    }

    static Sql connectToTargetDB() {
        TARGET_DB_CONNECTION = makeConnection(CONFIG.targetDB as String)
        return TARGET_DB_CONNECTION;
    }


    static String internalFileContent(String fileName, Map binding) {
        def file = FileUtils.getInternalFile(fileName)
        def engine = new SimpleTemplateEngine();
        def script = engine.createTemplate(file).make(binding).toString()
        return script;
    }

    static boolean evalInternalFile(Sql connection, String fileName, Map binding) {
        def script = internalFileContent(fileName, binding);
        def separator = "\n" + "=" * 80 + "\n";
        println("$separator Executing SQL: $separator $script $separator")
        connection.execute(script)
    }

    static boolean freshInstall() {
        return upgrade(true, 0, Integer.MAX_VALUE)
    }

    static boolean upgrade(boolean initialSchema, int startFromPatch, int stopOnPatch) {


        def evalFiles = {
            it.each {
                println "Eval file: $it"
                String sql = it.text;
                TARGET_DB_CONNECTION.execute(sql)
            }
        }

        def evalInternalFile = {
            name, mapping ->
                println "Eval internal file: $name"
                def content = internalFileContent(name, mapping)
                TARGET_DB_CONNECTION.execute(content)
        }

        //Нужно для того, чтоб записать последнюю версию
        Collection<File> patchs = patchFiles(startFromPatch, stopOnPatch);
        Integer versionAfterUpdate;
        String scriptFileName;

        if (patchs) {
            scriptFileName = patchs.last().getName();
            versionAfterUpdate = extractVersionFromPatchFile(patchs.last());
        } else {
            if (initialSchema) {
                scriptFileName = "initial";
                versionAfterUpdate = 0;
            } else {
                scriptFileName = "src-update";
                versionAfterUpdate = currentDBVersion();
            }
        }

        TARGET_DB_CONNECTION.withTransaction {

            //Если нужно - накатываем изначальную схему
            if (initialSchema)
                evalFiles(schemaFiles())
            //Накатываем патчи
            evalFiles(patchs)


            //Дропаем все исходники(хранимки,вью,мвью)
            evalInternalFile("drop-src.sql", ["schema": "public"])
            //Накатываем исходники
            evalFiles(sourceFiles());


            //Проставляем правильные права для всех объектов
            evalInternalFile("ownership.sql", ["dbuser": CONFIG.targetDBUser])
            //Пишем версию накатанной схемы
            TARGET_DB_CONNECTION.execute("INSERT INTO $DBVCS_TABLENAME(version,script_name) VALUES (?1,?2)", [versionAfterUpdate, scriptFileName])
        }

        return true;
    }

    static Pattern PATCH_FILE_REGEXP = ~/^(\d+)_.*\.sql/

    static Integer extractVersionFromPatchFile(File f) {
        def match = (f.getName() =~ PATCH_FILE_REGEXP)
        if (match.matches()) {
            String ver = match.group(1);
            Integer verInt = ver as Integer;
            return verInt
        } else
            return null;
    }

    static TreeMap<Integer, File> patchFileTreeMap(Integer begin, Integer end) {
        def patchesDir = Paths.get("patch").toFile()
        TreeMap<Integer, File> matchedPatch = [:]

        //Собираем файлы, которые попали под регулярку и диапазон версий
        patchesDir.eachFile {
            File it ->
                Integer ver = extractVersionFromPatchFile(it)
                if (ver && begin < ver && end <= ver) {
                    matchedPatch.put(ver, it);
                } else {
                    println "Patch file ${it} skipped"
                }
        }
        return matchedPatch;
    }

    static Collection<File> patchFiles(Integer begin, Integer end) {

        TreeMap<Integer, File> matchedPatch = patchFileTreeMap(begin, end)
        //Таким способом сортируем, в правильном порядке - сначала старые потом новые
        return matchedPatch.collect {
            k, v -> v
        }
    }

    static Collection<File> sourceFiles() {
        LinkedHashSet result = []
        def srcPath = Paths.get("src");
        //Сначала парсим файлик с порядком, и добавляем их в начало, все остальное - как получится
        def orderFile = srcPath.resolve("order.txt").toFile();
        if (orderFile.exists()) {
            String[] hasOrder = orderFile;
            for (String h : hasOrder) {
                def file = srcPath.resolve(h).toFile();
                if (file.exists())
                    result.add(file)
                else
                    println "Bad order file, $h is not found"
            }
        } else
            println "File ${orderFile.getAbsolutePath()} not exists"

        //Сканириуем директорию на предмет sql файлов, и добавляем их в результат
        //Но лишь при условии, что они там отсутствуют (чтобы не портить порядок)
        def appendInDirectory = { String dir ->
            def storedProcedures = srcPath.resolve(dir).toFile();
            if (storedProcedures.exists()) {
                storedProcedures.eachFile(FileType.FILES) {
                    if (it.getName().endsWith(".sql")) {
                        if (!result.contains(it)) {
                            result.add(it)
                        }
                    } else
                        println "File ${it} skipped"
                }
            } else
                println "${dir} is not found, skip it"
        }
        //Хранимые процедуры
        appendInDirectory("sp")
        //Представления
        appendInDirectory("view")

        return result;
    }

    static Collection<File> schemaFiles() {
        def files = [
                Paths.get("schema", "schema.sql").toFile(),
                Paths.get("schema", "data.sql").toFile()
        ]
        return files;
    }
}
