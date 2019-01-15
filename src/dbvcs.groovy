@GrabConfig(systemClassLoader = true)
//Почемуто без этого, все фейлиться, с сообщением что не найден класс для JDBC соединения.
//Хотя в гайдах везде говорят что systemClassLoader=true достаточно
@GrabResolver(name = 'prka-snapshot', root = "http://nexus.prk-a.ax:8081/repository/maven-central/")
@Grab(group = 'org.postgresql', module = 'postgresql', version = '9.4.1209')

import static ConfigUtils.CONFIG

static void main(String[] args) {

    println "Configuration is: "
    CONFIG.each {
        k, v ->
            println "$k -> $v"
    }
    println "=" * 80
    IOUtils.askYesNo("Continue?")

    String targetDBName
    if (!DBUtils.isTargetDBExists()) {
        targetDBName = DBUtils.createTargetDB()
    }

    println "Database $CONFIG.targetDB EXISTS, check for version.."

    def sql = DBUtils.connectToTargetDB()

    def vcsDBTableExists = DBUtils.isDBVCSTableExists()
    if (!vcsDBTableExists && IOUtils.askYesNo("Database version table is missing, create it automatically?")) {
        DBUtils.createVCSDBTable()
    }


    def current = DBUtils.currentDBVersion()
    //0 это корректное значение, значит есть голая схема, а NULL значит ничего нет вообще
    if (current != null) {
        println "Database has version $current"

        //В случае, если 0 версия -- то будем обновлять все патчи (на текущий момент)
        def patchMap = DBUtils.patchFileTreeMap(0, current != 0 ? current : Integer.MAX_VALUE)
        //TODO: Можно сделать выбор до какого патча откатывать
        //TODO: хотя это смысла особо не имеет, т.к. хранимки всегда последней версии в репозитории
        def lastPatchVersion = patchMap.lastEntry().getKey()

        def patchList = DBUtils.patchFiles(current, lastPatchVersion)

        String msg
        if (patchList) {
            StringBuilder sb = new StringBuilder();
            patchList.each {
                sb.append(it).append("\n");
            }
            msg = "We can apply ${patchList.size()} patches listed below. Continue? \n${sb.toString()}"
        } else {
            msg = "No new patches found. We can just update stored procedures,views,mviews. Continue?"
        }

        boolean ok = IOUtils.askYesNo(msg)
        if (ok) {
            DBUtils.upgrade(false, current, lastPatchVersion)
        }

    } else {
        //TODO: Схемы нет, спросить пользователя, накатываем ли схему?
        boolean all = IOUtils.askYesNo("No schema are present, should we deploy schema + all patches?")

        if (all) {
            DBUtils.freshInstall()
        } else {
            println "Not implemented yet"
            System.exit(0)
        }
    }
}










