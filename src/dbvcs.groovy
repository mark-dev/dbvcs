@GrabConfig(systemClassLoader = true)
@Grab(group = 'org.postgresql', module = 'postgresql', version = '9.4.1209')

import static ConfigUtils.CONFIG

static void printConfigurationAndAckContinue() {
    println "Configuration is: "
    CONFIG.each {
        k, v ->
            println "$k -> $v"
    }
    println "=" * 80
    IOUtils.ensureYesOrShutdown("Continue?", 0)
}

static void main(String[] args) {

    printConfigurationAndAckContinue()

    //Создаем БД, если ее нет
    if (!DBUtils.isTargetDBExists()) {
        IOUtils.ensureYesOrShutdown("Target DB is not exists. Create it automatically?", 0)
        DBUtils.createTargetDB()
    }
    println "Database $CONFIG.targetDB EXISTS, check for version.."


    //Соеденяемся, проверяем, что есть таблица версионности, создаем если нет
    DBUtils.establishTargetDBConnection()

    def vcsDBTableExists = DBUtils.isDBVCSTableExists()
    if (!vcsDBTableExists) {
        IOUtils.ensureYesOrShutdown("Database version table is missing, create it automatically?", 0)
        DBUtils.createVCSDBTable()
        boolean hasSchema = IOUtils.askYesNo("Database version table created. You already have schema?")
        if (hasSchema) {
            int version = IOUtils.askInteger("Enter your database version, use 0 if you have just schema, no patches")
            DBUtils.setDatabaseVersion(version, "old-schema")
        }
    }


    def current = DBUtils.currentDBVersion()

    //В зависимости от версии, будем или накатывать с нуля все, или только нужные патчи.

    //0 это корректное значение, значит есть голая схема, а NULL значит ничего нет вообще
    //Т.е. просто if(current) тут нельзя написать
    if (current != null) {

        println "Database has version $current"

        //В случае, если 0 версия -- то будем обновлять все патчи (на текущий момент)
        def patchMap = DBUtils.patchFileTreeMap(0, current != 0 ? current : Integer.MAX_VALUE)
        def lastPatchVersion = 0;

        //Нужно найти последний из имеющихся в проекте патчей, но патчей может и не быть вообще(как и директории)
        def patchList = []
        if (patchMap) {
            lastPatchVersion = patchMap.lastKey();
            //TODO: Можно сделать выбор до какого патча откатывать
            //TODO: хотя это смысла особо не имеет, т.к. хранимки всегда последней версии в репозиторииlastPatchVersion = patchMap.lastEntry().getKey()
            patchList = DBUtils.patchFiles(current, lastPatchVersion);
        }

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

        IOUtils.ensureYesOrShutdown(msg, 0)
        DBUtils.upgrade(false, current, lastPatchVersion)

    } else {
        //TODO: Схемы нет, спросить пользователя, накатываем ли схему?
        IOUtils.ensureYesOrShutdown("No schema are present, should we deploy schema + all patches?", 0)
        DBUtils.freshInstall()
    }
    println "OK"
}










