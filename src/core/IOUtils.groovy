package core

class IOUtils {
    static boolean askYesNo(String msg) {
        String input;
        do {
            println "$msg [Y/N]: "
        }
        while (!(input = System.in.newReader().readLine()).matches("[YyNn]"));

        return input.toLowerCase() == 'y'
    }
}
