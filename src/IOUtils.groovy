class IOUtils {
    static boolean askYesNo(String msg) {
        String input;
        do {
            println "$msg [Y/N]: "
        }
        while (!(input = System.in.newReader().readLine()).matches("[YyNn]"));

        return input.toLowerCase() == 'y'
    }

    static Integer askInteger(String msg) {
        String input;
        do {
            println msg
        }
        while (!(input = System.in.newReader().readLine()).matches("\\d+"));

        return Integer.parseInt(input)
    }

    static void ensureYes(String msg, int exitCodeIfNo) {
        if (!askYesNo(msg))
            System.exit(exitCodeIfNo)
    }
}
