package core


import static core.ConfigUtils.CONFIG

@GrabConfig(systemClassLoader = true)
@Grab(group = 'org.postgresql', module = 'postgresql', version = '9.4-1205-jdbc42')

public static void main(String[] args) {
    String targetDBName;
    if (!DBUtils.isTargetDBExists()) {
        targetDBName = DBUtils.createTargetDB()
    }

    println "Database $CONFIG.targetDB EXISTS, check for version.. Not implemented yet."

    def sql = DBUtils.connectToTargetDB();

    DBUtils.ensureVCSDBTableExists();
    def current = DBUtils.currentDBVersion();
    if (current) {
        println "Database has version $current"

        def patchMap = DBUtils.patchFileTreeMap(0, current);
        //TODO: Можно сделать выбор до какого патча откатывать
        //TODO: хотя это смысла особо не имеет, т.к. хранимки всегда последней версии в репозитории
        def lastPatchVersion = patchMap.lastEntry().getKey();

        def patchList = DBUtils.patchFiles(current, lastPatchVersion)

        String msg;
        if (patchList) {
            msg = "We can apply ${patchList},continue?";
        } else {
            msg = "No new patches found. We can just update stored procedures,views,mviews, continue?"
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
            System.exit(0);
        }
    }
}










